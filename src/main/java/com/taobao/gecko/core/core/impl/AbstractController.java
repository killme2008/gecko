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
package com.taobao.gecko.core.core.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Controller;
import com.taobao.gecko.core.core.ControllerLifeCycle;
import com.taobao.gecko.core.core.ControllerStateListener;
import com.taobao.gecko.core.core.Dispatcher;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.SocketOption;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.statistics.Statistics;
import com.taobao.gecko.core.statistics.impl.DefaultStatistics;
import com.taobao.gecko.core.statistics.impl.SimpleStatistics;
import com.taobao.gecko.core.util.DispatcherFactory;
import com.taobao.gecko.core.util.LinkedTransferQueue;


/**
 * Controller抽象基类
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:03:07
 */
public abstract class AbstractController implements Controller, ControllerLifeCycle {

    protected Statistics statistics = new DefaultStatistics();
    protected long statisticsInterval;

    protected static final Log log = LogFactory.getLog(AbstractController.class);
    /**
     * controller state listener list
     */
    protected CopyOnWriteArrayList<ControllerStateListener> stateListeners =
            new CopyOnWriteArrayList<ControllerStateListener>();
    /**
     * Event handler
     */
    protected Handler handler;
    /**
     * Codec Factory
     */

    protected volatile CodecFactory codecFactory;
    /**
     * Status
     */
    protected volatile boolean started;
    /**
     * local bind address
     */
    protected InetSocketAddress localSocketAddress;
    /**
     * Read event processing thread count
     */
    protected int readThreadCount;
    protected int writeThreadCount;
    protected int dispatchMessageThreadCount;
    protected Configuration configuration;
    protected Dispatcher readEventDispatcher, dispatchMessageDispatcher, writeEventDispatcher;
    protected long sessionTimeout;
    protected volatile boolean handleReadWriteConcurrently = true;

    protected int soTimeout;

    private Thread shutdownHookThread;
    private volatile boolean isHutdownHookCalled = false;

    /**
     * Socket options
     */
    protected Map<SocketOption<?>, Object> socketOptions = new HashMap<SocketOption<?>, Object>();


    public void setSocketOptions(final Map<SocketOption<?>, Object> socketOptions) {
        if (socketOptions == null) {
            throw new NullPointerException("Null socketOptions");
        }
        this.socketOptions = socketOptions;
    }

    /**
     * Connected session set
     */
    private final Set<Session> sessionSet = new HashSet<Session>();


    public final int getDispatchMessageThreadCount() {
        return this.dispatchMessageThreadCount;
    }


    public final void setDispatchMessageThreadCount(final int dispatchMessageThreadPoolSize) {
        if (this.started) {
            throw new IllegalStateException("Controller is started");
        }
        if (dispatchMessageThreadPoolSize < 0) {
            throw new IllegalArgumentException("dispatchMessageThreadPoolSize<0");
        }
        this.dispatchMessageThreadCount = dispatchMessageThreadPoolSize;
    }


    public long getSessionIdleTimeout() {
        return this.configuration.getSessionIdleTimeout();
    }


    /**
     * Build write queue for session
     * 
     * @return
     */
    protected Queue<WriteMessage> buildQueue() {
        return new LinkedTransferQueue<WriteMessage>();
    }


    public void setSessionIdleTimeout(final long sessionIdleTimeout) {
        this.configuration.setSessionIdleTimeout(sessionIdleTimeout);

    }


