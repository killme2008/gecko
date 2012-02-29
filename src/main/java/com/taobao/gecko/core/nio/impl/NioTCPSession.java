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
package com.taobao.gecko.core.nio.impl;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Future;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.ByteBufferCodecFactory;
import com.taobao.gecko.core.core.impl.ByteBufferWriteMessage;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.nio.NioSessionConfig;
import com.taobao.gecko.core.nio.input.ChannelInputStream;
import com.taobao.gecko.core.nio.output.ChannelOutputStream;
import com.taobao.gecko.core.util.ByteBufferUtils;
import com.taobao.gecko.core.util.SelectorFactory;


/**
 * Nio tcp连接
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:09:15
 */
public class NioTCPSession extends AbstractNioSession {
    private InetSocketAddress remoteAddress;
    private final int initialReadBufferSize;
    private int recvBufferSize = 16 * 1024;


    @Override
    public final boolean isExpired() {
        if (log.isDebugEnabled()) {
            log.debug("sessionTimeout=" + this.sessionTimeout + ",this.timestamp=" + this.lastOperationTimeStamp.get()
                    + ",current=" + System.currentTimeMillis());
        }
        return this.sessionTimeout <= 0 ? false
                : System.currentTimeMillis() - this.lastOperationTimeStamp.get() >= this.sessionTimeout;
    }


    public NioTCPSession(final NioSessionConfig sessionConfig, final int readRecvBufferSize) {
        super(sessionConfig);
        if (this.selectableChannel != null && this.getRemoteSocketAddress() != null) {
            this.loopback = this.getRemoteSocketAddress().getAddress().isLoopbackAddress();
        }
        this.setReadBuffer(IoBuffer.allocate(readRecvBufferSize));
        this.initialReadBufferSize = this.readBuffer.capacity();
        this.onCreated();
        try {
            this.recvBufferSize = ((SocketChannel) this.selectableChannel).socket().getReceiveBufferSize();
        }
        catch (final Exception e) {
            log.error("Get socket receive buffer size failed", e);
        }
    }


    protected final long doRealWrite(final SelectableChannel channel, final WriteMessage message) throws IOException {
        return message.write((WritableByteChannel) channel);
    }

    /**
     * 如果写入返回为0，强制循环多次，提高发送效率
     */
    static final int WRITE_SPIN_COUNT = Integer.parseInt(System.getProperty("notify.remoting.write_spin_count", "16"));


    @Override
    protected Object writeToChannel0(final WriteMessage message) throws IOException {
        if (message.getWriteFuture() != null && !message.isWriting() && message.getWriteFuture().isCancelled()) {
            this.scheduleWritenBytes.addAndGet(0 - message.remaining());
            return message.getMessage();
        }
        if (!message.hasRemaining()) {
            if (message.getWriteFuture() != null) {
                message.getWriteFuture().setResult(Boolean.TRUE);
            }
            return message.getMessage();
        }
        // begin writing
        message.writing();
        if (this.useBlockingWrite) {
            return this.blockingWrite(this.selectableChannel, message, message);
        }
        else {
            for (int i = 0; i < WRITE_SPIN_COUNT; i++) {
                final long n = this.doRealWrite(this.selectableChannel, message);
                if (n > 0) {
                    this.statistics.statisticsWrite(n);
                    this.scheduleWritenBytes.addAndGet(0 - n);
                    break;
                }
            }
            if (!message.hasRemaining()) {
                if (message.getWriteFuture() != null) {
                    message.getWriteFuture().setResult(Boolean.TRUE);
                }
                return message.getMessage();
            }
            // have more data, but the buffer is full,
            // wait next time to write
            return null;
        }

    }


    public InetSocketAddress getRemoteSocketAddress() {
        if (this.remoteAddress == null) {
            if (this.selectableChannel instanceof SocketChannel) {
                this.remoteAddress =
                        (InetSocketAddress) ((SocketChannel) this.selectableChannel).socket().getRemoteSocketAddress();
            }
        }
        return this.remoteAddress;
    }


