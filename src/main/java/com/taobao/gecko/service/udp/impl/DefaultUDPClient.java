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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.nio.UDPConnectorController;
import com.taobao.gecko.service.RemotingController;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.udp.UDPClient;
import com.taobao.gecko.service.udp.UDPServiceHandler;


/**
 * UDP客户端实现
 * 
 * @author boyan
 * @Date 2010-8-26
 * 
 */
public class DefaultUDPClient extends DefaultUDPServer implements UDPClient {

    public DefaultUDPClient(final UDPServiceHandler handler) throws NotifyRemotingException {
        super(handler, 0);

    }


    public DefaultUDPClient(final RemotingController remotingController, final UDPServiceHandler handler)
            throws NotifyRemotingException {
        super(remotingController, handler, 0);
    }


    public void send(final InetSocketAddress inetSocketAddress, final ByteBuffer buff) throws NotifyRemotingException {
        try {
            ((UDPConnectorController) this.udpController).send(inetSocketAddress, IoBuffer.wrap(buff));
        }
        catch (final Throwable t) {
            throw new NotifyRemotingException(t);
        }
    }


    @Override
    protected void initController() {
        this.udpController = new UDPConnectorController();
    }

}