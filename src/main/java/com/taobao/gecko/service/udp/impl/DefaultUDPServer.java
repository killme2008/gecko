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
package com.taobao.gecko.service.udp.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.UDPController;
import com.taobao.gecko.core.nio.impl.DatagramChannelController;
import com.taobao.gecko.core.nio.impl.NioController;
import com.taobao.gecko.service.RemotingController;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.impl.BaseRemotingController;
import com.taobao.gecko.service.udp.UDPServer;
import com.taobao.gecko.service.udp.UDPServiceHandler;


/**
 * UDP服务端实现
 * 
 * @author boyan
 * @Date 2010-8-26
 * 
 */
public class DefaultUDPServer implements UDPServer {
    protected DatagramChannelController udpController;
    protected final UDPServiceHandler handler;


    public DefaultUDPServer(final UDPServiceHandler handler, final int port) throws NotifyRemotingException {
        this(null, handler, port);
    }


    public DefaultUDPServer(final RemotingController remotingController, final UDPServiceHandler handler, final int port)
            throws NotifyRemotingException {
        this.initController();
        this.handler = handler;
        this.udpController.setHandler(new HandlerAdapter() {

            @Override
            public void onMessageReceived(final Session session, final Object message) {
                DefaultUDPServer.this.handler.onMessageReceived((DatagramPacket) message);
            }

        });
        // 复用SelectorManager
        if (remotingController != null && remotingController.isStarted()) {
            final NioController nioController = ((BaseRemotingController) remotingController).getController();
            this.udpController.setSelectorManager(nioController.getSelectorManager());
        }
        try {
            this.udpController.bind(new InetSocketAddress(port));
        }
        catch (final IOException e) {
            throw new NotifyRemotingException("启动udp服务器失败，端口为" + port, e);
        }

    }


    public boolean isStarted() {
        return this.udpController.isStarted();
    }


    protected void initController() {
        this.udpController = new UDPController();
        this.udpController.setSocketOption(StandardSocketOption.SO_REUSEADDR, true);
    }


    public UDPServiceHandler getUDPServiceHandler() {
        return this.handler;
    }


    public void start() throws NotifyRemotingException {
        try {
            this.udpController.start();
        }
        catch (final IOException e) {
            throw new NotifyRemotingException(e);
        }

    }


    public void stop() throws NotifyRemotingException {
        try {
            this.udpController.stop();
        }
        catch (final IOException e) {
            throw new NotifyRemotingException(e);
        }

    }

}