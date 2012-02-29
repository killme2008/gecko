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

import java.net.InetSocketAddress;
import java.net.URI;

import com.taobao.gecko.service.config.ServerConfig;


/**
 * 
 * Remoting服务器
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 上午11:11:29
 */

public interface RemotingServer extends RemotingController {

    /**
     * 设置服务器配置，包括端口、TCP选项等
     * 
     * @param serverConfig
     */
    public void setServerConfig(ServerConfig serverConfig);


    /**
     * 返回可供连接的URI
     * 
     * @return
     */
    public URI getConnectURI();


    /**
     * 返回绑定地址
     * 
     * @return
     */
    public InetSocketAddress getInetSocketAddress();

}