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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.core.util.ExceptionMonitor;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingController;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.exception.IllegalMessageException;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 网络层的业务处理器
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 上午11:14:51
 */

public class GeckoHandler implements Handler {

    /**
     * 请求处理器的任务包装
     * 
     * @author boyan
     * 
     */
    private static final class ProcessorRunner<T extends RequestCommand> implements Runnable {
        private final DefaultConnection defaultConnection;
        private final RequestProcessor<T> processor;
        private final T message;


        private ProcessorRunner(final DefaultConnection defaultConnection, final RequestProcessor<T> processor,
                final T message) {
            this.defaultConnection = defaultConnection;
            this.processor = processor;
            this.message = message;
        }


        public void run() {
            this.processor.handleRequest(this.message, this.defaultConnection);
        }
    }

    /**
     * 心跳命令的异步监听器
     * 
     * @author boyan
     * 
     */
    private final static class HeartBeatListener implements SingleRequestCallBackListener {
        private final Connection conn;
        static final String HEARBEAT_FAIL_COUNT = "connection_heartbeat_fail_count";


        public ThreadPoolExecutor getExecutor() {
            return null;
        }


        private HeartBeatListener(final Connection conn) {
            this.conn = conn;
        }


        public void onException(final Exception e) {
            this.innerCloseConnection(this.conn);
        }


        public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
            if (responseCommand == null || responseCommand.getResponseStatus() != ResponseStatus.NO_ERROR) {
                Integer count = (Integer) this.conn.setAttributeIfAbsent(HEARBEAT_FAIL_COUNT, 1);
                if (count != null) {
                    count++;
                    if (count < 3) {
                        conn.setAttribute(HEARBEAT_FAIL_COUNT, count);
                    }
                    else {
                        this.innerCloseConnection(conn);
                    }
                }
            }
            else {
                this.conn.removeAttribute(HEARBEAT_FAIL_COUNT);
            }
        }


