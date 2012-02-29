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
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.SocketOption;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.impl.SocketChannelController;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.core.util.WorkerThreadFactory;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.ConnectionSelector;
import com.taobao.gecko.service.GroupAllConnectionCallBackListener;
import com.taobao.gecko.service.MultiGroupCallBackListener;
import com.taobao.gecko.service.RemotingController;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.callback.GroupAllConnectionRequestCallBack;
import com.taobao.gecko.service.callback.MultiGroupRequestCallBack;
import com.taobao.gecko.service.config.BaseConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * Remoting Controller基础实现类，实现了一些server、client共有的功能，如发送请求等
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午02:16:29
 */

public abstract class BaseRemotingController implements RemotingController {
    protected ConcurrentHashMap<String/* group */, ConcurrentHashMap<String/* key */, Object>> attributes =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();
    protected DefaultRemotingContext remotingContext;
    protected ConnectionSelector connectionSelector = new RoundRobinConnectionSelector();
    /**
     * 用于扫描所有连接的定时线程池，可以设定一些全局性的任务，如扫描无效的连接，或者连接上的无效的callBack
     */
    protected ScheduledExecutorService scanAllConnectionExecutor;
    protected SocketChannelController controller;
    protected BaseConfig config;
    protected volatile boolean started;
    private Thread shutdownHook;
    private volatile boolean isHutdownHookCalled;
    /**
     * 默认调用超时时间，1秒
     */
    protected long opTimeout = 1000L;

    private static final Log log = LogFactory.getLog(BaseRemotingController.class);


    /**
     * 本方法暴露内部实现，用户切勿使用此方法
     * 
     * @return
     */
    public SocketChannelController getController() {
        return this.controller;
    }


    public final synchronized void start() throws NotifyRemotingException {
        if (this.started) {
            return;
        }
        this.started = true;
        final StringBuffer info = new StringBuffer("即将启动RemotingController...\n");
        info.append("配置为：\n").append(this.config.toString());
        log.info(info.toString());
        if (this.remotingContext == null) {
            this.remotingContext =
                    new DefaultRemotingContext(this.config, this.config.getWireFormatType().newCommandFactory());
        }
        else {
            // 复用processor和listener
            this.remotingContext.dispose();
            final ConcurrentHashMap<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>> processorMap =
                    this.remotingContext.processorMap;
            final CopyOnWriteArrayList<ConnectionLifeCycleListener> connectionLifeCycleListenerList =
                    this.remotingContext.connectionLifeCycleListenerList;
            this.remotingContext =
                    new DefaultRemotingContext(this.remotingContext.getConfig(), this.config.getWireFormatType()
                        .newCommandFactory());
            this.remotingContext.processorMap.putAll(processorMap);
            this.remotingContext.connectionLifeCycleListenerList.addAll(connectionLifeCycleListenerList);
        }
        final Configuration conf = this.getConfigurationFromConfig(this.config);
        this.controller = this.initController(conf);
        this.controller.setCodecFactory(this.config.getWireFormatType().newCodecFactory());
        this.controller.setHandler(new GeckoHandler(this));
        this.controller.setSoLinger(this.config.isSoLinger(), this.config.getLinger());
        this.controller.setSocketOptions(this.getSocketOptionsFromConfig(this.config));
        this.controller.setSelectorPoolSize(this.config.getSelectorPoolSize());
        this.scanAllConnectionExecutor =
                Executors
                    .newSingleThreadScheduledExecutor(new WorkerThreadFactory("notify-remoting-ScanAllConnection"));
        this.scanAllConnectionExecutor.scheduleAtFixedRate(new ScanAllConnectionRunner(this, this.getScanTasks()), 1,
            this.config.getScanAllConnectionInterval(), TimeUnit.SECONDS);
        this.doStart();
        this.addShutdownHook();
    }


