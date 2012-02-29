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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Future;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.UDPSession;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.ByteBufferCodecFactory;
import com.taobao.gecko.core.core.impl.ByteBufferWriteMessage;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.core.impl.UDPHandlerAdapter;
import com.taobao.gecko.core.nio.NioSessionConfig;
import com.taobao.gecko.core.statistics.impl.DefaultStatistics;


/**
 * Nio UDP连接
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:10:25
 */
public class NioUDPSession extends AbstractNioSession implements UDPSession {

    public NioUDPSession(final NioSessionConfig sessionConfig, final int maxDatagramPacketLength) {
        super(sessionConfig);
        this.setReadBuffer(IoBuffer.allocate(maxDatagramPacketLength));
        this.onCreated();
    }


    @Override
    protected Object writeToChannel0(final WriteMessage msg) throws IOException {
        // Check if it is canceled
        if (msg.getWriteFuture() != null && !msg.isWriting() && msg.getWriteFuture().isCancelled()) {
            return msg.getMessage();
        }
        final UDPWriteMessage message = (UDPWriteMessage) msg;
        final IoBuffer gatherBuffer = message.getWriteBuffer();
        final int length = gatherBuffer.remaining();
        // begin writing
        msg.writing();
        while (gatherBuffer.hasRemaining()) {
            ((DatagramChannel) this.selectableChannel).send(gatherBuffer.buf(), message.getTargetAddress());
        }
        this.statistics.statisticsWrite(length);
        this.scheduleWritenBytes.addAndGet(0 - length);
        if (message.getWriteFuture() != null) {
            message.getWriteFuture().setResult(Boolean.TRUE);
        }
        return message.getMessage();
    }


    @Override
    public Future<Boolean> asyncWrite(final Object packaet) {
        if (packaet instanceof DatagramPacket) {
            if (this.isClosed()) {
                final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
                future.failure(new IOException("连接已经被关闭"));
                return future;
            }
            final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
            final WriteMessage message = this.wrapMessage(packaet, future);
            this.scheduleWritenBytes.addAndGet(message.remaining());
            this.writeFromUserCode(message);
            return future;
        }
        else {
            throw new IllegalArgumentException("UDP session must write DatagramPacket");
        }

    }


    @Override
    protected void closeChannel() throws IOException {
        try {
            ((DatagramChannel) this.selectableChannel).socket().close();
        }
        finally {
            this.unregisterSession();
        }
    }


    @Override
    public void write(final Object packet) {
        if (packet instanceof DatagramPacket) {
            if (this.isClosed()) {
                return;
            }
            final WriteMessage message = this.wrapMessage(packet, null);
            this.scheduleWritenBytes.addAndGet(message.remaining());
            this.writeFromUserCode(message);
        }
        else {
            throw new IllegalArgumentException("UDP session must write DatagramPacket");
        }
    }


    @Override
    protected WriteMessage wrapMessage(final Object obj, final Future<Boolean> writeFuture) {
        final DatagramPacket packet = (DatagramPacket) obj;
        final WriteMessage message =
                new UDPWriteMessage(packet.getSocketAddress(), packet.getData(), (FutureImpl<Boolean>) writeFuture);
        return message;
    }


    public Future<Boolean> asyncWrite(final SocketAddress targetAddr, final Object msg) {
        if (this.isClosed()) {
            final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
            future.failure(new IOException("连接已经被关闭"));
            return future;
        }
        final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        final WriteMessage message = new UDPWriteMessage(targetAddr, msg, future);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
        return future;
    }


    public void write(final SocketAddress targetAddr, final Object packet) {
        if (this.isClosed()) {
            return;
        }
        final WriteMessage message = new UDPWriteMessage(targetAddr, packet, null);
        this.scheduleWritenBytes.addAndGet(message.remaining());
        this.writeFromUserCode(message);
    }


    @Override
    protected synchronized void readFromBuffer() {
        if (this.closed) {
            return;
        }
        this.readBuffer.clear();
        try {
            this.decode();
            this.selectorManager.registerSession(this, EventType.ENABLE_READ);
        }
        catch (final Throwable e) {
            log.error("Read from buffer error", e);
            this.onException(e);
            this.close0();
        }
    }


    @Override
    public void decode() {
        try {
            final SocketAddress address = ((DatagramChannel) this.selectableChannel).receive(this.readBuffer.buf());
            this.readBuffer.flip();
            this.statistics.statisticsRead(this.readBuffer.remaining());
            if (address != null) {
                if (!(this.decoder instanceof ByteBufferCodecFactory.ByteBufferDecoder)) {
                    final Object msg = this.decoder.decode(this.readBuffer, this);
                    if (msg != null) {
                        this.dispatchReceivedMessage(address, msg);
                    }
                }
                else {
                    final byte[] bytes = new byte[this.readBuffer.remaining()];
                    this.readBuffer.get(bytes);
                    final DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length, address);
                    this.dispatchReceivedMessage(datagramPacket);
                }
            }
        }
        catch (final ClosedChannelException e) {
            this.close0();
            // ignore
            log.error("Decode error", e);
        }
        catch (final Throwable e) {
            this.close0();
            log.error("Decode error", e);
            this.onException(e);
        }
    }


    protected void dispatchReceivedMessage(final SocketAddress address, final Object message) {
        long start = -1;
        if (!(this.statistics instanceof DefaultStatistics)) {
            start = System.currentTimeMillis();
        }
        if (this.handler instanceof UDPHandlerAdapter) {
            ((UDPHandlerAdapter) this.handler).onMessageReceived(this, address, message);
        }
        else {
            this.handler.onMessageReceived(this, message);
        }
        if (start != -1) {
            this.statistics.statisticsProcess(System.currentTimeMillis() - start);
        }
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.TCPHandler#getRemoteSocketAddress()
     */
    public InetSocketAddress getRemoteSocketAddress() {
        throw new UnsupportedOperationException();
    }

    /**
     * UDP消息封装，增加了一个发送地址
     * 
     * 
     * 
     * @author boyan
     * 
     * @since 1.0, 2009-12-16 下午06:10:42
     */
    class UDPWriteMessage extends ByteBufferWriteMessage {

        private final SocketAddress targetAddress;


        private UDPWriteMessage(final SocketAddress targetAddress, final Object message,
                final FutureImpl<Boolean> writeFuture) {
            super(message, writeFuture);
            this.targetAddress = targetAddress;
            if (message instanceof byte[]) {
                this.buffer = IoBuffer.wrap((byte[]) message);
            }
            else {
                this.buffer = NioUDPSession.this.encoder.encode(message, NioUDPSession.this);
            }
        }


        public SocketAddress getTargetAddress() {
            return this.targetAddress;
        }
    }


    @Override
    public boolean isUseBlockingWrite() {
        return false;
    }


    @Override
    public void setUseBlockingWrite(final boolean useBlockingWrite) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isUseBlockingRead() {
        return false;
    }


    @Override
    public void setUseBlockingRead(final boolean useBlockingRead) {
        throw new UnsupportedOperationException();
    }


    public DatagramSocket socket() {
        return ((DatagramChannel) this.selectableChannel).socket();
    }
}