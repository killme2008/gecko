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
/**
 * 
 */

package com.taobao.gecko.core.nio.impl;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Date;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.AbstractSession;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.util.LinkedTransferQueue;
import com.taobao.gecko.core.util.SystemUtils;


/**
 * 
 * Reactor实现
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 下午01:25:19
 */

public final class Reactor extends Thread {
    /**
     * 定时器队列访问器
     * 
     * @author boyan
     * @Date 2010-5-20
     * 
     */
    private final class TimerQueueVisitor implements TimerRefQueue.TimerQueueVisitor {
        private final long now;


        private TimerQueueVisitor(final long now) {
            this.now = now;
        }


        public boolean visit(final TimerRef timerRef) {
            if (!timerRef.isCanceled()) {
                // 已经超时，马上处理
                if (timerRef.getTimeoutTimestamp() < this.now) {
                    Reactor.this.timerQueue.remove(timerRef);
                    Reactor.this.controller.onTimeout(timerRef);
                }
                else if (this.now - timerRef.addTimestamp >= TIMEOUT_THRESOLD) {
                    // 超过阀值，搬迁到优先队列
                    Reactor.this.timerQueue.remove(timerRef);
                    Reactor.this.timerHeap.offer(timerRef);
                }

            }
            return true;
        }
    }

    public static final long TIMEOUT_THRESOLD = Long.parseLong(System.getProperty(
        "notify.remoting.timer.timeout_threshold", "500"));
    /**
     * 防止jvm bug
     */
    public static final int JVMBUG_THRESHHOLD = Integer.getInteger("com.googlecode.yanf4j.nio.JVMBUG_THRESHHOLD", 128);
    public static final int JVMBUG_THRESHHOLD2 = JVMBUG_THRESHHOLD * 2;
    public static final int JVMBUG_THRESHHOLD1 = (JVMBUG_THRESHHOLD2 + JVMBUG_THRESHHOLD) / 2;

    public static final int MAX_TIMER_COUNT = 500000;

    public static final int MAX_TIME_OUT_EVENT_PER_TIME = 2000;

    private static final Log log = LogFactory.getLog(Reactor.class);

    // bug等级
    private boolean jvmBug0;
    private boolean jvmBug1;

    private final int reactorIndex;

    private final SelectorManager selectorManager;

    // bug产生次数
    private final AtomicInteger jvmBug = new AtomicInteger(0);

    // 上一次发生bug的时间
    private long lastJVMBug;

    private volatile Selector selector;

    private final NioController controller;

    private final Configuration configuration;

    private final AtomicBoolean wakenUp = new AtomicBoolean(false);

    /**
     * 注册的事件列表
     */
    private final Queue<Object[]> register = new LinkedTransferQueue<Object[]>();

    private final TimerRefQueue timerQueue = new TimerRefQueue();

    /**
     * 记录cancel的key数目，这里本当用AtomicInteger，不过我们不追求完全精确的控制，只是一个预防手段
     */
    private volatile int cancelledKeys;

    // cancel keys的个数阀值，超过这个数值调用一次selectNow一次性清除
    static final int CLEANUP_INTERVAL = 256;

    /**
     * 超时时间的二叉堆
     */
    private final PriorityQueue<TimerRef> timerHeap = new PriorityQueue<TimerRef>();
    /**
     * 时间缓存
     */
    private volatile long timeCache;

    private final Lock gate = new ReentrantLock();

    private volatile int selectTries = 0;

    private long nextTimeout = 0;

    private long lastMoveTimestamp = 0; // 上次从timerQueue搬迁到timerHeap的时间戳
    
    private volatile long lastCheckSessionTimeoutTs;


    Reactor(final SelectorManager selectorManager, final Configuration configuration, final int index)
            throws IOException {
        super();
        this.reactorIndex = index;
        this.selectorManager = selectorManager;
        this.controller = selectorManager.getController();
        this.selector = SystemUtils.openSelector();
        this.configuration = configuration;
        this.setName("notify-remoting-reactor-" + index);
    }


    final Selector getSelector() {
        return this.selector;
    }