    public long getSessionTimeout() {
        return this.sessionTimeout;
    }


    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }


    public int getSoTimeout() {
        return this.soTimeout;
    }


    public void setSoTimeout(final int timeout) {
        this.soTimeout = timeout;
    }


    public AbstractController() {
        this(new Configuration(), null, null);
    }


    public double getReceiveThroughputLimit() {
        return this.statistics.getReceiveThroughputLimit();
    }


    public void setReceiveThroughputLimit(final double receiveThroughputLimit) {
        this.statistics.setReceiveThroughputLimit(receiveThroughputLimit);

    }


    public AbstractController(final Configuration configuration) {
        this(configuration, null, null);

    }


    public AbstractController(final Configuration configuration, final CodecFactory codecFactory) {
        this(configuration, null, codecFactory);
    }


    public AbstractController(final Configuration configuration, final Handler handler, final CodecFactory codecFactory) {
        this.init(configuration, handler, codecFactory);
    }


    private synchronized void init(final Configuration configuration, final Handler handler,
            final CodecFactory codecFactory) {
        this.setHandler(handler);
        this.setCodecFactory(codecFactory);
        this.setConfiguration(configuration);
        this.setReadThreadCount(configuration.getReadThreadCount());
        this.setWriteThreadCount(configuration.getWriteThreadCount());
        this.setDispatchMessageThreadCount(configuration.getDispatchMessageThreadCount());
        this.setHandleReadWriteConcurrently(configuration.isHandleReadWriteConcurrently());
        this.setSoTimeout(configuration.getSoTimeout());
        this.setStatisticsConfig(configuration);
        this.setReceiveThroughputLimit(-0.1d);
        this.setStarted(false);
    }


    void setStarted(final boolean started) {
        this.started = started;
    }


    private void setStatisticsConfig(final Configuration configuration) {
        if (configuration.isStatisticsServer()) {
            this.statistics = new SimpleStatistics();
            this.statisticsInterval = configuration.getStatisticsInterval();

        }
        else {
            this.statistics = new DefaultStatistics();
            this.statisticsInterval = -1;
        }
    }


    public Configuration getConfiguration() {
        return this.configuration;
    }


    public void setConfiguration(final Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Null Configuration");
        }
        this.configuration = configuration;
    }


    public InetSocketAddress getLocalSocketAddress() {
        return this.localSocketAddress;
    }


    public void setLocalSocketAddress(final InetSocketAddress inetSocketAddress) {
        this.localSocketAddress = inetSocketAddress;
    }


    public void onAccept(final SelectionKey sk) throws IOException {
        this.statistics.statisticsAccept();
    }


    public void onConnect(final SelectionKey key) throws IOException {
        throw new UnsupportedOperationException();
    }


    public void addStateListener(final ControllerStateListener listener) {
        this.stateListeners.add(listener);
    }


    public void removeStateListener(final ControllerStateListener listener) {
        this.stateListeners.remove(listener);
    }


    public boolean isHandleReadWriteConcurrently() {
        return this.handleReadWriteConcurrently;
    }


    public void setHandleReadWriteConcurrently(final boolean handleReadWriteConcurrently) {
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    }


    public int getReadThreadCount() {
        return this.readThreadCount;
    }


    public void setReadThreadCount(final int readThreadCount) {
        if (this.started) {
            throw new IllegalStateException();
        }
        if (readThreadCount < 0) {
            throw new IllegalArgumentException("readThreadCount<0");
        }
        this.readThreadCount = readThreadCount;
    }


    public final int getWriteThreadCount() {
        return this.writeThreadCount;
    }


    public final void setWriteThreadCount(final int writeThreadCount) {
        if (this.started) {
            throw new IllegalStateException();
        }
        if (writeThreadCount < 0) {
            throw new IllegalArgumentException("readThreadCount<0");
        }
        this.writeThreadCount = writeThreadCount;
    }


    public Handler getHandler() {
        return this.handler;
    }


    public void setHandler(final Handler handler) {
        if (this.started) {
            throw new IllegalStateException("The Controller have started");
        }
        this.handler = handler;
    }


    public int getPort() {
        if (this.localSocketAddress != null) {
            return this.localSocketAddress.getPort();
        }
        throw new NullPointerException("Controller is not binded");
    }


    public synchronized void start() throws IOException {
        if (this.isStarted()) {
            return;
        }
        if (this.getHandler() == null) {
            throw new IOException("The handler is null");
        }
        if (this.getCodecFactory() == null) {
            this.setCodecFactory(new ByteBufferCodecFactory());
        }
        this.setStarted(true);
        this.setReadEventDispatcher(DispatcherFactory.newDispatcher(this.getReadThreadCount(),
            "notify-remoting-ReadEvent", new ThreadPoolExecutor.CallerRunsPolicy()));
        this.setWriteEventDispatcher(DispatcherFactory.newDispatcher(this.getWriteThreadCount(),
            "notify-remoting-WriteEvent", new ThreadPoolExecutor.CallerRunsPolicy()));
        this.setDispatchMessageDispatcher(DispatcherFactory.newDispatcher(this.getDispatchMessageThreadCount(),
            "notify-remoting-DispatchMessage", new ThreadPoolExecutor.CallerRunsPolicy()));
        this.startStatistics();
        this.start0();
        this.notifyStarted();
        this.addShutdownHook();
        log.warn("The Controller started at " + this.localSocketAddress + " ...");
    }


    private void addShutdownHook() {
        this.shutdownHookThread = new Thread() {
            @Override
            public void run() {
                try {
                    AbstractController.this.isHutdownHookCalled = true;
                    AbstractController.this.stop();
                }
                catch (final IOException e) {
                    log.error("Stop controller fail", e);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(this.shutdownHookThread);
    }


    protected abstract void start0() throws IOException;


    void setDispatchMessageDispatcher(final Dispatcher dispatcher) {
        final Dispatcher oldDispatcher = this.dispatchMessageDispatcher;
        this.dispatchMessageDispatcher = dispatcher;
        if (oldDispatcher != null) {
            oldDispatcher.stop();
        }
    }


    Dispatcher getReadEventDispatcher() {
        return this.readEventDispatcher;
    }


    void setReadEventDispatcher(final Dispatcher dispatcher) {
        final Dispatcher oldDispatcher = this.readEventDispatcher;
        this.readEventDispatcher = dispatcher;
        if (oldDispatcher != null) {
            oldDispatcher.stop();
        }
    }


    void setWriteEventDispatcher(final Dispatcher dispatcher) {
        final Dispatcher oldDispatcher = this.writeEventDispatcher;
        this.writeEventDispatcher = dispatcher;
        if (oldDispatcher != null) {
            oldDispatcher.stop();
        }
    }


    private final void startStatistics() {
        this.statistics.start();
    }


    public void notifyStarted() {
        for (final ControllerStateListener stateListener : this.stateListeners) {
            stateListener.onStarted(this);
        }
    }


    public boolean isStarted() {
        return this.started;
    }


    public final Statistics getStatistics() {
        return this.statistics;
    }


    public final CodecFactory getCodecFactory() {
        return this.codecFactory;
    }


    public final void setCodecFactory(final CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }


    public void notifyReady() {
        for (final ControllerStateListener stateListener : this.stateListeners) {
            stateListener.onReady(this);
        }
    }


    public synchronized final void unregisterSession(final Session session) {
        this.sessionSet.remove(session);
        if (this.sessionSet.isEmpty()) {
            this.notifyAllSessionClosed();
        }
    }


    public void checkStatisticsForRestart() {
        if (this.statisticsInterval > 0
                && System.currentTimeMillis() - this.statistics.getStartedTime() > this.statisticsInterval * 1000) {
            this.statistics.restart();
        }
    }


    public final synchronized void registerSession(final Session session) {
        if (this.started && !session.isClosed()) {
            this.sessionSet.add(session);
        }
        else {
            session.close();
        }

    }


    public void stop() throws IOException {
        Set<Session> copySet = null;
        synchronized (this) {
            if (!this.isStarted()) {
                return;
            }
            this.setStarted(false);
            copySet = new HashSet<Session>(this.sessionSet);
            this.sessionSet.clear();
        }
        if (copySet != null) {
            for (final Session session : copySet) {
                ((AbstractSession) session).close0();
            }
        }
        this.stopStatistics();
        this.stopDispatcher();
        this.notifyStopped();
        this.clearStateListeners();
        this.stop0();
        this.removeShutdownHook();
        log.info("Controller has been stopped.");

    }


    private void removeShutdownHook() {
        if (!this.isHutdownHookCalled && this.shutdownHookThread != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHookThread);
        }
    }


    protected abstract void stop0() throws IOException;


    private final void stopDispatcher() {
        if (this.readEventDispatcher != null) {
            this.readEventDispatcher.stop();
        }
        if (this.dispatchMessageDispatcher != null) {
            this.dispatchMessageDispatcher.stop();
        }
        if (this.writeEventDispatcher != null) {
            this.writeEventDispatcher.stop();
        }
    }


    private final void stopStatistics() {
        this.statistics.stop();
    }


    private final void clearStateListeners() {
        this.stateListeners.clear();
    }


    public final void notifyException(final Throwable t) {
        for (final ControllerStateListener stateListener : this.stateListeners) {
            stateListener.onException(this, t);
        }
    }


    public final void notifyStopped() {
        for (final ControllerStateListener stateListener : this.stateListeners) {
            stateListener.onStopped(this);
        }
    }


    public final void notifyAllSessionClosed() {
        for (final ControllerStateListener stateListener : this.stateListeners) {
            stateListener.onAllSessionClosed(this);
        }
    }


    public Set<Session> getSessionSet() {
        return Collections.unmodifiableSet(this.sessionSet);
    }


    public <T> void setSocketOption(final SocketOption<T> socketOption, final T value) {
        if (socketOption == null) {
            throw new NullPointerException("Null socketOption");
        }
        if (value == null) {
            throw new NullPointerException("Null value");
        }
        if (!socketOption.type().equals(value.getClass())) {
            throw new IllegalArgumentException("Expected " + socketOption.type().getSimpleName() + " value,but givend "
                    + value.getClass().getSimpleName());
        }
        this.socketOptions.put(socketOption, value);
    }


    @SuppressWarnings("unchecked")
    public <T> T getSocketOption(final SocketOption<T> socketOption) {
        return (T) this.socketOptions.get(socketOption);
    }


    /**
     * Bind localhost address
     * 
     * @param inetSocketAddress
     * @throws IOException
     */
    public void bind(final InetSocketAddress inetSocketAddress) throws IOException {
        if (inetSocketAddress == null) {
            throw new IllegalArgumentException("Null inetSocketAddress");
        }
        this.setLocalSocketAddress(inetSocketAddress);
        this.start();
    }
}