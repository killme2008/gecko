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
package com.taobao.gecko.example.rpc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.transport.RpcRequestProcessor;
import com.taobao.gecko.example.rpc.transport.RpcWireFormatType;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;


public class RpcServer {
    private InetSocketAddress serverAddr;

    private RemotingServer remotingServer;


    public void bind(BeanLocator beanLocator, InetSocketAddress serverAddr) throws IOException {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new RpcWireFormatType());
        this.serverAddr = serverAddr;
        serverConfig.setLocalInetSocketAddress(serverAddr);
        this.remotingServer = RemotingFactory.newRemotingServer(serverConfig);
        this.remotingServer.registerProcessor(RpcRequest.class, new RpcRequestProcessor((ThreadPoolExecutor) Executors
            .newCachedThreadPool(), beanLocator));
        try {
            this.remotingServer.start();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }
}