    public int getReactorIndex() {
        return this.reactorIndex;
    }


    /**
     * 取最近的超时时间的时间
     * 
     * @return
     */
    private long timeoutNext() {
        long selectionTimeout = TIMEOUT_THRESOLD;
        TimerRef timerRef = this.timerHeap.peek();
        while (timerRef != null && timerRef.isCanceled()) {
            this.timerHeap.poll();
            timerRef = this.timerHeap.peek();
        }
        if (timerRef != null) {
            final long now = this.getTime();
            // 已经有事件超时，返回-1，不进行select，及时处理超时
            if (timerRef.getTimeoutTimestamp() < now) {
                selectionTimeout = -1L;
            }
            else {
                selectionTimeout = timerRef.getTimeoutTimestamp() - now;
            }
        }
        return selectionTimeout;
    }


    /**
     * Select并派发事件
     */
    @Override
    public void run() {
        this.selectorManager.notifyReady();
        while (this.selectorManager.isStarted() && this.selector.isOpen()) {
            try {
                this.cancelledKeys = 0;
                this.beforeSelect();

                long before = -1;
                if (this.isNeedLookingJVMBug()) {
                    before = System.currentTimeMillis();
                }
                long wait = this.timeoutNext();
                if (this.nextTimeout > 0 && this.nextTimeout < wait) {
                    wait = this.nextTimeout;
                }
                // 清空时间缓存
                this.timeCache = 0;
                this.wakenUp.set(false);
                final int selected = this.select(wait);
                if (selected == 0) {
                    /**
                     * 查看是否发生BUG，参见http://bugs.sun.com/bugdatabase /view_bug
                     * .do?bug_id=6403933
                     */
                    if (before != -1) {
                        this.lookJVMBug(before, selected, wait);
                    }
                    this.selectTries++;
                    // 检测连接是否过期或者idle，计算下次timeout时间
                    this.nextTimeout = this.checkSessionTimeout();
                }
                else {
                    this.selectTries = 0;
                }
                // 缓存时间，那么后续的处理超时和添加timer的获取的时间都是缓存的时间，降低开销
                this.timeCache = this.getTime();
                this.processTimeout();
                this.processSelectedKeys();
            }
            catch (final ClosedSelectorException e) {
                break;
            }
            catch (final Exception e) {
                log.error("Reactor select error", e);
                if (this.selector.isOpen()) {
                    continue;
                }
                else {
                    break;
                }
            }
        }
        if (this.selector != null) {
            if (this.selector.isOpen()) {
                try {
                    this.controller.closeChannel(this.selector);
                    this.selector.selectNow();
                    this.selector.close();
                }
                catch (final IOException e) {
                    this.controller.notifyException(e);
                    log.error("stop reactor error", e);
                }
            }
        }

    }


    private void processTimeout() {
        if (!this.timerHeap.isEmpty()) {
            final long now = this.getTime();
            TimerRef timerRef = null;
            while ((timerRef = this.timerHeap.peek()) != null) {
                if (timerRef.isCanceled()) {
                    this.timerHeap.poll();
                    continue;
                }
                // 没有超时，break掉
                if (timerRef.getTimeoutTimestamp() > now) {
                    break;
                }
                // 移除并处理
                this.controller.onTimeout(this.timerHeap.poll());
            }
        }
    }


    private Set<SelectionKey> processSelectedKeys() throws IOException {
        final Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
        this.gate.lock();
        try {
            this.postSelect(selectedKeys, this.selector.keys());
            this.dispatchEvent(selectedKeys);
        }
        finally {
            this.gate.unlock();
        }
        this.clearCancelKeys();
        return selectedKeys;
    }


    private void clearCancelKeys() throws IOException {
        if (this.cancelledKeys > CLEANUP_INTERVAL) {
            final Selector selector = this.selector;
            selector.selectNow();
            this.cancelledKeys = 0;
        }
    }


    private int select(final long wait) throws IOException {
        // 这里仍然是有竞争条件的，只能尽量避免
        if (wait > 0 && !this.wakenUp.get()) {
            return this.selector.select(wait);
        }
        else {
            return this.selector.selectNow();
        }
    }


