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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.NioSessionConfig;


/**
 * Nio tcp µœ÷
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 œ¬ŒÁ06:18:00
 */
public abstract class SocketChannelController extends NioController {

    protected boolean soLingerOn = false;


    public void setSoLinger(final boolean on, final int value) {
        this.soLingerOn = on;
        this.socketOptions.put(StandardSocketOption.SO_LINGER, value);
    }


    public SocketChannelController() {
        super();
    }


    public SocketChannelController(final Configuration configuration) {
        super(configuration, null, null);

    }


    public SocketChannelController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
    }


    public SocketChannelController(final Configuration configuration, final Handler handler,
            final CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
    }


    @Override
    protected final void dispatchReadEvent(final SelectionKey key) {
        final Session session = (Session) key.attachment();
        if (session != null) {
            ((NioSession) session).onEvent(EventType.READABLE, key.selector());
        }
        else {
            log.warn("Could not find session for readable event,maybe it is closed");
        }
    }


    @Override
    protected final void dispatchWriteEvent(final SelectionKey key) {
        final Session session = (Session) key.attachment();
        if (session != null) {
            ((NioSession) session).onEvent(EventType.WRITEABLE, key.selector());
        }
        else {
            log.warn("Could not find session for writable event,maybe it is closed");
        }

    }


    protected NioSession buildSession(final SocketChannel sc) {
        final Queue<WriteMessage> queue = this.buildQueue();
        final NioSessionConfig sessionConfig = this.buildSessionConfig(sc, queue);
        final NioSession session = new NioTCPSession(sessionConfig, this.configuration.getSessionReadBufferSize());
        return session;
    }


    protected final void configureSocketChannel(final SocketChannel sc) throws IOException {
        sc.socket().setSoTimeout(this.soTimeout);
        sc.configureBlocking(false);
        if (this.socketOptions.get(StandardSocketOption.SO_REUSEADDR) != null) {
            sc.socket().setReuseAddress(
                StandardSocketOption.SO_REUSEADDR.type()
                    .cast(this.socketOptions.get(StandardSocketOption.SO_REUSEADDR)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_SNDBUF) != null) {
            sc.socket().setSendBufferSize(
                StandardSocketOption.SO_SNDBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_SNDBUF)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_KEEPALIVE) != null) {
            sc.socket().setKeepAlive(
                StandardSocketOption.SO_KEEPALIVE.type()
                    .cast(this.socketOptions.get(StandardSocketOption.SO_KEEPALIVE)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_LINGER) != null) {
            sc.socket().setSoLinger(this.soLingerOn,
                StandardSocketOption.SO_LINGER.type().cast(this.socketOptions.get(StandardSocketOption.SO_LINGER)));
        }
        if (this.socketOptions.get(StandardSocketOption.SO_RCVBUF) != null) {
            sc.socket().setReceiveBufferSize(
                StandardSocketOption.SO_RCVBUF.type().cast(this.socketOptions.get(StandardSocketOption.SO_RCVBUF)));

        }
        if (this.socketOptions.get(StandardSocketOption.TCP_NODELAY) != null) {
            sc.socket().setTcpNoDelay(
                StandardSocketOption.TCP_NODELAY.type().cast(this.socketOptions.get(StandardSocketOption.TCP_NODELAY)));
        }
    }

}