    /**
     * 阻塞写，采用temp selector强制写入
     * 
     * @param channel
     * @param message
     * @param writeBuffer
     * @return
     * @throws IOException
     * @throws ClosedChannelException
     */
    protected final Object blockingWrite(final SelectableChannel channel, final WriteMessage message,
            final WriteMessage writeBuffer) throws IOException, ClosedChannelException {
        SelectionKey tmpKey = null;
        Selector writeSelector = null;
        int attempts = 0;
        int bytesProduced = 0;
        try {
            while (writeBuffer.hasRemaining()) {
                final long len = this.doRealWrite(channel, writeBuffer);
                if (len > 0) {
                    attempts = 0;
                    bytesProduced += len;
                    this.statistics.statisticsWrite(len);
                }
                else {
                    attempts++;
                    if (writeSelector == null) {
                        writeSelector = SelectorFactory.getSelector();
                        if (writeSelector == null) {
                            // Continue using the main one.
                            continue;
                        }
                        tmpKey = channel.register(writeSelector, SelectionKey.OP_WRITE);
                    }
                    if (writeSelector.select(1000) == 0) {
                        if (attempts > 2) {
                            throw new IOException("Client disconnected");
                        }
                    }
                }
            }
            if (!writeBuffer.hasRemaining() && message.getWriteFuture() != null) {
                message.getWriteFuture().setResult(Boolean.TRUE);
            }
        }
        finally {
            if (tmpKey != null) {
                tmpKey.cancel();
                tmpKey = null;
            }
            if (writeSelector != null) {
                // Cancel the key.
                writeSelector.selectNow();
                SelectorFactory.returnSelector(writeSelector);
            }
            this.scheduleWritenBytes.addAndGet(0 - bytesProduced);
        }
        return message.getMessage();
    }


    @Override
    protected WriteMessage wrapMessage(final Object msg, final Future<Boolean> writeFuture) {
        final ByteBufferWriteMessage message = new ByteBufferWriteMessage(msg, (FutureImpl<Boolean>) writeFuture);
        if (message.getWriteBuffer() == null) {
            message.setWriteBuffer(this.encoder.encode(message.getMessage(), this));
        }
        return message;
    }


    @Override
    protected void readFromBuffer() {
        if (!this.readBuffer.hasRemaining()) {
            this.readBuffer =
                    IoBuffer.wrap(ByteBufferUtils.increaseBufferCapatity(this.readBuffer.buf(), this.recvBufferSize));
        }
        int n = -1;
        int readCount = 0;
        try {
            while ((n = ((ReadableByteChannel) this.selectableChannel).read(this.readBuffer.buf())) > 0) {
                readCount += n;
                // readBuffer没有空间，跳出循环
                if (!this.readBuffer.hasRemaining()) {
                    break;
                }
            }
            if (readCount > 0) {
                this.readBuffer.flip();
                this.decode();
                this.readBuffer.compact();
            }
            else if (readCount == 0 && this.useBlockingRead) {
                if (this.selectableChannel instanceof SocketChannel
                        && !((SocketChannel) this.selectableChannel).socket().isInputShutdown()) {
                    n = this.blockingRead();
                }
                if (n > 0) {
                    readCount += n;
                }
            }
            if (n < 0) { // Connection closed
                this.close0();
            }
            else {
                this.selectorManager.registerSession(this, EventType.ENABLE_READ);
            }
            if (log.isDebugEnabled()) {
                log.debug("read " + readCount + " bytes from channel");
            }
        }
        catch (final ClosedChannelException e) {
            // ignore，不需要用户知道
            this.close0();
        }
        catch (final Throwable e) {
            this.close0();
            this.onException(e);

        }
    }