    public long getTime() {
        final long timeCache = this.timeCache;
        if (timeCache > 0) {
            return timeCache;
        }
        else {
            return System.currentTimeMillis();
        }
    }


    /**
     * 插入定时器，返回当前时间
     * 
     * @param timeout
     * @param runnable
     */
    public void insertTimer(final TimerRef timerRef) {
        if (timerRef.getTimeout() > 0 && timerRef.getRunnable() != null && !timerRef.isCanceled()) {
            final long now = this.getTime();
            final long timestamp = now + timerRef.getTimeout();
            timerRef.setTimeoutTimestamp(timestamp);
            timerRef.addTimestamp = now;
            this.timerQueue.add(timerRef);
        }
    }


    private boolean lookJVMBug(final long before, final int selected, final long wait) throws IOException {
        boolean seeing = false;
        final long now = System.currentTimeMillis();
        /**
         * Bug判断条件,(1)select为0 (2)select阻塞时间小于某个阀值 (3)非线程中断引起 (4)非wakenup引起
         */
        if (JVMBUG_THRESHHOLD > 0 && selected == 0 && wait > JVMBUG_THRESHHOLD && now - before < wait / 4
                && !this.wakenUp.get() /* waken up */
                && !Thread.currentThread().isInterrupted()/* Interrupted */) {
            this.jvmBug.incrementAndGet();
            // 严重等级1，重新创建selector
            if (this.jvmBug.get() >= JVMBUG_THRESHHOLD2) {
                this.gate.lock();
                try {
                    this.lastJVMBug = now;
                    log.warn("JVM bug occured at " + new Date(this.lastJVMBug)
                            + ",http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933,reactIndex="
                            + this.reactorIndex);
                    if (this.jvmBug1) {
                        log.debug("seeing JVM BUG(s) - recreating selector,reactIndex=" + this.reactorIndex);
                    }
                    else {
                        this.jvmBug1 = true;
                        log.info("seeing JVM BUG(s) - recreating selector,reactIndex=" + this.reactorIndex);
                    }
                    seeing = true;
                    // 创建新的selector
                    final Selector new_selector = SystemUtils.openSelector();

                    for (final SelectionKey k : this.selector.keys()) {
                        if (!k.isValid() || k.interestOps() == 0) {
                            continue;
                        }

                        final SelectableChannel channel = k.channel();
                        final Object attachment = k.attachment();
                        // 将不是无效，并且interestOps>0的channel继续注册
                        channel.register(new_selector, k.interestOps(), attachment);
                    }

                    this.selector.close();
                    this.selector = new_selector;

                }
                finally {
                    this.gate.unlock();
                }
                this.jvmBug.set(0);

            }
            else if (this.jvmBug.get() == JVMBUG_THRESHHOLD || this.jvmBug.get() == JVMBUG_THRESHHOLD1) {
                // BUG严重等级0，取消所有interestedOps==0的key
                if (this.jvmBug0) {
                    log.debug("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex=" + this.reactorIndex);
                }
                else {
                    this.jvmBug0 = true;
                    log.info("seeing JVM BUG(s) - cancelling interestOps==0,reactIndex=" + this.reactorIndex);
                }
                this.gate.lock();
                seeing = true;
                try {
                    for (final SelectionKey k : this.selector.keys()) {
                        if (k.isValid() && k.interestOps() == 0) {
                            k.cancel();
                        }
                    }
                }
                finally {
                    this.gate.unlock();
                }
            }
        }
        else {
            this.jvmBug.set(0);
        }
        return seeing;
    }


    private boolean isNeedLookingJVMBug() {
        return SystemUtils.isLinuxPlatform() && !SystemUtils.isAfterJava6u4Version();
    }


