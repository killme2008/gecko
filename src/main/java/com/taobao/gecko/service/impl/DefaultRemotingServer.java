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
package com.taobao.gecko.service.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.nio.TCPController;
import com.taobao.gecko.core.nio.impl.SocketChannelController;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.processor.HeartBeatCommandProecssor;


/**
 * 
 * RemotingServer，服务器的默认实现
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 上午11:13:24
 */

public class DefaultRemotingServer extends BaseRemotingController implements RemotingServer {

    public DefaultRemotingServer(final ServerConfig serverConfig) {
        super(serverConfig);
        this.config = serverConfig;

    }


    public void setServerConfig(final ServerConfig serverConfig) {
        if (this.controller != null && this.controller.isStarted()) {
            throw new IllegalStateException("RemotingServer已经启动，设置无效");
        }
        this.config = serverConfig;
    }


    /**
     * 服务端还需要扫描连接是否存活
     */
    @Override
    protected ScanTask[] getScanTasks() {
        return new ScanTask[] { new InvalidCallBackScanTask(), new InvalidConnectionScanTask() };
    }


    @Override
    protected void doStart() throws NotifyRemotingException {
        // 如果没有设置心跳处理器，则使用默认
        if (!this.remotingContext.processorMap.containsKey(HeartBeatRequestCommand.class)) {
            this.registerProcessor(HeartBeatRequestCommand.class, new HeartBeatCommandProecssor());
        }
        try {

            final ServerConfig serverConfig = (ServerConfig) this.config;
            ((TCPController) this.controller).setBacklog(serverConfig.getBacklog());
            // 优先绑定指定IP地址
            if (serverConfig.getLocalInetSocketAddress() != null) {
                this.controller.bind(serverConfig.getLocalInetSocketAddress());
            }
            else {
                this.controller.bind(serverConfig.getPort());
            }
        }
        catch (final IOException e) {
            throw new NotifyRemotingException(e);
        }
    }


    @Override
    protected void doStop() throws NotifyRemotingException {
        // 关闭所有连接
        final List<Connection> connections = this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP);
        if (connections != null) {
            for (final Connection conn : connections) {
                conn.close(false);
            }
        }
    }


    public synchronized URI getConnectURI() {
        final InetSocketAddress socketAddress = this.getInetSocketAddress();
        if (socketAddress == null) {
            throw new IllegalStateException("server未启动");
        }
        InetAddress inetAddress = null;
        try {
            inetAddress = RemotingUtils.getLocalHostAddress();
        }
        catch (final Exception e) {
            throw new IllegalStateException("获取IP地址失败", e);
        }
        try {
            if (inetAddress instanceof Inet4Address) {
                return new URI(this.config.getWireFormatType().getScheme() + "://" + inetAddress.getHostAddress() + ":"
                        + socketAddress.getPort());
            }
            else if (inetAddress instanceof Inet6Address) {
                return new URI(this.config.getWireFormatType().getScheme() + "://[" + inetAddress.getHostAddress()
                        + "]:" + socketAddress.getPort());
            }
            else {
                throw new IllegalStateException("Unknow InetAddress type " + inetAddress);
            }
        }
        catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }


    public InetSocketAddress getInetSocketAddress() {
        return this.controller == null ? null : this.controller.getLocalSocketAddress();
    }


    @Override
    protected SocketChannelController initController(final Configuration conf) {
        return new TCPController(conf);
    }

}