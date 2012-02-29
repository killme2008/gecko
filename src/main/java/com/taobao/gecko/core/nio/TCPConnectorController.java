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
package com.taobao.gecko.core.nio;

/**
 *Copyright [2008] [dennis zhuang]
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
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.nio.impl.NioTCPSession;
import com.taobao.gecko.core.nio.impl.SocketChannelController;


/**
 * Controller for client connecting
 * 
 * @author dennis
 * 
 */
public class TCPConnectorController extends SocketChannelController implements SingleConnector {
    protected SocketChannel socketChannel;

    protected SocketAddress remoteAddress;

    protected NioTCPSession session;

    private FutureImpl<Boolean> connectFuture;


    public TCPConnectorController() {
        super();
    }


    public TCPConnectorController(final Configuration configuration) {
        super(configuration, null, null);

    }


    public SocketAddress getRemoteSocketAddress() {
        return this.remoteAddress;
    }


    public TCPConnectorController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
    }


    public Future<Boolean> connect(final SocketAddress remoteAddr, final Object... args) throws IOException {
        if (this.started) {
            throw new IllegalStateException("SocketChannel has been connected");
        }
        if (remoteAddr == null) {
            throw new IllegalArgumentException("Null remote address");
        }
        this.connectFuture = new FutureImpl<Boolean>(args);
        this.remoteAddress = remoteAddr;
        this.start();
        return this.connectFuture;
    }


    public void awaitConnectUnInterrupt() throws IOException {
        if (this.connectFuture == null) {
            throw new IllegalStateException("The connector has not been started");
        }
        try {
            this.connectFuture.get();
        }
        catch (final ExecutionException e) {
            throw new IOException(e.getMessage());
        }
        catch (final InterruptedException e) {

        }
    }


    public Future<Boolean> send(final Object msg) {
        if (this.session == null || this.session.isClosed()) {
            throw new IllegalStateException("SocketChannel has not been connected");
        }
        return this.session.asyncWrite(msg);
    }


    public boolean isConnected() {
        return this.session != null && !this.session.isClosed();
    }


    public void disconnect() throws IOException {
        this.stop();
    }


    /**
     * 断开连接并重连
     * 
     * @throws IOException
     */
    public Future<Boolean> reconnect() throws IOException {
        if (!this.started) {
            final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
            future.setResult(false);
            return future;
        }
        this.session.close();
        this.connectFuture = new FutureImpl<Boolean>();
        this.doStart();
        return this.connectFuture;
    }


    /**
     * 断开当前连接，并重连新地址remoteAddr
     * 
     * @param remoteAddr
     * @throws IOException
     */
    public void reconnect(final SocketAddress remoteAddr) throws IOException {
        if (!this.started) {
            return;
        }
        this.remoteAddress = remoteAddr;
        this.session.close();
        this.doStart();
    }


    @Override
    protected void doStart() throws IOException {
        this.initialSelectorManager();
        this.socketChannel = SocketChannel.open();
        try {
            this.configureSocketChannel(this.socketChannel);
            if (this.localSocketAddress != null) {
                this.socketChannel.socket().bind(this.localSocketAddress);
            }
            if (!this.socketChannel.connect(this.remoteAddress)) {
                this.selectorManager.registerChannel(this.socketChannel, SelectionKey.OP_CONNECT, null);
            }
            else {
                this.createSession(this.socketChannel, this.connectFuture.getArgs());
                this.connectFuture.setResult(true);
            }
        }
        catch (final IOException e) {
            if (this.socketChannel != null) {
                this.socketChannel.close();
            }
            throw e;
        }
    }


    @Override
    public void onConnect(final SelectionKey key) throws IOException {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
        key.attach(null);
        try {
            if (!((SocketChannel) key.channel()).finishConnect()) {
                throw new IOException("Connect Fail");
            }
            this.createSession((SocketChannel) key.channel(), this.connectFuture.getArgs());
            this.connectFuture.setResult(true);
        }
        catch (final Exception e) {
            log.error("Connect error", e);
            this.cancelKey(key);
            this.connectFuture.failure(e);
        }
    }


    protected void cancelKey(final SelectionKey key) throws IOException {
        try {
            if (key.channel() != null) {
                key.channel().close();
            }
        }
        finally {
            key.cancel();
        }
    }


    protected void createSession(final SocketChannel socketChannel, final Object... args) {
        this.session = (NioTCPSession) this.buildSession(socketChannel);
        this.selectorManager.registerSession(this.session, EventType.ENABLE_READ);
        this.setLocalSocketAddress((InetSocketAddress) socketChannel.socket().getLocalSocketAddress());
        this.session.start();
        this.handler.onSessionConnected(this.session, args);

    }


    public void closeChannel(final Selector selector) throws IOException {
        if (this.session != null) {
            this.session.close();
            this.session = null;
        }
        selector.selectNow();
    }
}