    final void dispatchEvent(final Set<SelectionKey> selectedKeySet) {
        final Iterator<SelectionKey> it = selectedKeySet.iterator();
        boolean skipOpRead = false; // 是否跳过读
        while (it.hasNext()) {
            final SelectionKey key = it.next();
            it.remove();
            if (!key.isValid()) {
                if (key.attachment() != null) {
                    this.controller.closeSelectionKey(key);
                }
                else {
                    key.cancel();
                }
                continue;
            }
            try {
                if (key.isAcceptable()) {
                    this.controller.onAccept(key);
                    continue;
                }
                if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                    // Remove write interest
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    this.controller.onWrite(key);
                    if (!this.controller.isHandleReadWriteConcurrently()) {
                        skipOpRead = true;
                    }
                }
                if (!skipOpRead && (key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    // 移除对read的兴趣
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                    // 是否超过流量控制
                    if (!this.controller.getStatistics().isReceiveOverFlow()) {
                        // Remove read interest

                        this.controller.onRead(key);// 派发读
                        continue;
                    }
                    else {
                        key.interestOps(key.interestOps() // 继续注册读
                                | SelectionKey.OP_READ);
                    }

                }
                if ((key.readyOps() & SelectionKey.OP_CONNECT) == SelectionKey.OP_CONNECT) {
                    this.controller.onConnect(key);
                    continue;
                }

            }
            catch (final RejectedExecutionException e) {
                // 捕获线程池繁忙的异常，不关闭连接
                if (key.attachment() instanceof AbstractNioSession) {
                    ((AbstractSession) key.attachment()).onException(e);
                }
                this.controller.notifyException(e);
                if (this.selector.isOpen()) {
                    continue;
                }
                else {
                    break;
                }
            }
            catch (final CancelledKeyException e) {
                // ignore
            }
            catch (final Exception e) {
                if (key.attachment() instanceof AbstractNioSession) {
                    ((AbstractSession) key.attachment()).onException(e);
                }
                this.controller.closeSelectionKey(key);
                this.controller.notifyException(e);
                log.error("Reactor dispatch events error", e);
                if (this.selector.isOpen()) {
                    continue;
                }
                else {
                    break;
                }
            }
        }
    }