    private void addShutdownHook() {
        this.shutdownHook = new Thread() {
            @Override
            public void run() {
                try {
                    BaseRemotingController.this.isHutdownHookCalled = true;
                    BaseRemotingController.this.stop();
                }
                catch (final NotifyRemotingException e) {
                    log.error("Shutdown remoting failed", e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }


    /**
     * 本控制器所需要的全局扫描任务，默认只有扫描无效callback的任务，子类可覆盖此方法
     * 
     * @return
     */
    protected ScanTask[] getScanTasks() {
        return new ScanTask[] { new InvalidCallBackScanTask() };
    }


    public DefaultRemotingContext getRemotingContext() {
        return this.remotingContext;
    }


    protected abstract SocketChannelController initController(Configuration conf);


    protected abstract void doStart() throws NotifyRemotingException;


    protected abstract void doStop() throws NotifyRemotingException;


    public synchronized final void stop() throws NotifyRemotingException {
        if (!this.started) {
            return;
        }
        this.started = false;
        this.doStop();
        try {
            this.controller.stop();
        }
        catch (final IOException e) {
            throw new NotifyRemotingException("关闭连接错误", e);
        }
        this.remotingContext.dispose();
        this.scanAllConnectionExecutor.shutdown();
        try {
            if (!this.scanAllConnectionExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                this.scanAllConnectionExecutor.shutdownNow();
            }
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.attributes.clear();
        this.removeShutdownHook();
    }


    private void removeShutdownHook() {
        if (!this.isHutdownHookCalled && this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        }
    }


    public void insertTimer(final TimerRef timerRef) {
        if (timerRef == null) {
            throw new IllegalArgumentException("无效的timerRef,不能为null");
        }
        if (timerRef.getRunnable() == null) {
            throw new IllegalArgumentException("runnable不能为null，请指定超时运行的任务");
        }
        if (timerRef.getTimeout() <= 0) {
            throw new IllegalArgumentException("timeout必须大于0");
        }
        this.controller.getSelectorManager().insertTimer(timerRef);
    }


    public boolean isStarted() {
        return this.controller != null && this.controller.isStarted();
    }


    public BaseRemotingController(final BaseConfig baseConfig) {
        this.config = baseConfig;
        if (this.config == null) {
            throw new IllegalArgumentException("Null config object");
        }
        if (this.config.getWireFormatType() == null) {
            throw new IllegalArgumentException("Please set the wire format type");
        }
        this.remotingContext =
                new DefaultRemotingContext(this.config, this.config.getWireFormatType().newCommandFactory());
    }


    public <T extends RequestCommand> void registerProcessor(final Class<T> commandClazz,
            final RequestProcessor<T> processor) {
        if (commandClazz == null) {
            throw new NullPointerException("Null command class");
        }
        if (processor == null) {
            throw new NullPointerException("Null processor");
        }
        this.remotingContext.processorMap.put(commandClazz, processor);
    }


    public RequestProcessor<? extends RequestCommand> getProcessor(final Class<? extends RequestCommand> clazz) {
        return this.remotingContext.processorMap.get(clazz);
    }


    public RequestProcessor<? extends RequestCommand> unreigsterProcessor(final Class<? extends RequestCommand> clazz) {
        return this.remotingContext.processorMap.remove(clazz);
    }


    public void addAllProcessors(
            final Map<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>> map) {
        this.remotingContext.processorMap.putAll(map);
    }


    public void addConnectionLifeCycleListener(final ConnectionLifeCycleListener connectionLifeCycleListener) {
        this.remotingContext.connectionLifeCycleListenerList.add(connectionLifeCycleListener);

    }


    public void removeConnectionLifeCycleListener(final ConnectionLifeCycleListener connectionLifeCycleListener) {
        this.remotingContext.connectionLifeCycleListenerList.remove(connectionLifeCycleListener);
    }


    public void sendToAllConnections(final RequestCommand command) throws NotifyRemotingException {
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        final List<Connection> connnections = this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP);
        if (connnections != null) {
            for (final Connection conn : connnections) {
                if (conn.isConnected()) {
                    conn.send(command);
                }
            }
        }

    }


    protected Configuration getConfigurationFromConfig(final BaseConfig config) {
        final Configuration conf = new Configuration();
        conf.setSessionReadBufferSize(config.getReadBufferSize());
        conf.setSessionIdleTimeout(config.getIdleTime() * 1000);
        conf.setStatisticsServer(false);
        conf.setHandleReadWriteConcurrently(true);
        conf.setDispatchMessageThreadCount(config.getDispatchMessageThreadCount());
        conf.setReadThreadCount(config.getReadThreadCount());
        conf.setWriteThreadCount(config.getWriteThreadCount());
        return conf;
    }


    protected Map<SocketOption<?>, Object> getSocketOptionsFromConfig(final BaseConfig config) {
        final Map<SocketOption<?>, Object> result = new HashMap<SocketOption<?>, Object>();

        result.put(StandardSocketOption.SO_SNDBUF, config.getSndBufferSize());
        result.put(StandardSocketOption.SO_KEEPALIVE, config.isKeepAlive());
        if (config.isSoLinger()) {
            result.put(StandardSocketOption.SO_LINGER, config.getLinger());
        }
        result.put(StandardSocketOption.SO_RCVBUF, config.getRcvBufferSize());
        result.put(StandardSocketOption.SO_REUSEADDR, config.isReuseAddr());
        result.put(StandardSocketOption.TCP_NODELAY, config.isTcpNoDelay());
        return result;
    }


    public void sendToGroup(final String group, final RequestCommand command) throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, command);
        if (conn != null) {
            conn.send(command);
        }
        else {
            throw new NotifyRemotingException("分组" + group + "没有可用的连接");
        }
    }


    /**
     * 根据策略从分组中的连接选择一个
     * 
     * @param group
     * @param connectionSelector
     *            连接选择器
     * @return
     */
    public Connection selectConnectionForGroup(final String group, final ConnectionSelector connectionSelector,
            final RequestCommand request) throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        final List<Connection> connnections = this.remotingContext.getConnectionsByGroup(group);
        if (connnections != null) {
            return connectionSelector.select(group, request, connnections);
        }
        else {
            return null;
        }
    }


    public void sendToGroup(final String group, final RequestCommand command,
            final SingleRequestCallBackListener listener) throws NotifyRemotingException {
        this.sendToGroup(group, command, listener, this.opTimeout, TimeUnit.MILLISECONDS);
    }


    public void sendToGroup(final String group, final RequestCommand request,
            final SingleRequestCallBackListener listener, final long time, final TimeUnit timeunut)
            throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (request == null) {
            throw new NotifyRemotingException("Null command");
        }
        if (listener == null) {
            throw new NotifyRemotingException("Null listener");
        }
        if (timeunut == null) {
            throw new NotifyRemotingException("Null TimeUnit");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, request);
        if (conn != null) {
            conn.send(request, listener, time, timeunut);
        }
        else {
            if (listener != null) {
                final ThreadPoolExecutor executor = listener.getExecutor();
                if (executor != null) {
                    executor.execute(new Runnable() {
                        public void run() {
                            listener.onResponse(BaseRemotingController.this.createNoConnectionResponseCommand(request
                                .getRequestHeader()), null);
                        }
                    });
                }
                else {
                    listener.onResponse(this.createNoConnectionResponseCommand(request.getRequestHeader()), null);
                }
            }
        }

    }


