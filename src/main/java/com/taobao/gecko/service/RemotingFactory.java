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
package com.taobao.gecko.service;

import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.impl.DefaultRemotingClient;
import com.taobao.gecko.service.impl.DefaultRemotingServer;


/**
 * 
 * Remoting工厂，创建通讯层组件
 * 
 * @author boyan
 * 
 * @since 1.0, 2010-1-27 下午04:00:54
 */

public final class RemotingFactory {

    /**
     * 初始化并启动服务器，绑定到指定IP地址
     * 
     * @param serverConfig
     * @return
     * @throws NotifyRemotingException
     */
    public static RemotingServer bind(final ServerConfig serverConfig) throws NotifyRemotingException {
        final RemotingServer server = newRemotingServer(serverConfig);
        server.start();
        return server;
    }


    /**
     * 创建一个服务器对象，不启动
     * 
     * @param serverConfig
     * @return
     */
    public static RemotingServer newRemotingServer(final ServerConfig serverConfig) {
        return new DefaultRemotingServer(serverConfig);
    }


    /**
     * 创建一个客户端对象，不启动
     * 
     * @param clientConfig
     * @return
     */
    public static RemotingClient newRemotingClient(final ClientConfig clientConfig) {
        return new DefaultRemotingClient(clientConfig);
    }


    /**
     * 创建一个客户端对象并启动
     * 
     * @param clientConfig
     * @return
     * @throws NotifyRemotingException
     */
    public static RemotingClient connect(final ClientConfig clientConfig) throws NotifyRemotingException {
        final DefaultRemotingClient remotingClient = new DefaultRemotingClient(clientConfig);
        remotingClient.start();
        return remotingClient;
    }
}