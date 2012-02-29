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
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Dispatcher;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.SessionConfig;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.statistics.Statistics;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 连接抽象基类
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:04:05
 */
public abstract class AbstractSession implements Session {

    protected IoBuffer readBuffer;
    protected static final Log log = LogFactory.getLog(AbstractSession.class);

    protected final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    protected Queue<WriteMessage> writeQueue;

    protected volatile long sessionIdleTimeout;

    protected volatile long sessionTimeout;


    public long getSessionIdleTimeout() {
        return this.sessionIdleTimeout;
    }


    public void setSessionIdleTimeout(final long sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }


    public long getSessionTimeout() {
        return this.sessionTimeout;
    }


    public void setSessionTimeout(final long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }


    public Queue<WriteMessage> getWriteQueue() {
        return this.writeQueue;
    }


    public Statistics getStatistics() {
        return this.statistics;
    }


    public Handler getHandler() {
        return this.handler;
    }


    public Dispatcher getDispatchMessageDispatcher() {
        return this.dispatchMessageDispatcher;
    }


    public ReentrantLock getWriteLock() {
        return this.writeLock;
    }

    protected CodecFactory.Encoder encoder;
    protected CodecFactory.Decoder decoder;

    protected volatile boolean closed, innerClosed;

    protected Statistics statistics;

    protected Handler handler;

    protected boolean loopback;

    public AtomicLong lastOperationTimeStamp = new AtomicLong(0);

    protected AtomicLong scheduleWritenBytes = new AtomicLong(0);

    protected final Dispatcher dispatchMessageDispatcher;
    protected volatile boolean useBlockingWrite = false;
    protected volatile boolean useBlockingRead = true;
    protected volatile boolean handleReadWriteConcurrently = true;


    public abstract void decode();


    public void updateTimeStamp() {
        this.lastOperationTimeStamp.set(System.currentTimeMillis());
    }


    public long getLastOperationTimeStamp() {
        return this.lastOperationTimeStamp.get();
    }


    public final boolean isHandleReadWriteConcurrently() {
        return this.handleReadWriteConcurrently;
    }


    public final void setHandleReadWriteConcurrently(final boolean handleReadWriteConcurrently) {
        this.handleReadWriteConcurrently = handleReadWriteConcurrently;
    }


    public long getScheduleWritenBytes() {
        return this.scheduleWritenBytes.get();
    }


    public CodecFactory.Encoder getEncoder() {
        return this.encoder;
    }


    public void setEncoder(final CodecFactory.Encoder encoder) {
        this.encoder = encoder;
    }


    public CodecFactory.Decoder getDecoder() {
        return this.decoder;
    }


    public IoBuffer getReadBuffer() {
        return this.readBuffer;
    }


    public void setReadBuffer(final IoBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }


    public void setDecoder(final CodecFactory.Decoder decoder) {
        this.decoder = decoder;
    }


    public final ByteOrder getReadBufferByteOrder() {
        if (this.readBuffer == null) {
            throw new IllegalStateException();
        }
        return this.readBuffer.order();
    }


    public final void setReadBufferByteOrder(final ByteOrder readBufferByteOrder) {
        if (this.readBuffer == null) {
            throw new NullPointerException("Null ReadBuffer");
        }
        this.readBuffer.order(readBufferByteOrder);
    }