    private BooleanAckCommand createNoConnectionResponseCommand(final CommandHeader requestHeader) {
        return this.createCommErrorResponseCommand(requestHeader, "无可用连接");
    }


    private BooleanAckCommand createCommErrorResponseCommand(final CommandHeader requestHeader, final String message) {
        final BooleanAckCommand responseCommand =
                this.remotingContext.getCommandFactory().createBooleanAckCommand(requestHeader,
                    ResponseStatus.ERROR_COMM, message);
        responseCommand.setResponseTime(System.currentTimeMillis());
        return responseCommand;
    }


    public ResponseCommand invokeToGroup(final String group, final RequestCommand command) throws InterruptedException,
            TimeoutException, NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, command);
        if (conn != null) {
            return conn.invoke(command);
        }
        else {
            return this.createNoConnectionResponseCommand(command.getRequestHeader());
        }
    }


    public ResponseCommand invokeToGroup(final String group, final RequestCommand command, final long time,
            final TimeUnit timeUnit) throws InterruptedException, TimeoutException, NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        if (timeUnit == null) {
            throw new NotifyRemotingException("Null TimeUnit");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, command);
        if (conn != null) {
            return conn.invoke(command, time, timeUnit);
        }
        else {
            return this.createNoConnectionResponseCommand(command.getRequestHeader());
        }
    }


    public void sendToGroupAllConnections(final String group, final RequestCommand command)
            throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);
        if (connections != null) {
            for (final Connection conn : connections) {
                if (conn.isConnected()) {
                    conn.send(command);
                }
            }
        }
    }


    public void sendToGroupAllConnections(final String group, final RequestCommand command,
            final GroupAllConnectionCallBackListener listener, final long timeout, final TimeUnit timeUnit)
            throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        if (timeUnit == null) {
            throw new NotifyRemotingException("Null timeUnit");
        }
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);

        if (connections != null && connections.size() > 0) {
            final CountDownLatch countDownLatch = new CountDownLatch(connections.size());
            final ConcurrentHashMap<Connection, ResponseCommand> resultMap =
                    new ConcurrentHashMap<Connection, ResponseCommand>();
            final long timeoutInMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
            // 创建callBack
            final GroupAllConnectionRequestCallBack requestCallBack =
                    new GroupAllConnectionRequestCallBack(listener, countDownLatch, timeoutInMillis,
                        System.currentTimeMillis(), resultMap);
            // 创建定时器引用
            final TimerRef timerRef =
                    new TimerRef(timeoutInMillis, new GroupAllConnectionCallBackRunner(requestCallBack, resultMap,
                        connections, command.getRequestHeader()));
            requestCallBack.setTimerRef(timerRef);

            for (final Connection conn : connections) {
                final DefaultConnection connection = (DefaultConnection) conn;
                if (connection.isConnected()) {
                    try {
                        connection.addRequestCallBack(command.getOpaque(), requestCallBack);
                        requestCallBack.addWriteFuture(connection, connection.asyncSend(command));
                    }
                    catch (final Throwable t) {
                        requestCallBack.onResponse(
                            group,
                            this.createCommErrorResponseCommand(command.getRequestHeader(),
                                "发送失败，root:" + t.getMessage()), connection);
                    }
                }
                else {
                    requestCallBack.onResponse(group,
                        this.createCommErrorResponseCommand(command.getRequestHeader(), "连接已经关闭"), connection);
                }
            }
            // 插入定时器
            this.insertTimer(timerRef);
        }
        else {
            if (listener != null) {
                if (listener.getExecutor() != null) {
                    listener.getExecutor().execute(new Runnable() {
                        public void run() {
                            listener.onResponse(new HashMap<Connection, ResponseCommand>());
                        }
                    });
                }
                else {
                    // 否则，直接返回空结果集
                    listener.onResponse(new HashMap<Connection, ResponseCommand>());
                }
            }
        }

    }


    public Map<Connection, ResponseCommand> invokeToGroupAllConnections(final String group,
            final RequestCommand command, final long time, final TimeUnit timeUnit) throws InterruptedException,
            NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        if (command == null) {
            throw new NotifyRemotingException("Null command");
        }
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);

        if (connections != null && connections.size() > 0) {
            final long now = System.currentTimeMillis();
            final CountDownLatch countDownLatch = new CountDownLatch(connections.size());
            final ConcurrentHashMap<Connection, ResponseCommand> resultMap =
                    new ConcurrentHashMap<Connection, ResponseCommand>();
            final GroupAllConnectionRequestCallBack requestCallBack =
                    new GroupAllConnectionRequestCallBack(null, countDownLatch, TimeUnit.MILLISECONDS.convert(time,
                        timeUnit), now, resultMap);

            for (final Connection conn : connections) {
                final DefaultConnection connection = (DefaultConnection) conn;
                if (connection.isConnected()) {
                    try {
                        connection.addRequestCallBack(command.getOpaque(), requestCallBack);
                        requestCallBack.addWriteFuture(connection, connection.asyncSend(command));
                    }
                    catch (final Throwable e) {
                        requestCallBack
                            .onResponse(group,
                                this.createCommErrorResponseCommand(command.getRequestHeader(), e.getMessage()),
                                connection);
                    }
                }
                else {
                    requestCallBack.onResponse(group,
                        this.createCommErrorResponseCommand(command.getRequestHeader(), "连接已经关闭"), connection);
                }
            }
            if (!countDownLatch.await(time, timeUnit)) {
                for (final Connection conn : connections) {
                    if (!resultMap.containsKey(conn)) {
                        if (resultMap.putIfAbsent(conn,
                            this.createTimeoutCommand(command.getRequestHeader(), conn.getRemoteSocketAddress())) == null) {
                            requestCallBack.cancelWrite(conn);
                            // 切记移除回调
                            ((DefaultConnection) conn).removeRequestCallBack(command.getOpaque());
                        }
                    }
                }
            }
            return resultMap;
        }
        else {
            return null;
        }
    }


    private BooleanAckCommand createTimeoutCommand(final CommandHeader requestHeader, final InetSocketAddress address) {
        final BooleanAckCommand value =
                this.remotingContext.getCommandFactory().createBooleanAckCommand(requestHeader, ResponseStatus.TIMEOUT,
                    "等待响应超时");
        value.setResponseStatus(ResponseStatus.TIMEOUT);
        value.setResponseTime(System.currentTimeMillis());
        value.setResponseHost(address);
        return value;
    }


    public Map<Connection, ResponseCommand> invokeToGroupAllConnections(final String group, final RequestCommand command)
            throws InterruptedException, NotifyRemotingException {
        return this.invokeToGroupAllConnections(group, command, this.opTimeout, TimeUnit.MILLISECONDS);
    }


    public void sendToGroupAllConnections(final String group, final RequestCommand command,
            final GroupAllConnectionCallBackListener listener) throws NotifyRemotingException {
        this.sendToGroupAllConnections(group, command, listener, this.opTimeout, TimeUnit.MILLISECONDS);
    }


    public void sendToGroups(final Map<String, RequestCommand> groupObjects, final MultiGroupCallBackListener listener,
            final long timeout, final TimeUnit timeUnit, final Object... args) throws NotifyRemotingException {
        if (groupObjects == null || groupObjects.size() == 0) {
            throw new NotifyRemotingException("groupObject为空");
        }
        if (listener == null) {
            throw new NotifyRemotingException("Null GroupCallBackListener");
        }
        if (timeUnit == null) {
            throw new NotifyRemotingException("Null TimeUnit");
        }
        // 结果收集
        final CountDownLatch countDownLatch = new CountDownLatch(groupObjects.size());
        final ConcurrentHashMap<String/* group */, ResponseCommand/* 应答 */> resultMap =
                new ConcurrentHashMap<String, ResponseCommand>();
        // 连接映射
        final Map<String/* group */, Connection> connectionMap = new HashMap<String, Connection>();
        // 请求映射
        final Map<String/* group */, CommandHeader> headerMap = new HashMap<String, CommandHeader>();
        // 防止重复响应的响应标记
        final AtomicBoolean responsed = new AtomicBoolean(false);

        InetSocketAddress remoteAddr = null;
        // 避免重复获取时间
        final long now = System.currentTimeMillis();

        final long timeoutInMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        // 创建callBack
        final MultiGroupRequestCallBack groupRequestCallBack =
                new MultiGroupRequestCallBack(listener, countDownLatch, timeoutInMillis, now, resultMap, responsed,
                    args);
        // 创建定时器引用
        final TimerRef timerRef =
                new TimerRef(timeoutInMillis, new GroupCallBackRunner(connectionMap, groupRequestCallBack, headerMap,
                    resultMap, remoteAddr));
        groupRequestCallBack.setTimerRef(timerRef);

        for (final Map.Entry<String, RequestCommand> entry : groupObjects.entrySet()) {
            final RequestCommand requestCommand = entry.getValue();
            final String group = entry.getKey();
            final DefaultConnection conn =
                    (DefaultConnection) this.selectConnectionForGroup(group, this.connectionSelector, requestCommand);

            if (conn != null) {
                try {
                    // 添加映射
                    connectionMap.put(group, conn);
                    // 添加应答的分组映射
                    conn.addOpaqueToGroupMapping(requestCommand.getOpaque(), group);
                    // 添加回调
                    conn.addRequestCallBack(requestCommand.getOpaque(), groupRequestCallBack);
                    if (remoteAddr == null) {
                        remoteAddr = conn.getRemoteSocketAddress();
                    }
                    groupRequestCallBack.addWriteFuture(conn, conn.asyncSend(requestCommand));
                    headerMap.put(group, requestCommand.getRequestHeader());
                }
                catch (final Throwable t) {
                    groupRequestCallBack.onResponse(group,
                        this.createCommErrorResponseCommand(requestCommand.getRequestHeader(), t.getMessage()), conn);
                }
            }
            else {
                // 直接返回错误应答
                groupRequestCallBack.onResponse(group,
                    this.createNoConnectionResponseCommand(requestCommand.getRequestHeader()), null);
            }
        }
        // 插入定时器
        this.insertTimer(timerRef);
    }

    /**
     * 单分组所有连接的回调执行线程
     * 
     * @author boyan
     * 
     * @since 1.0, 2009-12-16 下午03:41:29
     */
    private final class GroupAllConnectionCallBackRunner implements Runnable {
        private final ConcurrentHashMap<Connection, ResponseCommand> resultMap;
        private final GroupAllConnectionRequestCallBack requestCallBack;
        private final List<Connection> connections;
        final CommandHeader requestHeader;


        private GroupAllConnectionCallBackRunner(final GroupAllConnectionRequestCallBack requestCallBack,
                final ConcurrentHashMap<Connection, ResponseCommand> resultMap, final List<Connection> connections,
                final CommandHeader requestHeader) {
            this.requestCallBack = requestCallBack;
            this.resultMap = resultMap;
            this.connections = connections;
            this.requestHeader = requestHeader;
        }


        public void run() {
            for (final Connection conn : this.connections) {
                if (!this.resultMap.containsKey(conn)) {
                    this.requestCallBack.cancelWrite(conn);
                    this.requestCallBack.onResponse(
                        null,
                        BaseRemotingController.this.createTimeoutCommand(this.requestHeader,
                            conn.getRemoteSocketAddress()), conn);
                }
            }

        }
    }

    /**
     * 多分组发送回调执行线程
     * 
     * @author boyan
     * 
     * @since 1.0, 2009-12-16 下午02:00:27
     */
    protected class GroupCallBackRunner implements Runnable {
        final Map<String, CommandHeader> groupObjects;
        final Map<String/* group */, ResponseCommand/* 应答 */> resultMap;
        final Map<String /* group */, Connection> connectionMap;
        final MultiGroupRequestCallBack requestCallBack;
        final InetSocketAddress remoteAddr;


        public GroupCallBackRunner(final Map<String /* group */, Connection> connectionMap,
                final MultiGroupRequestCallBack requestCallBack, final Map<String, CommandHeader> groupObjects,
                final Map<String, ResponseCommand> resultMap, final InetSocketAddress remoteAddr) {
            super();
            this.connectionMap = connectionMap;
            this.requestCallBack = requestCallBack;
            this.groupObjects = groupObjects;
            this.resultMap = resultMap;
            this.remoteAddr = remoteAddr;
        }


        public void run() {
            for (final Map.Entry<String, CommandHeader> entry : this.groupObjects.entrySet()) {
                final String group = entry.getKey();
                // 没有返回应答的，设置超时
                if (!this.resultMap.containsKey(group)) {
                    final Connection connection = this.connectionMap.get(group);
                    if (connection != null) {
                        this.requestCallBack.cancelWrite(connection);
                        // 修复投递超时时, 日志中记录的host为null的情况
                        /*
                         * ResponseCommand value =
                         * BaseRemotingController.this.createTimeoutCommand
                         * (entry.getValue(), this.remoteAddr);
                         */
                        final ResponseCommand value =
                                BaseRemotingController.this.createTimeoutCommand(entry.getValue(),
                                    connection.getRemoteSocketAddress());
                        this.requestCallBack.onResponse(group, value, connection);
                    }
                    else {
                        this.requestCallBack.onResponse(group,
                            BaseRemotingController.this.createNoConnectionResponseCommand(entry.getValue()), null);
                    }
                }
            }
        }

    }


    public void sendToGroups(final Map<String, RequestCommand> groupObjects) throws NotifyRemotingException {
        if (groupObjects == null || groupObjects.size() == 0) {
            throw new NotifyRemotingException("groupObjects为空");
        }
        for (final Map.Entry<String, RequestCommand> entry : groupObjects.entrySet()) {
            final RequestCommand requestCommand = entry.getValue();
            final String group = entry.getKey();
            final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, requestCommand);
            if (conn != null) {
                conn.send(requestCommand);
            }
        }

    }


    public void transferToGroup(final String group, final IoBuffer head, final IoBuffer tail,
            final FileChannel channel, final long position, final long size, final Integer opaque,
            final SingleRequestCallBackListener listener, final long time, final TimeUnit unit)
            throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, null);
        if (conn != null) {
            conn.transferFrom(head, tail, channel, position, size, opaque, listener, time, unit);
        }
        else {
            throw new NotifyRemotingException("分组" + group + "没有可用的连接");
        }

    }


    public void transferToGroup(final String group, final IoBuffer head, final IoBuffer tail,
            final FileChannel channel, final long position, final long size) throws NotifyRemotingException {
        if (group == null) {
            throw new NotifyRemotingException("Null group");
        }
        final Connection conn = this.selectConnectionForGroup(group, this.connectionSelector, null);
        if (conn != null) {
            conn.transferFrom(head, tail, channel, position, size);
        }
        else {
            throw new NotifyRemotingException("分组" + group + "没有可用的连接");
        }

    }


    public void setConnectionSelector(final ConnectionSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("Null selector");
        }
        this.connectionSelector = selector;

    }


    public Object getAttribute(final String url, final String key) {
        final ConcurrentHashMap<String, Object> subAttr = this.attributes.get(url);
        if (subAttr == null) {
            return null;
        }
        return subAttr.get(key);
    }


    public Object removeAttribute(final String url, final String key) {
        final ConcurrentHashMap<String, Object> subAttr = this.attributes.get(url);
        if (subAttr == null) {
            return null;
        }
        return subAttr.remove(key);
    }


    public void setAttribute(final String url, final String key, final Object value) {
        ConcurrentHashMap<String, Object> subAttr = this.attributes.get(url);
        if (subAttr == null) {
            subAttr = new ConcurrentHashMap<String, Object>();
            final ConcurrentHashMap<String, Object> oldSubAttr = this.attributes.putIfAbsent(url, subAttr);
            if (oldSubAttr != null) {
                subAttr = oldSubAttr;
            }
        }
        subAttr.put(key, value);

    }


    public Object setAttributeIfAbsent(final String url, final String key, final Object value) {
        ConcurrentHashMap<String, Object> subAttr = this.attributes.get(url);
        if (subAttr == null) {
            subAttr = new ConcurrentHashMap<String, Object>();
            final ConcurrentHashMap<String, Object> oldSubAttr = this.attributes.putIfAbsent(url, subAttr);
            if (oldSubAttr != null) {
                subAttr = oldSubAttr;
            }
        }
        return subAttr.putIfAbsent(key, value);
    }


    public int getConnectionCount(final String group) {
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(group);
        return connections == null ? 0 : connections.size();
    }


    public Set<String> getGroupSet() {
        return this.remotingContext.getGroupSet();
    }

}