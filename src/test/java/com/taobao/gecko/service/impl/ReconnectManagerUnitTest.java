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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.notify.NotifyWireFormatType;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-23 下午02:35:36
 */

public class ReconnectManagerUnitTest {
    ReconnectManager reconnectManager;
    RemotingClient remotingClient;
    ClientConfig clientConfig;


    @Before
    public void setUp() throws Exception {
        this.clientConfig = new ClientConfig();
        this.clientConfig.setSelectorPoolSize(4);
        this.clientConfig.setWireFormatType(new NotifyWireFormatType());
        this.clientConfig.setHealConnectionExecutorPoolSize(3);
        this.remotingClient = RemotingFactory.connect(this.clientConfig);
        this.remotingClient.start();
        this.reconnectManager = ((DefaultRemotingClient) this.remotingClient).getReconnectManager();

    }


    @Test
    public void testIsValidTask_有效任务() {
        final Set<String> groupSet = new HashSet<String>();
        groupSet.add(Constants.DEFAULT_GROUP);
        groupSet.add("test-group");
        final InetSocketAddress addr = new InetSocketAddress(9001);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        assertTrue(this.reconnectManager.isValidTask(task));
    }


    @Test
    public void testIsValidTask_无效任务_分组为空() {
        final Set<String> groupSet = new HashSet<String>();
        final InetSocketAddress addr = new InetSocketAddress(9001);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        assertFalse(this.reconnectManager.isValidTask(task));
    }


    @Test
    public void testIsValidTask_无效任务_任务已经完成() {
        final Set<String> groupSet = new HashSet<String>();
        final InetSocketAddress addr = new InetSocketAddress(9001);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        task.setDone(true);
        assertFalse(this.reconnectManager.isValidTask(task));
    }


    @Test
    public void testIsValidTask_无效任务_仅有默认分组() {
        final Set<String> groupSet = new HashSet<String>();
        groupSet.add(Constants.DEFAULT_GROUP);
        final InetSocketAddress addr = new InetSocketAddress(9001);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        assertFalse(this.reconnectManager.isValidTask(task));
    }


    @Test
    public void testAddReconnectTask_有效任务_服务器存在() throws Exception {
        final ServerConfig serverConfig = new ServerConfig();
        final int port = 6859;
        serverConfig.setPort(port);
        final RemotingServer server = RemotingFactory.bind(serverConfig);
        try {
            final String url = server.getConnectURI().toString();

            this.remotingClient.setAttribute(url, Constants.CONNECTION_ATTR, new Object());
            this.remotingClient.setAttribute(url, Constants.CONNECTION_COUNT_ATTR, 1);
            final Set<String> groupSet = new HashSet<String>();
            groupSet.add(Constants.DEFAULT_GROUP);
            groupSet.add(url);
            final InetSocketAddress addr = new InetSocketAddress(port);
            final ReconnectTask task = new ReconnectTask(groupSet, addr);
            assertEquals(0, this.remotingClient.getConnectionCount(url));
            this.reconnectManager.addReconnectTask(task);
            Thread.sleep(2 * this.clientConfig.getHealConnectionInterval());
            assertEquals(1, this.remotingClient.getConnectionCount(url));
        }
        finally {
            server.stop();
        }
    }


    @Test
    public void testAddReconnectTask_有效任务_服务器不存在() throws Exception {

        final int port = 6860;
        final String url = "tcp://localhost:" + 6860;

        this.remotingClient.setAttribute(url, Constants.CONNECTION_ATTR, new Object());
        this.remotingClient.setAttribute(url, Constants.CONNECTION_COUNT_ATTR, 1);
        final Set<String> groupSet = new HashSet<String>();
        groupSet.add(Constants.DEFAULT_GROUP);
        groupSet.add(url);
        final InetSocketAddress addr = new InetSocketAddress(port);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        assertEquals(0, this.remotingClient.getConnectionCount(url));
        this.reconnectManager.addReconnectTask(task);
        Thread.sleep(3 * this.clientConfig.getHealConnectionInterval());
        assertEquals(0, this.remotingClient.getConnectionCount(url));
    }


    @Test
    public void testAddReconnectTask_有效任务_超过连接数限制() throws Exception {

        final ServerConfig serverConfig = new ServerConfig();
        final int port = 6861;
        serverConfig.setPort(port);
        final RemotingServer server = RemotingFactory.bind(serverConfig);
        try {
            final String url = server.getConnectURI().toString();

            this.remotingClient.connect(url, 5);
            this.remotingClient.awaitReadyInterrupt(url);
            final Set<String> groupSet = new HashSet<String>();
            groupSet.add(Constants.DEFAULT_GROUP);
            groupSet.add(url);
            final InetSocketAddress addr = new InetSocketAddress(port);
            final ReconnectTask task = new ReconnectTask(groupSet, addr);
            assertEquals(5, this.remotingClient.getConnectionCount(url));
            this.reconnectManager.addReconnectTask(task);
            Thread.sleep(3 * this.clientConfig.getHealConnectionInterval());
            assertEquals(5, this.remotingClient.getConnectionCount(url));
            assertEquals(0, this.reconnectManager.getReconnectTaskCount());
        }
        finally {
            server.stop();
        }
    }


    @Test
    public void testAddReconnectTask_取消任务() throws Exception {

        final int port = 6862;

        final String url = "tcp://localhost:" + port;
        final Set<String> groupSet = new HashSet<String>();
        groupSet.add(Constants.DEFAULT_GROUP);
        groupSet.add(url);
        final InetSocketAddress addr = new InetSocketAddress(port);
        final ReconnectTask task = new ReconnectTask(groupSet, addr);
        assertEquals(0, this.remotingClient.getConnectionCount(url));
        this.reconnectManager.addReconnectTask(task);
        Thread.sleep(3 * this.clientConfig.getHealConnectionInterval());
        assertEquals(0, this.remotingClient.getConnectionCount(url));
        this.reconnectManager.cancelReconnectGroup(url);
        Thread.sleep(2 * this.clientConfig.getHealConnectionInterval());
        assertEquals(0, this.reconnectManager.getReconnectTaskCount());
    }


    private ServerConfig getServerConfigByPort(final int port) {
        final InetSocketAddress serverAddr1 = new InetSocketAddress("localhost", port);
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setLocalInetSocketAddress(serverAddr1);
        return serverConfig;
    }


    @After
    public void tearDown() throws Exception {
        this.remotingClient.stop();
    }

}