    // 同步，防止多个reactor并发调用此方法
    protected synchronized void onIdle() {
        try {
            // 再次检测，防止重复调用
            if (this.isIdle()) {
                this.onIdle0();
                this.handler.onSessionIdle(this);
                this.updateTimeStamp();
            }
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }


    protected void onIdle0() {
        // callback for sub class
    }


    protected void onConnected() {
        try {
            this.handler.onSessionConnected(this, null);
        }
        catch (final Throwable throwable) {
            this.onException(throwable);
        }
    }


    public void onExpired() {
        try {
            if (this.isExpired() && !this.isClosed()) {
                this.handler.onSessionExpired(this);
                this.close();
            }
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }


    protected abstract WriteMessage wrapMessage(Object msg, Future<Boolean> writeFuture);


    /**
     * Pre-Process WriteMessage before writing to channel
     * 
     * @param writeMessage
     * @return
     */
    protected WriteMessage preprocessWriteMessage(final WriteMessage writeMessage) {
        return writeMessage;
    }


    protected void dispatchReceivedMessage(final Object message) {
        if (this.dispatchMessageDispatcher == null) {
            long start = -1;
            if (this.statistics != null && this.statistics.isStatistics()) {
                start = System.currentTimeMillis();
            }
            this.onMessage(message, this);
            if (start != -1) {
                this.statistics.statisticsProcess(System.currentTimeMillis() - start);
            }
        }
        else {
            this.dispatchMessageDispatcher.dispatch(new Runnable() {
                public void run() {
                    long start = -1;
                    if (AbstractSession.this.statistics != null && AbstractSession.this.statistics.isStatistics()) {
                        start = System.currentTimeMillis();
                    }
                    AbstractSession.this.onMessage(message, AbstractSession.this);
                    if (start != -1) {
                        AbstractSession.this.statistics.statisticsProcess(System.currentTimeMillis() - start);
                    }
                }

            });

        }

    }


    private void onMessage(final Object message, final Session session) {
        try {
            this.handler.onMessageReceived(session, message);
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }


    public final boolean isClosed() {
        return this.closed;
    }


    public final void setClosed(final boolean closed) {
        this.closed = closed;
    }


    public final void close() {
        this.setClosed(true);
        // 加入毒丸到队列
        this.addPoisonWriteMessage(new PoisonWriteMessage());

    }


    protected abstract void addPoisonWriteMessage(PoisonWriteMessage poisonWriteMessage);


    protected void close0() {
        synchronized (this) {
            if (this.innerClosed) {
                return;
            }
            this.innerClosed = true;
            this.setClosed(true);
        }
        try {
            this.closeChannel();
            log.debug("session closed");
        }
        catch (final IOException e) {
            this.onException(e);
            log.error("Close session error", e);
        }
        finally {
            // 如果最后一个消息已经完全写入，告知用户
            final WriteMessage writeMessage = this.writeQueue.poll();
            if (writeMessage != null && !writeMessage.hasRemaining()) {
                this.onMessageSent(writeMessage);
            }

            for (final WriteMessage msg : this.writeQueue) {
                if (msg != null && msg.getWriteFuture() != null) {
                    msg.getWriteFuture().failure(new NotifyRemotingException("连接已经关闭"));
                }
            }
            this.onClosed();
            this.clearAttributes();
            this.writeQueue.clear();
        }
    }


    protected abstract void closeChannel() throws IOException;


    public void onException(final Throwable e) {
        this.handler.onExceptionCaught(this, e);
    }


    protected void onClosed() {
        try {
            this.handler.onSessionClosed(this);
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }


    public void setAttribute(final String key, final Object value) {
        this.attributes.put(key, value);
    }


    public Set<String> attributeKeySet() {
        return this.attributes.keySet();
    }


    public Object setAttributeIfAbsent(final String key, final Object value) {
        return this.attributes.putIfAbsent(key, value);
    }


    public void removeAttribute(final String key) {
        this.attributes.remove(key);
    }


    public Object getAttribute(final String key) {
        return this.attributes.get(key);
    }


    public void clearAttributes() {
        this.attributes.clear();
    }


    public synchronized void start() {
        log.debug("session started");
        this.onStarted();
        this.start0();
    }


    protected abstract void start0();


    protected void onStarted() {
        try {
            this.handler.onSessionStarted(this);
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }

    protected ReentrantLock writeLock = new ReentrantLock();

    protected AtomicReference<WriteMessage> currentMessage = new AtomicReference<WriteMessage>();

    static final class FailFuture implements Future<Boolean> {

        public boolean cancel(final boolean mayInterruptIfRunning) {
            return Boolean.FALSE;
        }


        public Boolean get() throws InterruptedException, ExecutionException {
            return Boolean.FALSE;
        }


        public Boolean get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
            return Boolean.FALSE;
        }


        public boolean isCancelled() {
            return false;
        }


        public boolean isDone() {
            return true;
        }

    }


    public Future<Boolean> asyncWrite(final Object packet) {
        if (this.isClosed()) {
            final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
            writeFuture.failure(new IOException("连接已经被关闭"));
            return writeFuture;
        }
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }
        final FutureImpl<Boolean> writeFuture = new FutureImpl<Boolean>();
        final WriteMessage message = this.wrapMessage(packet, writeFuture);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
        return writeFuture;
    }


    public void write(final Object packet) {
        if (packet == null) {
            throw new NullPointerException("Null packet");
        }
        if (this.isClosed()) {
            return;
        }
        final WriteMessage message = this.wrapMessage(packet, null);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
    }


    protected abstract void writeFromUserCode(WriteMessage message);


    public final boolean isLoopbackConnection() {
        return this.loopback;
    }


    public boolean isUseBlockingWrite() {
        return this.useBlockingWrite;
    }


    public void setUseBlockingWrite(final boolean useBlockingWrite) {
        this.useBlockingWrite = useBlockingWrite;
    }


    public boolean isUseBlockingRead() {
        return this.useBlockingRead;
    }


    public void setUseBlockingRead(final boolean useBlockingRead) {
        this.useBlockingRead = useBlockingRead;
    }


    public void clearWriteQueue() {
        this.writeQueue.clear();
    }


    public boolean isExpired() {
        return false;
    }


    public boolean isIdle() {
        final long lastOpTimestamp = this.getLastOperationTimeStamp();
        return lastOpTimestamp > 0 && System.currentTimeMillis() - lastOpTimestamp > this.sessionIdleTimeout;
    }


    public AbstractSession(final SessionConfig sessionConfig) {
        super();
        this.lastOperationTimeStamp.set(System.currentTimeMillis());
        this.statistics = sessionConfig.statistics;
        this.handler = sessionConfig.handler;
        this.writeQueue = sessionConfig.queue;
        this.encoder = sessionConfig.codecFactory.getEncoder();
        this.decoder = sessionConfig.codecFactory.getDecoder();
        this.dispatchMessageDispatcher = sessionConfig.dispatchMessageDispatcher;
        this.handleReadWriteConcurrently = sessionConfig.handleReadWriteConcurrently;
        this.sessionTimeout = sessionConfig.sessionTimeout;
        this.sessionIdleTimeout = sessionConfig.sessionIdelTimeout;
    }


    protected void onCreated() {
        try {
            this.handler.onSessionCreated(this);
        }
        catch (final Throwable e) {
            this.onException(e);
        }
    }


    protected void onMessageSent(final WriteMessage message) {
        this.handler.onMessageSent(this, message.getMessage());
    }
}