    protected final int blockingRead() throws ClosedChannelException, IOException {
        int n = 0;
        final Selector readSelector = SelectorFactory.getSelector();
        SelectionKey tmpKey = null;
        try {
            if (this.selectableChannel.isOpen()) {
                tmpKey = this.selectableChannel.register(readSelector, 0);
                tmpKey.interestOps(tmpKey.interestOps() | SelectionKey.OP_READ);
                final int code = readSelector.select(500);
                tmpKey.interestOps(tmpKey.interestOps() & ~SelectionKey.OP_READ);
                if (code > 0) {
                    do {
                        n = ((ReadableByteChannel) this.selectableChannel).read(this.readBuffer.buf());
                        if (log.isDebugEnabled()) {
                            log.debug("use temp selector read " + n + " bytes");
                        }
                    } while (n > 0 && this.readBuffer.hasRemaining());
                    this.readBuffer.flip();
                    this.decode();
                    this.readBuffer.compact();
                }
            }
        }
        finally {
            if (tmpKey != null) {
                tmpKey.cancel();
                tmpKey = null;
            }
            if (readSelector != null) {
                // Cancel the key.
                readSelector.selectNow();
                SelectorFactory.returnSelector(readSelector);
            }
        }
        return n;
    }


    /**
     * 解码并派发消息
     */
    @Override
    public void decode() {
        Object message;
        int size = this.readBuffer.remaining();
        while (this.readBuffer.hasRemaining()) {
            try {
                message = this.decoder.decode(this.readBuffer, this);
                if (message == null) {
                    break;
                }
                else {
                    if (this.statistics.isStatistics()) {
                        this.statistics.statisticsRead(size - this.readBuffer.remaining());
                        size = this.readBuffer.remaining();
                    }
                }
                this.dispatchReceivedMessage(message);
            }
            catch (final Exception e) {
                this.onException(e);
                log.error("Decode error", e);
                super.close();
                break;
            }
        }
    }


    public Socket socket() {
        return ((SocketChannel) this.selectableChannel).socket();
    }


    public ChannelInputStream getInputStream(final Object msg) throws IOException {
        if (this.decoder instanceof ByteBufferCodecFactory.ByteBufferDecoder) {
            return new ChannelInputStream(((IoBuffer) msg).buf());
        }
        else {
            throw new IOException(
                "If you want to use ChannelInputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }


    public ChannelOutputStream getOutputStream() throws IOException {
        if (this.encoder instanceof ByteBufferCodecFactory.ByteBufferEncoder) {
            return new ChannelOutputStream(this, 0, false);
        }
        else {
            throw new IOException(
                "If you want to use ChannelOutputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }


    public ChannelOutputStream getOutputStream(final int capacity, final boolean direct) throws IOException {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity<0");
        }
        if (this.encoder instanceof ByteBufferCodecFactory.ByteBufferEncoder) {
            return new ChannelOutputStream(this, capacity, direct);
        }
        else {
            throw new IOException(
                "If you want to use ChannelOutputStream,please set CodecFactory to ByteBufferCodecFactory");
        }
    }


    @Override
    protected final void closeChannel() throws IOException {
        // 优先关闭输出流
        try {
            if (this.selectableChannel instanceof SocketChannel) {
                final Socket socket = ((SocketChannel) this.selectableChannel).socket();
                try {
                    if (!socket.isClosed() && !socket.isOutputShutdown()) {
                        socket.shutdownOutput();
                    }
                    if (!socket.isClosed() && !socket.isInputShutdown()) {
                        socket.shutdownInput();
                    }
                }
                catch (final IOException e) {
                    // ignore
                }
                try {
                    socket.close();
                }
                catch (final IOException e) {
                    // ignore
                }
            }
        }
        finally {
            this.unregisterSession();
        }
    }


    @Override
    protected void onIdle0() {
        if (this.initialReadBufferSize > 0 && this.readBuffer.capacity() > this.initialReadBufferSize) {
            this.readBuffer =
                    IoBuffer.wrap(ByteBufferUtils.decreaseBufferCapatity(this.readBuffer.buf(), this.recvBufferSize,
                        this.initialReadBufferSize));

        }
    }

}