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
 *Copyright [2009-2010] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package com.taobao.gecko.core.nio.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.util.SystemUtils;


/**
 * Nio的UDP实现
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:08:54
 */
public abstract class DatagramChannelController extends NioController {

    protected DatagramChannel channel;
    protected NioSession udpSession;
    protected int maxDatagramPacketLength;


    public DatagramChannelController() {
        super();
        this.maxDatagramPacketLength = 4096;
    }


    @Override
    protected void doStart() throws IOException {
        this.buildDatagramChannel();
        this.initialSelectorManager();
        this.buildUDPSession();
    }


    public DatagramChannelController(final Configuration configuration) {
        super(configuration, null, null);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public DatagramChannelController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public DatagramChannelController(final Configuration configuration, final Handler handler,
            final CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public int getMaxDatagramPacketLength() {
        return this.maxDatagramPacketLength;
    }


    @Override
    public void setReadThreadCount(final int readThreadCount) {
        if (readThreadCount > 1) {
            throw new IllegalArgumentException("UDP controller could not have more than 1 read thread");
        }
        super.setReadThreadCount(readThreadCount);
    }


    public void setMaxDatagramPacketLength(final int maxDatagramPacketLength) {
        if (this.isStarted()) {
            throw new IllegalStateException();
        }
        if (SystemUtils.isLinuxPlatform() && maxDatagramPacketLength > 9216) {
            throw new IllegalArgumentException(
                "The maxDatagramPacketLength could not be larger than 9216 bytes on linux");
        }
        else if (maxDatagramPacketLength > 65507) {
            throw new IllegalArgumentException("The maxDatagramPacketLength could not be larger than 65507 bytes");
        }
        this.maxDatagramPacketLength = maxDatagramPacketLength;
    }


    public void closeChannel(final Selector selector) throws IOException {
        this.closeChannel0();
        selector.selectNow();
    }


    private void closeChannel0() throws IOException {
        if (this.udpSession != null && !this.udpSession.isClosed()) {
            this.udpSession.close();
            this.udpSession = null;
        }
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
            this.channel = null;
        }
    }


    @Override
    protected void stop0() throws IOException {
        this.closeChannel0();
        super.stop0();
    }


    protected void buildUDPSession() {
        final Queue<WriteMessage> queue = this.buildQueue();
        this.udpSession = new NioUDPSession(this.buildSessionConfig(this.channel, queue), this.maxDatagramPacketLength);
        this.selectorManager.registerSession(this.udpSession, EventType.ENABLE_READ);
        this.udpSession.start();
    }


    protected void buildDatagramChannel() throws IOException, SocketException, ClosedChannelException {
        this.channel = DatagramChannel.open();
        this.channel.socket().setSoTimeout(this.soTimeout);

        if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
            this.channel.socket().setReuseAddress(
                StandardSocketOption.SO_REUSEADDR.type()
                    .cast(this.socketOptions.get(StandardSocketOption.SO_REUSEADDR)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
            this.channel.socket().setReceiveBufferSize(
                StandardSocketOption.SO_RCVBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_RCVBUF)));

        }
        if (this.socketOptions.get(StandardSocketOption.SO_SNDBUF) != null) {
            this.channel.socket().setSendBufferSize(
                StandardSocketOption.SO_SNDBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_SNDBUF)));
        }
        this.channel.configureBlocking(false);
        if (this.localSocketAddress != null) {
            this.channel.socket().bind(this.localSocketAddress);
        }
        else {
            this.channel.socket().bind(new InetSocketAddress("localhost", 0));
        }
        this.setLocalSocketAddress((InetSocketAddress) this.channel.socket().getLocalSocketAddress());

    }


    @Override
    protected void dispatchReadEvent(final SelectionKey key) {
        if (this.udpSession != null) {
            this.udpSession.onEvent(EventType.READABLE, key.selector());
        }
        else {
            log.warn("NO session to dispatch read event");
        }

    }


    @Override
    protected void dispatchWriteEvent(final SelectionKey key) {
        if (this.udpSession != null) {
            this.udpSession.onEvent(EventType.WRITEABLE, key.selector());
        }
        else {
            log.warn("NO session to dispatch write event");
        }

    }

}