    final void unregisterChannel(final SelectableChannel channel) {
        try {
            final Selector selector = this.selector;
            if (selector != null) {
                if (channel != null) {
                    final SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                        this.cancelledKeys++;
                    }
                }
            }
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        }
        catch (final Throwable t) {
            // ignore
        }
        this.wakeup();
    }


    private final long checkSessionTimeout() {
        long nextTimeout = 0;
        if (this.configuration.getCheckSessionTimeoutInterval() > 0) {
            this.gate.lock();
            try {
                if (isNeedCheckSessionTimeout()) {
                    nextTimeout = this.configuration.getCheckSessionTimeoutInterval();
                    for (final SelectionKey key : this.selector.keys()) {
                        // 检测是否expired或者idle
                        if (key.attachment() != null) {
                            final long n = this.checkExpiredIdle(key, this.getSessionFromAttchment(key));
                            nextTimeout = n < nextTimeout ? n : nextTimeout;
                        }
                    }
                    this.selectTries = 0;
                    this.lastCheckSessionTimeoutTs = this.getTime();
                }
            }
            finally {
                this.gate.unlock();
            }
        }
        return nextTimeout;
    }


	private boolean isNeedCheckSessionTimeout() {
		return this.selectTries * 1000 >= this.configuration.getCheckSessionTimeoutInterval()
				|| this.getTime() - lastCheckSessionTimeoutTs >= this.configuration.getCheckSessionTimeoutInterval();
	}


    private final Session getSessionFromAttchment(final SelectionKey key) {
        if (key.attachment() instanceof Session) {
            return (Session) key.attachment();
        }
        return null;
    }


    final void registerSession(final Session session, final EventType event) {
        final Selector selector = this.selector;
        if (this.isReactorThread() && selector != null) {
            this.dispatchSessionEvent(session, event, selector);
        }
        else {
            this.register.offer(new Object[] { session, event });
            this.wakeup();
        }
    }


    private final boolean isReactorThread() {
        return Thread.currentThread() == this;
    }


    final void beforeSelect() throws IOException {
        this.controller.checkStatisticsForRestart();
        this.processRegister();
        this.processMoveTimer();
        this.clearCancelKeys();
    }


    private void processMoveTimer() {
        final long now = this.getTime();
        // 距离上一次检测时间超过1秒
        if (now - this.lastMoveTimestamp >= TIMEOUT_THRESOLD && !this.timerQueue.isEmpty()) {
            this.lastMoveTimestamp = now;
            this.timerQueue.iterateQueue(new TimerQueueVisitor(now));
        }
    }


    private final void processRegister() {
        Object[] object = null;
        while ((object = this.register.poll()) != null) {
            switch (object.length) {
            case 2:
                this.dispatchSessionEvent((Session) object[0], (EventType) object[1], this.selector);
                break;
            case 3:
                this.registerChannelNow((SelectableChannel) object[0], (Integer) object[1], object[2], this.selector);
                break;
            }
        }
    }


    Configuration getConfiguration() {
        return this.configuration;
    }


    private final void dispatchSessionEvent(final Session session, final EventType event, final Selector selector) {
        if (EventType.REGISTER.equals(event)) {
            this.controller.registerSession(session);
        }
        else if (EventType.UNREGISTER.equals(event)) {
            this.controller.unregisterSession(session);
            this.unregisterChannel(((NioSession) session).channel());
        }
        else {
            ((NioSession) session).onEvent(event, selector);
        }
    }


	final void postSelect(final Set<SelectionKey> selectedKeys, final Set<SelectionKey> allKeys) {
		if (this.controller.getSessionTimeout() > 0 || this.controller.getSessionIdleTimeout() > 0) {
			if (isNeedCheckSessionTimeout()) {
				for (final SelectionKey key : allKeys) {
					// 没有触发的key检测是否超时或者idle
					if (!selectedKeys.contains(key)) {
						if (key.attachment() != null) {
							this.checkExpiredIdle(key, this.getSessionFromAttchment(key));
						}
					}
				}
				this.lastCheckSessionTimeoutTs = this.getTime();
			}
		}
	}


    private long checkExpiredIdle(final SelectionKey key, final Session session) {
        if (session == null) {
            return 0;
        }
        long nextTimeout = 0;
        boolean expired = false;
        if (this.controller.getSessionTimeout() > 0) {
            expired = this.checkExpired(key, session);
            nextTimeout = this.controller.getSessionTimeout();
        }
        if (this.controller.getSessionIdleTimeout() > 0 && !expired) {
            this.checkIdle(session);
            nextTimeout = this.controller.getSessionIdleTimeout();
        }
        return nextTimeout;
    }


    private final void checkIdle(final Session session) {
        if (this.controller.getSessionIdleTimeout() > 0) {
            if (session.isIdle()) {
                ((NioSession) session).onEvent(EventType.IDLE, this.selector);
            }
        }
    }


    private final boolean checkExpired(final SelectionKey key, final Session session) {
        if (session.isExpired()) {
            ((NioSession) session).onEvent(EventType.EXPIRED, this.selector);
            return true;
        }
        return false;
    }


    final void registerChannel(final SelectableChannel channel, final int ops, final Object attachment) {
        final Selector selector = this.selector;
        if (this.isReactorThread() && selector != null) {
            this.registerChannelNow(channel, ops, attachment, selector);
        }
        else {
            this.register.offer(new Object[] { channel, ops, attachment });
            this.wakeup();
        }

    }


    private void registerChannelNow(final SelectableChannel channel, final int ops, final Object attachment,
            final Selector selector) {
        this.gate.lock();
        try {
            if (channel.isOpen()) {
                channel.register(selector, ops, attachment);
            }
        }
        catch (final ClosedChannelException e) {
            log.error("Register channel error", e);
            this.controller.notifyException(e);
        }
        finally {
            this.gate.unlock();
        }
    }


    final void wakeup() {
        if (this.wakenUp.compareAndSet(false, true)) {
            final Selector selector = this.selector;
            if (selector != null) {
                selector.wakeup();
            }
        }
    }


    final void selectNow() throws IOException {
        final Selector selector = this.selector;
        if (selector != null) {
            selector.selectNow();
        }
    }
}