        private void innerCloseConnection(final Connection conn) {
            log.info("心跳检测失败，关闭连接" + conn.getRemoteSocketAddress() + ",分组信息" + conn.getGroupSet());
            try {
                conn.close(true);
            }
            catch (final NotifyRemotingException e) {
                log.error("关闭连接失败", e);
            }
        }
    }

    private final DefaultRemotingContext remotingContext;
    private final RemotingController remotingController;
    private ReconnectManager reconnectManager;
    private static final Log log = LogFactory.getLog(GeckoHandler.class);


    public void setReconnectManager(final ReconnectManager reconnectManager) {
        this.reconnectManager = reconnectManager;
    }


    private void responseThreadPoolBusy(final Session session, final Object msg,
            final DefaultConnection defaultConnection) {
        if (defaultConnection != null && msg instanceof RequestCommand) {
            try {
                defaultConnection.response(defaultConnection
                    .getRemotingContext()
                    .getCommandFactory()
                    .createBooleanAckCommand(((RequestCommand) msg).getRequestHeader(), ResponseStatus.THREADPOOL_BUSY,
                        "线程池繁忙"));
            }
            catch (final NotifyRemotingException e) {
                this.onExceptionCaught(session, e);
            }
        }
    }


    public GeckoHandler(final RemotingController remotingController) {
        this.remotingContext = (DefaultRemotingContext) remotingController.getRemotingContext();
        this.remotingController = remotingController;
    }


    public void onExceptionCaught(final Session session, final Throwable throwable) {
        if (throwable.getCause() != null) {
            ExceptionMonitor.getInstance().exceptionCaught(throwable.getCause());
        }
        else {
            ExceptionMonitor.getInstance().exceptionCaught(throwable);
        }
    }


    public void onMessageReceived(final Session session, final Object message) {
        final DefaultConnection defaultConnection = this.remotingContext.getConnectionBySession((NioSession) session);
        if (defaultConnection == null) {
            log.error("Connection[" + RemotingUtils.getAddrString(session.getRemoteSocketAddress()) + "]已经被关闭，无法处理消息");
            session.close();
            return;
        }
        if (message instanceof RequestCommand) {
            this.processRequest(session, message, defaultConnection);
        }
        else if (message instanceof ResponseCommand) {
            this.processResponse(message, defaultConnection);
        }
        else {
            throw new IllegalMessageException("未知的消息类型" + message);
        }

    }


    private void processResponse(final Object message, final DefaultConnection defaultConnection) {
        final ResponseCommand responseCommand = (ResponseCommand) message;
        responseCommand.setResponseHost(defaultConnection.getRemoteSocketAddress());
        responseCommand.setResponseTime(System.currentTimeMillis());
        final RequestCallBack requestCallBack = defaultConnection.getRequestCallBack(responseCommand.getOpaque());
        if (requestCallBack != null) {
            requestCallBack.onResponse(null, responseCommand, defaultConnection);
        }
    }


    @SuppressWarnings("unchecked")
    private <T extends RequestCommand> void processRequest(final Session session, final Object message,
            final DefaultConnection defaultConnection) {
        final RequestProcessor<T> processor = this.getProcessorByMessage(message);
        if (processor == null) {
            log.error("未找到" + message.getClass().getCanonicalName() + "对应的处理器");
            this.responseNoProcessor(session, message, defaultConnection);
            return;
        }
        else {
            this.executeProcessor(session, (T) message, defaultConnection, processor);
        }
    }


    @SuppressWarnings("unchecked")
    private <T extends RequestCommand> RequestProcessor<T> getProcessorByMessage(final Object message) {
        final RequestProcessor<T> processor;
        if (message instanceof HeartBeatRequestCommand) {
            processor = (RequestProcessor<T>) this.remotingContext.processorMap.get(HeartBeatRequestCommand.class);
        }
        else {
            processor = (RequestProcessor<T>) this.remotingContext.processorMap.get(message.getClass());
        }
        return processor;
    }


    /**
     * 执行实际的Processor
     * 
     * @param session
     * @param message
     * @param defaultConnection
     * @param processor
     */
    private <T extends RequestCommand> void executeProcessor(final Session session, final T message,
            final DefaultConnection defaultConnection, final RequestProcessor<T> processor) {
        if (processor.getExecutor() == null) {
            processor.handleRequest(message, defaultConnection);
        }
        else {
            try {
                processor.getExecutor().execute(new ProcessorRunner<T>(defaultConnection, processor, message));
            }
            catch (final RejectedExecutionException e) {
                this.responseThreadPoolBusy(session, message, defaultConnection);
            }
        }
    }


    private void responseNoProcessor(final Session session, final Object message,
            final DefaultConnection defaultConnection) {
        if (defaultConnection != null && message instanceof RequestCommand) {
            try {
                defaultConnection.response(defaultConnection
                    .getRemotingContext()
                    .getCommandFactory()
                    .createBooleanAckCommand(((RequestCommand) message).getRequestHeader(),
                        ResponseStatus.NO_PROCESSOR, "未注册请求处理器，请求处理器类为" + message.getClass().getCanonicalName()));
            }
            catch (final NotifyRemotingException e) {
                this.onExceptionCaught(session, e);
            }
        }
    }


    public void onMessageSent(final Session session, final Object msg) {

    }


    public void onSessionClosed(final Session session) {
        final InetSocketAddress remoteSocketAddress = session.getRemoteSocketAddress();
        final DefaultConnection conn = this.remotingContext.getConnectionBySession((NioSession) session);

        if (conn == null) {
            session.close();
            return;
        }

        log.debug("远端连接" + RemotingUtils.getAddrString(remoteSocketAddress) + "断开,分组信息" + conn.getGroupSet());

        // 允许重连，并且是客户端，加入重连任务
        if (conn.isAllowReconnect() && this.reconnectManager != null) {
            this.waitForReady(conn);
            this.addReconnectTask(remoteSocketAddress, conn);
        }
        // 从分组中移除
        this.removeFromGroups(conn);
        // 处理剩余的callBack
        conn.dispose();
        // 移除session到connection映射
        this.remotingContext.removeSession2ConnectionMapping((NioSession) session);
        this.adjustMaxScheduleWrittenBytes();
        this.remotingContext.notifyConnectionClosed(conn);
    }


    private void removeFromGroups(final DefaultConnection conn) {
        // 从所有分组中移除
        for (final String group : conn.getGroupSet()) {
            this.remotingContext.removeConnectionFromGroup(group, conn);

        }
    }


    private void addReconnectTask(final InetSocketAddress remoteSocketAddress, final DefaultConnection conn) {
        // make a copy
        final Set<String> groupSet = conn.getGroupSet();
        log.info("远端连接" + RemotingUtils.getAddrString(remoteSocketAddress) + "关闭，启动重连任务");
        // 重新检查
        synchronized (conn) {
            if (!groupSet.isEmpty() && !this.hasOnlyDefaultGroup(groupSet) && conn.isAllowReconnect()) {
                this.reconnectManager.addReconnectTask(new ReconnectTask(groupSet, remoteSocketAddress));
                // 不允许发起重连任务，防止重复
                conn.setAllowReconnect(false);
            }
        }
    }


    private boolean hasOnlyDefaultGroup(final Set<String> groupSet) {
        return groupSet.size() == 1 && groupSet.contains(Constants.DEFAULT_GROUP);
    }


    private void waitForReady(final DefaultConnection conn) {
        /**
         * 此处做同步保护，等待连接就绪，防止重连的时候遗漏分组信息
         */
        synchronized (conn) {
            int count = 0;
            while (!conn.isReady() && conn.isAllowReconnect() && count++ < 3) {
                try {
                    conn.wait(5000);
                }
                catch (final InterruptedException e) {
                    // 重设中断状态给上层处理
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    @SuppressWarnings("unchecked")
    public void onSessionConnected(final Session session, final Object... args) {
        final Set<String> groupSet = (Set<String>) args[0];
        if (args.length >= 3) {
            final TimerRef timerRef = (TimerRef) args[2];
            if (timerRef != null) {
                timerRef.cancel();
            }
        }
        final DefaultConnection conn = this.remotingContext.getConnectionBySession((NioSession) session);
        try {
            // 连接已经被关闭，或者groupSet为空，则关闭session，无需重连
            if (conn == null || groupSet.isEmpty()) {
                // 可能关闭了
                session.close();
                log.error("建立的连接没有对应的connection");
            }
            else {
                this.addConnection2Group(conn, groupSet);
            }
        }
        finally {
            // 一定要通知就绪
            if (conn != null && conn.isConnected()) {
                this.notifyConnectionReady(conn);
            }
        }
    }


    private void addConnection2Group(final DefaultConnection conn, final Set<String> groupSet) {
        if (groupSet.isEmpty() || this.hasOnlyDefaultGroup(groupSet)) {
            this.closeConnectionWithoutReconnect(conn);
            return;
        }
        // 将建立的连接加入分组
        for (final String group : groupSet) {
            final Object attribute = this.remotingController.getAttribute(group, Constants.CONNECTION_COUNT_ATTR);
            if (attribute == null) {
                // 没有发起连接请求并且不是默认分组，强制关闭
                log.info("连接被强制断开，由于分组" + group + "没有发起过连接请求");
                this.closeConnectionWithoutReconnect(conn);
                return;
            }
            else {
                final int maxConnCount = (Integer) attribute;
                /**
                 * 判断分组连接数和加入分组放入同一个同步块，防止竞争条件
                 */
                synchronized (this) {
                    // 加入分组
                    if (this.remotingController.getConnectionCount(group) < maxConnCount) {
                        this.addConnectionToGroup(conn, group, maxConnCount);
                    }
                    else {
                        // 尝试移除断开的连接，再次加入
                        if (this.removeDisconnectedConnection(group)) {
                            this.addConnectionToGroup(conn, group, maxConnCount);
                        }
                        else {
                            // 确认是多余的，关闭
                            log.warn("连接数(" + conn.getRemoteSocketAddress() + ")超过设定值" + maxConnCount + "，连接将被关闭");
                            this.closeConnectionWithoutReconnect(conn);
                        }
                    }
                }
            }
        }
    }


    private void closeConnectionWithoutReconnect(final DefaultConnection conn) {
        try {
            conn.close(false);
        }
        catch (final NotifyRemotingException e) {
            log.error("关闭连接失败", e);
        }
    }


    private void notifyConnectionReady(final DefaultConnection conn) {
        // 通知连接已经就绪，断开连接的时候将自动将此连接加入该分组
        if (conn != null) {
            synchronized (conn) {
                conn.setReady(true);
                conn.notifyAll();
            }
            // 通知监听器连接就绪
            for (final ConnectionLifeCycleListener listener : this.remotingContext.connectionLifeCycleListenerList) {
                try {
                    listener.onConnectionReady(conn);
                }
                catch (final Throwable t) {
                    log.error("调用ConnectionLifeCycleListener.onConnectionReady异常", t);
                }
            }
        }
    }


    private boolean removeDisconnectedConnection(final String group) {
        // 超过最大数目限制，遍历所有连接，移除断开的connection（可能没有被及时移除)
        final List<Connection> currentConnList =
                this.remotingController.getRemotingContext().getConnectionsByGroup(group);
        Connection disconnectedConn = null;
        if (currentConnList != null) {

            synchronized (currentConnList) {
                final ListIterator<Connection> it = currentConnList.listIterator();
                while (it.hasNext()) {
                    final Connection currentConn = it.next();
                    if (!currentConn.isConnected()) {
                        disconnectedConn = currentConn;
                        break;
                    }
                    else {
                        // 当前可用连接，确保已经是就绪状态，这是为了防止下列场景：
                        // 连接建立成功，但是超过了规定的超时时间，却仍然被加入了分组，没有通知就绪
                        if (!((DefaultConnection) currentConn).isReady() && !currentConn.getGroupSet().isEmpty()) {
                            this.notifyConnectionReady((DefaultConnection) currentConn);
                        }
                    }
                }
            }
        }
        if (disconnectedConn != null) {
            return currentConnList.remove(disconnectedConn);
        }
        else {
            return false;
        }
    }


    private void addConnectionToGroup(final DefaultConnection conn, final String group, final int maxConnCount) {
        conn.getRemotingContext().addConnectionToGroup(group, conn);
        // 获取分组连接就绪锁
        final Object readyLock = this.remotingController.getAttribute(group, Constants.GROUP_CONNECTION_READY_LOCK);
        if (readyLock != null) {
            // 通知分组所有连接就绪
            synchronized (readyLock) {
                if (this.remotingController.getConnectionCount(group) >= maxConnCount) {
                    readyLock.notifyAll();
                }
            }
        }
    }


    public void onSessionCreated(final Session session) {
        log.debug("连接建立，远端信息:" + RemotingUtils.getAddrString(session.getRemoteSocketAddress()));
        final DefaultConnection connection = new DefaultConnection((NioSession) session, this.remotingContext);
        // 加入默认分组
        this.remotingContext.addConnection(connection);
        // 加入session到connection的映射
        this.remotingContext.addSession2ConnectionMapping((NioSession) session, connection);
        this.remotingContext.notifyConnectionCreated(connection);
        this.adjustMaxScheduleWrittenBytes();
    }


    private void adjustMaxScheduleWrittenBytes() {
        // Server根据连接数自动调整最大发送流量参数
        if (this.remotingController instanceof RemotingServer) {
            final List<Connection> connections = this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP);
            final int connectionCount = connections != null ? connections.size() : 0;
            if (connectionCount > 0) {
                this.remotingContext.getConfig().setMaxScheduleWrittenBytes(
                    Runtime.getRuntime().maxMemory() / 3 / connectionCount);
            }
        }
    }


    public void onSessionExpired(final Session session) {

    }


    public void onSessionIdle(final Session session) {
        final Connection conn = this.remotingContext.getConnectionBySession((NioSession) session);
        try {
            conn.send(conn.getRemotingContext().getCommandFactory().createHeartBeatCommand(), new HeartBeatListener(
                conn), 5000, TimeUnit.MILLISECONDS);
        }
        catch (final NotifyRemotingException e) {
            log.error("发送心跳命令失败", e);
        }

    }


    public void onSessionStarted(final Session session) {

    }

}