/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.gecko.service.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.extension.GeckoTCPConnectorController;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.core.util.ConcurrentHashSet;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 重连管理器
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午03:01:38
 */

public class ReconnectManager {
    /**
     * 重连任务队列
     */
    private final LinkedBlockingQueue<ReconnectTask> tasks = new LinkedBlockingQueue<ReconnectTask>();
    /**
     * 取消重连任务的分组
     */
    private final ConcurrentHashSet<String/* group */> canceledGroupSet = new ConcurrentHashSet<String>();
    private volatile boolean started = false;
    private final GeckoTCPConnectorController connector;
    private static final Log log = LogFactory.getLog(ReconnectManager.class);
    private final ClientConfig clientConfig;
    private final DefaultRemotingClient remotingClient;
    private int maxRetryTimes = -1;
    /**
     * 重连任务的执行线程
     */
    private final Thread[] healConnectionThreads;

    private final class HealConnectionRunner implements Runnable {
        private long lastConnectTime = -1; // 上次连接所花费的时间


        @Override
        public void run() {
            while (ReconnectManager.this.started) {
                long start = -1;
                ReconnectTask task = null;
                try {
                    // 只有当重连所花费的时间小于重连任务间隔的时候才sleep以下，减少日志打印
                    if (this.lastConnectTime > 0
                            && this.lastConnectTime < ReconnectManager.this.clientConfig.getHealConnectionInterval()
                            || this.lastConnectTime < 0) {
                        Thread.sleep(ReconnectManager.this.clientConfig.getHealConnectionInterval());
                    }
                    task = ReconnectManager.this.tasks.take();
                    // 拷贝保护，做日志记录
                    final Set<String> copySet = new HashSet<String>(task.getGroupSet());
                    // 移除默认分组
                    copySet.remove(Constants.DEFAULT_GROUP);
                    start = System.currentTimeMillis();
                    if (ReconnectManager.this.isValidTask(task)) {
                        this.doReconnectTask(task);
                    }
                    else {
                        log.warn("Invalid reconnect request,the group set is:" + copySet);
                    }
                    this.lastConnectTime = System.currentTimeMillis() - start;
                }
                catch (final InterruptedException e) {
                    // ignore，重新检测started状态
                }
                catch (final Exception e) {
                    if (start != -1) {
                        this.lastConnectTime = System.currentTimeMillis() - start;
                    }
                    if (task != null) {
                        log.error("Reconnect to " + RemotingUtils.getAddrString(task.getRemoteAddress()) + "失败",
                            e.getCause());
                        this.readdTask(task);
                    }
                }

            }
        }


        private void readdTask(ReconnectTask task) {
            if (ReconnectManager.this.maxRetryTimes <= 0
                    || task.increaseRetryCounterAndGet() < ReconnectManager.this.maxRetryTimes) {
                ReconnectManager.this.addReconnectTask(task);
            }
            else {
                log.warn("Retry too many times to reconnect to "
                        + RemotingUtils.getAddrString(task.getRemoteAddress())
                        + ",we will remove the task.");
            }
        }


        private void doReconnectTask(final ReconnectTask task) throws IOException, NotifyRemotingException {
            log.info("Try to reconnect to " + RemotingUtils.getAddrString(task.getRemoteAddress()));
            final TimerRef timerRef = new TimerRef(ReconnectManager.this.clientConfig.getConnectTimeout(), null);
            try {
                final Future<NioSession> future =
                        ReconnectManager.this.connector.connect(task.getRemoteAddress(), task.getGroupSet(),
                            task.getRemoteAddress(), timerRef);
                final DefaultRemotingClient.CheckConnectFutureRunner runnable =
                        new DefaultRemotingClient.CheckConnectFutureRunner(future, task.getRemoteAddress(),
                            task.getGroupSet(), ReconnectManager.this.remotingClient);
                timerRef.setRunnable(runnable);
                ReconnectManager.this.remotingClient.insertTimer(timerRef);
                // 标记这个任务完成
                task.setDone(true);
            }
            catch (final Exception e) {
                this.readdTask(task);
            }
        }
    }


    public ReconnectManager(final GeckoTCPConnectorController connector, final ClientConfig clientConfig,
            final DefaultRemotingClient remotingClient) {
        super();
        this.connector = connector;
        this.clientConfig = clientConfig;
        this.remotingClient = remotingClient;
        this.started = true;
        this.maxRetryTimes = clientConfig.getMaxReconnectTimes();
        this.healConnectionThreads = new Thread[this.clientConfig.getHealConnectionExecutorPoolSize()];

    }


    public synchronized void start() {
        for (int i = 0; i < this.clientConfig.getHealConnectionExecutorPoolSize(); i++) {
            this.healConnectionThreads[i] = new Thread(new HealConnectionRunner());
            this.healConnectionThreads[i].start();
        }
    }


    public int getReconnectTaskCount() {
        return this.tasks.size();
    }


    public void addReconnectTask(final ReconnectTask task) {
        if (!this.isValidTask(task)) {
            log.warn("Invalid reconnect request,it is removed,the group set is:" + task.getGroupSet());
            return;
        }
        this.tasks.offer(task);
    }


    boolean isValidTask(final ReconnectTask task) {
        task.getGroupSet().removeAll(this.canceledGroupSet);
        return this.isValidGroup(task) && !task.isDone();
    }


    /**
     * 判断是否有效分组
     * 
     * @param task
     * @return
     */
    boolean isValidGroup(final ReconnectTask task) {
        return !this.hasOnlyDefaultGroup(task) && !this.isEmptyGroupSet(task);
    }


    /**
     * 分组为空
     * 
     * @param task
     * @return
     */
    private boolean isEmptyGroupSet(final ReconnectTask task) {
        return task.getGroupSet().size() == 0;
    }


    /**
     * 仅有默认分组
     * 
     * @param task
     * @return
     */
    private boolean hasOnlyDefaultGroup(final ReconnectTask task) {
        return task.getGroupSet().size() == 1 && task.getGroupSet().contains(Constants.DEFAULT_GROUP);
    }


    public void removeCanceledGroup(final String group) {
        this.canceledGroupSet.remove(group);
    }


    public void cancelReconnectGroup(final String group) {
        this.canceledGroupSet.add(group);
        final Iterator<ReconnectTask> it = this.tasks.iterator();
        while (it.hasNext()) {
            final ReconnectTask task = it.next();
            if (task.getGroupSet().contains(group)) {
                log.warn("Invalid reconnect request,it is removed,the group set is:" + task.getGroupSet());
                it.remove();
            }
        }
    }


    public synchronized void stop() {
        if (!this.started) {
            return;
        }
        this.started = false;
        for (final Thread thread : this.healConnectionThreads) {
            thread.interrupt();
        }
        this.tasks.clear();
        this.canceledGroupSet.clear();
    }
}