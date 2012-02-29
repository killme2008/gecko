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
package com.taobao.gecko.service.config;

import java.net.InetSocketAddress;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 上午11:12:23
 */

public final class ServerConfig extends BaseConfig {
    /**
     * 端口
     */
    private int port = 9527;
    /**
     * backlog队列大小
     */
    private int backlog = 1000;

    private InetSocketAddress localInetSocketAddress;


    public InetSocketAddress getLocalInetSocketAddress() {
        return this.localInetSocketAddress;
    }


    public void setLocalInetSocketAddress(InetSocketAddress localInetSocketAddress) {
        this.localInetSocketAddress = localInetSocketAddress;
    }


    public ServerConfig() {
        super();
        this.setIdleTime(-1);
    }


    public int getBacklog() {
        return this.backlog;
    }


    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }


    public int getPort() {
        return this.port;
    }


    public void setPort(int port) {
        this.port = port;
    }

}