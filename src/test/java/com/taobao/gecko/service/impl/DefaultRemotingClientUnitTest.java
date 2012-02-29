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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.util.OpaqueGenerator;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.GroupAllConnectionCallBackListener;
import com.taobao.gecko.service.MultiGroupCallBackListener;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.config.WireFormatType;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.notify.NotifyWireFormatType;
import com.taobao.gecko.service.notify.OpCode;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;
import com.taobao.gecko.service.notify.response.NotifyResponseCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-22 下午01:15:04
 */

public class DefaultRemotingClientUnitTest {
    private RemotingClient remotingClient;
    static final int PORT = 8090;
    DummyRequestProcessor processor;
    String group;


    @Before
    public void setUp() throws Exception {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new NotifyWireFormatType());
        this.remotingClient = RemotingFactory.connect(clientConfig);
        this.processor = new DummyRequestProcessor();
        this.group = WireFormatType.valueOf("NOTIFY_V1").getScheme() + "://" + "127.0.0.1:" + PORT;

    }


    @Test
    public void connectIPV6() throws Exception {
        this.remotingClient.connect("tcp://[0:0:0:0:0:0:0:0]:8080");
        Assert.assertTrue(true);
    }


    @Test(timeout = 10000)
    public void testConnectAndSendTOGroup() throws Exception {
        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        serverConfig.setPort(PORT);
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {

            // 测试单个连接

            this.remotingClient.connect(this.group);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertEquals(1, this.remotingClient.getConnectionCount(this.group));
            Thread.sleep(1000);
            Assert.assertEquals(1, server.getConnectionCount(Constants.DEFAULT_GROUP));
            this.remotingClient.sendToGroup(this.group, this.createDummyRequest());
            Thread.sleep(1000);
            Assert.assertEquals(1, this.processor.recvCount.get());

            // 测试多个连接
            this.remotingClient.close(this.group, false);
            Thread.sleep(1000);
            this.remotingClient.connect(this.group, 10);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertEquals(10, this.remotingClient.getConnectionCount(this.group));
            this.remotingClient.sendToGroup(this.group, this.createDummyRequest());
            Thread.sleep(1000);
            Assert.assertEquals(2, this.processor.recvCount.get());

            // 测试监听器异步发送
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingClient.sendToGroup(this.group, this.createDummyRequest(), new SingleRequestCallBackListener() {

                public void onException(final Exception e) {

                }


                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final ResponseCommand responseCommand, final Connection conn) {

                    Assert.assertEquals(ResponseStatus.NO_ERROR, responseCommand.getResponseStatus());
                    synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                        invoked.set(true);
                        System.out.println("收到应答" + responseCommand);
                        DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                    }
                }

            }, 2000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            System.out.println("done");
            this.assertCallBackClear();
            Assert.assertEquals(3, this.processor.recvCount.get());
        }
        finally {

            server.stop();
            System.out.println("stop controller");
        }
    }


    private void assertCallBackClear() {
        final List<Connection> connetionList =
                this.remotingClient.getRemotingContext().getConnectionsByGroup(Constants.DEFAULT_GROUP);
        if (connetionList != null) {
            for (final Connection conn : connetionList) {
                Assert.assertEquals(0, ((DefaultConnection) conn).getRequstCallBackCount());
            }
        }
    }


    @Test(timeout = 10000)
    public void testSendToGroupTimeout() throws Exception {
        this.processor.sleepTime = 5000;
        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        serverConfig.setPort(PORT);
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);

        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {
            // 测试监听器异步发送并超时
            this.remotingClient.connect(this.group);
            this.remotingClient.awaitReadyInterrupt(this.group);
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingClient.sendToGroup(this.group, this.createDummyRequest(), new SingleRequestCallBackListener() {

                public void onException(final Exception e) {

                }


                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                    Assert.assertEquals(ResponseStatus.TIMEOUT, responseCommand.getResponseStatus());
                    synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                        invoked.set(true);
                        DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                    }
                }

            }, 2000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(1, this.processor.recvCount.get());
        }
        finally {
            server.stop();
        }
    }


    @Test(timeout = 20000)
    public void testSendToGroupAllConnections() throws Exception {
        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        serverConfig.setPort(PORT);
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);

        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {

            // 测试监听器异步发送并超时
            this.remotingClient.connect(this.group, 10);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertEquals(10, this.remotingClient.getConnectionCount(this.group));
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingClient.sendToGroupAllConnections(this.group, this.createDummyRequest(),
                new GroupAllConnectionCallBackListener() {

                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final Map<Connection, ResponseCommand> resultMap) {
                        Assert.assertEquals(10, resultMap.size());
                        for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                            Assert.assertEquals(ResponseStatus.NO_ERROR, entry.getValue().getResponseStatus());
                        }
                        synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                            invoked.set(true);
                            DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                        }

                    }

                }, 5000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(10, this.processor.recvCount.get());

            // 测试超时
            this.processor.sleepTime = 10000;
            invoked.set(false);
            this.remotingClient.sendToGroupAllConnections(this.group, this.createDummyRequest(),
                new GroupAllConnectionCallBackListener() {

                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final Map<Connection, ResponseCommand> resultMap) {
                        Assert.assertEquals(10, resultMap.size());
                        for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                            Assert.assertEquals(ResponseStatus.TIMEOUT, entry.getValue().getResponseStatus());
                        }
                        synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                            invoked.set(true);
                            DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                        }

                    }

                }, 5000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            this.assertCallBackClear();
        }
        finally {
            server.stop();
        }
    }


    @Test(timeout = 10000)
    public void testInvokeToGroupAllConnections() throws Exception {
        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);

        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {

            this.remotingClient.connect(this.group, 10);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertEquals(10, this.remotingClient.getConnectionCount(this.group));
            Map<Connection, ResponseCommand> resultMap =
                    this.remotingClient.invokeToGroupAllConnections(this.group, this.createDummyRequest(), 5000,
                        TimeUnit.MILLISECONDS);
            Assert.assertEquals(10, resultMap.size());

            for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                Assert.assertEquals(ResponseStatus.NO_ERROR, entry.getValue().getResponseStatus());
            }
            this.assertCallBackClear();
            Assert.assertEquals(10, this.processor.recvCount.get());

            // 测试超时
            this.processor.sleepTime = 10000;
            resultMap =
                    this.remotingClient.invokeToGroupAllConnections(this.group, this.createDummyRequest(), 5000,
                        TimeUnit.MILLISECONDS);
            Assert.assertEquals(10, resultMap.size());
            for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                Assert.assertEquals(ResponseStatus.TIMEOUT, entry.getValue().getResponseStatus());
            }
        }
        finally {
            server.stop();
        }
    }


    @Test(timeout = 15000)
    public void testSendToGroups() throws Exception {
        // 两个服务器，两个分组
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server1 = RemotingFactory.newRemotingServer(serverConfig);

        server1.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server1.start();

        serverConfig.setPort(PORT + 1);
        final RemotingServer server2 = RemotingFactory.newRemotingServer(serverConfig);

        server2.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server2.start();

        try {
            final String group2 =
                    RemotingUtils.formatServerUrl(WireFormatType.valueOf("NOTIFY_V1"), "localhost", PORT + 1);

            this.remotingClient.connect(this.group);
            this.remotingClient.connect(group2, 5);

            this.remotingClient.awaitReadyInterrupt(group2);
            this.remotingClient.awaitReadyInterrupt(this.group);

            this.remotingClient.sendToGroups(this.createGroupObjects(this.group, group2));
            Thread.sleep(5000);

            Assert.assertEquals(2, this.processor.recvCount.get());

            // 测试超时
            this.processor.sleepTime = 10000;
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingClient.sendToGroups(this.createGroupObjects(this.group, group2),
                new MultiGroupCallBackListener() {

                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                        Assert.assertEquals(2, groupResponses.size());
                        for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                            System.out.println(entry.getValue().getResponseStatus());
                            Assert.assertEquals(ResponseStatus.TIMEOUT, entry.getValue().getResponseStatus());
                        }
                        synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                            invoked.set(true);
                            DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                        }

                    }

                }, 3000, TimeUnit.MILLISECONDS);
            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            this.assertCallBackClear();
        }
        finally {
            server1.stop();
            server2.stop();
        }

    }


    @Test(timeout = Integer.MAX_VALUE)
    public void testConnectClose() throws Exception {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        server.start();

        try {
            Assert.assertFalse(this.remotingClient.isConnected(this.group));
            this.remotingClient.connect(this.group);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertTrue(this.remotingClient.isConnected(this.group));
            Assert.assertEquals(
                this.group,
                RemotingUtils.formatServerUrl(serverConfig.getWireFormatType(),
                    this.remotingClient.getRemoteAddress(this.group)));
            Assert.assertEquals(1, this.remotingClient.getConnectionCount(this.group));

            // 测试是否会自动重连
            this.remotingClient.close(this.group, true);
            Thread.sleep(1000);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertTrue(this.remotingClient.isConnected(this.group));
            Assert.assertEquals(
                this.group,
                RemotingUtils.formatServerUrl(serverConfig.getWireFormatType(),
                    this.remotingClient.getRemoteAddress(this.group)));
            Assert.assertEquals(1, this.remotingClient.getConnectionCount(this.group));

            // 彻底关闭，确定不会重连
            this.remotingClient.close(this.group, false);
            Thread.sleep(2000);
            try {
                this.remotingClient.awaitReadyInterrupt(this.group);
            }
            catch (final IllegalStateException e) {
                Assert.assertEquals("非法状态，你还没有调用connect方法进行连接操作。", e.getMessage());
            }
            Assert.assertFalse(this.remotingClient.isConnected(this.group));
            Assert.assertNull(this.remotingClient.getRemoteAddressString(this.group));

            // 建立多个连接
            this.remotingClient.connect(this.group, 100);
            this.remotingClient.awaitReadyInterrupt(this.group);
            Assert.assertEquals(100, this.remotingClient.getConnectionCount(this.group));
            Assert.assertTrue(this.remotingClient.isConnected(this.group));
            Assert.assertEquals(
                this.group,
                RemotingUtils.formatServerUrl(serverConfig.getWireFormatType(),
                    this.remotingClient.getRemoteAddress(this.group)));

            this.remotingClient.stop();
            Assert.assertFalse(this.remotingClient.isConnected(this.group));
        }
        finally {
            server.stop();
        }
        // 连接非法分组名
        try {
            this.remotingClient.connect(Constants.DEFAULT_GROUP);
            Assert.fail();
        }
        catch (final NotifyRemotingException e) {
            Assert.assertEquals("非法的Group格式，没有以tcp开头", e.getMessage());
        }
    }


    @Test(timeout = 10000)
    public void testSendErrorCommand() throws Exception {
        final RequestCommand errorCommand = new NotifyRequestCommand() {
            {
                this.opCode = OpCode.DUMMY;
                this.opaque = OpaqueGenerator.getNextOpaque();
            }


            public void encodeContent() {
                throw new RuntimeException();

            }


            public void decodeContent() {
                // TODO Auto-generated method stub

            }
        };

        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {

            // 发送编码错误命令，直接返回错误应答
            this.remotingClient.connect(this.group);
            this.remotingClient.awaitReadyInterrupt(this.group);
            try {
                this.remotingClient.invokeToGroup(this.group, errorCommand);
                Assert.fail();
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("同步调用失败", e.getMessage());
            }
            this.assertCallBackClear();
            // 发送正常命令
            final ResponseCommand response = this.remotingClient.invokeToGroup(this.group, this.createDummyRequest());
            Assert.assertNotNull(response);
            this.assertCallBackClear();
            Assert.assertEquals(ResponseStatus.NO_ERROR, response.getResponseStatus());
        }
        finally {
            server.stop();
        }
    }

    private static final class DisconnectRequestProcessor implements RequestProcessor<NotifyDummyRequestCommand> {

        AtomicInteger recvCount = new AtomicInteger(0);


        public ThreadPoolExecutor getExecutor() {
            return null;
        }


        public void handleRequest(final NotifyDummyRequestCommand request, final Connection conn) {
            try {
                final int i = this.recvCount.incrementAndGet();
                if (i % 2 == 0) {
                    System.out.println("关闭连接");
                    conn.close(false);
                }
                else {
                    conn.response(new NotifyDummyAckCommand(request, null));
                }
            }
            catch (final NotifyRemotingException e) {
                e.printStackTrace();
            }
        }
    }


    @Test(timeout = 40000)
    public void testReconnect() throws Exception {

        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        final DisconnectRequestProcessor processor2 = new DisconnectRequestProcessor();
        server.registerProcessor(NotifyDummyRequestCommand.class, processor2);
        server.start();
        try {
            this.remotingClient.connect(this.group, 10);
            this.remotingClient.awaitReadyInterrupt(this.group);
            List<Connection> connList = this.remotingClient.getRemotingContext().getConnectionsByGroup(this.group);

            for (final Connection conn : connList) {
                final Set<String> set = conn.getGroupSet();
                Assert.assertEquals(2, set.size());
                Assert.assertTrue(set.contains(this.group));
            }

            // 半数连接将断开
            this.remotingClient.sendToGroupAllConnections(this.group, this.createDummyRequest());
            Thread.sleep(20000);
            // 查看是否正常恢复
            Assert.assertEquals(10, this.remotingClient.getConnectionCount(this.group));
            connList = this.remotingClient.getRemotingContext().getConnectionsByGroup(this.group);
            for (final Connection conn : connList) {
                final Set<String> set = conn.getGroupSet();
                Assert.assertEquals(2, set.size());
                Assert.assertTrue(set.contains(this.group));
            }

        }
        finally {
            server.stop();
        }
    }


    private NotifyDummyRequestCommand createDummyRequest() {
        return new NotifyDummyRequestCommand((String) null);
    }


    @Test(timeout = 10000)
    public void testNoConnection() throws Exception {
        // 没有连接的情况，同步调用
        final ResponseCommand response = this.remotingClient.invokeToGroup(this.group, this.createDummyRequest());
        Assert.assertNotNull(response);
        Assert.assertEquals(OpCode.DUMMY, ((NotifyResponseCommand) response).getOpCode());
        Assert.assertEquals(ResponseStatus.ERROR_COMM, response.getResponseStatus());
        Assert.assertNull(response.getResponseHost());

        // 异步单向发送
        try {
            this.remotingClient.sendToGroup(this.group, this.createDummyRequest());
            Assert.fail();
        }
        catch (final NotifyRemotingException e) {
            Assert.assertEquals("分组" + this.group + "没有可用的连接", e.getMessage());

        }

        // 异步监听器
        final AtomicBoolean invoked = new AtomicBoolean(false);
        this.remotingClient.sendToGroup(this.group, this.createDummyRequest(), new SingleRequestCallBackListener() {

            public void onException(final Exception e) {

            }


            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                Assert.assertNotNull(responseCommand);
                Assert.assertEquals(OpCode.DUMMY, ((NotifyResponseCommand) responseCommand).getOpCode());
                Assert.assertEquals(ResponseStatus.ERROR_COMM, responseCommand.getResponseStatus());
                Assert.assertNull(responseCommand.getResponseHost());
                synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                    invoked.set(true);
                    DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                }
            }

        });
        synchronized (this.remotingClient) {
            while (!invoked.get()) {
                this.remotingClient.wait();
            }
        }

        // 单group没有连接情形，发送到所有连接
        invoked.set(false);
        this.remotingClient.sendToGroupAllConnections(this.group, this.createDummyRequest(),
            new GroupAllConnectionCallBackListener() {

                public void onResponse(final Map<Connection, ResponseCommand> resultMap) {
                    Assert.assertEquals(0, resultMap.size());
                    invoked.set(true);
                }


                public ThreadPoolExecutor getExecutor() {
                    return null;
                }
            });
        synchronized (this.remotingClient) {
            while (!invoked.get()) {
                this.remotingClient.wait();
            }
        }

        // 多分组发送的情形，全部没有连接的情形
        invoked.set(false);
        final String group2 = RemotingUtils.formatServerUrl(WireFormatType.valueOf("NOTIFY_V1"), "localhost", PORT + 1);
        Map<String, RequestCommand> groupObejcts = this.createGroupObjects(this.group, group2);
        this.remotingClient.sendToGroups(groupObejcts, new MultiGroupCallBackListener() {

            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                Assert.assertEquals(2, groupResponses.size());
                for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                    System.out.println(entry.getValue().getResponseStatus());
                    Assert.assertEquals(ResponseStatus.ERROR_COMM, entry.getValue().getResponseStatus());
                }
                synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                    invoked.set(true);
                    DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                }
            }

        }, 5000, TimeUnit.MILLISECONDS);

        synchronized (this.remotingClient) {
            while (!invoked.get()) {
                this.remotingClient.wait();
            }
        }
        this.assertCallBackClear();
        // 多分组发送的情形，部分没有连接的情形
        invoked.set(false);
        groupObejcts = this.createGroupObjects(this.group, group2);
        // 启动服务器
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        server.registerProcessor(NotifyDummyRequestCommand.class, this.processor);
        server.start();
        try {
            // 连接group
            this.remotingClient.connect(this.group, 2);
            this.remotingClient.awaitReadyInterrupt(this.group);
            this.remotingClient.sendToGroups(groupObejcts, new MultiGroupCallBackListener() {

                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                    Assert.assertEquals(2, groupResponses.size());
                    for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                        System.out.println(entry.getValue().getResponseStatus());
                        if (entry.getKey().equals(group2)) {
                            Assert.assertEquals(ResponseStatus.ERROR_COMM, entry.getValue().getResponseStatus());
                        }
                        else {
                            Assert.assertEquals(ResponseStatus.NO_ERROR, entry.getValue().getResponseStatus());
                        }
                    }
                    synchronized (DefaultRemotingClientUnitTest.this.remotingClient) {
                        invoked.set(true);
                        DefaultRemotingClientUnitTest.this.remotingClient.notifyAll();
                    }
                }

            }, 5000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingClient) {
                while (!invoked.get()) {
                    this.remotingClient.wait();
                }
            }
            this.assertCallBackClear();

        }
        finally {
            server.stop();
        }
    }


    private Map<String, RequestCommand> createGroupObjects(final String group, final String group2) {
        final Map<String, RequestCommand> groupObjects = new HashMap<String, RequestCommand>();
        groupObjects.put(group, this.createDummyRequest());
        groupObjects.put(group2, this.createDummyRequest());
        return groupObjects;
    }


    @After
    public void tearDown() throws Exception {
        if (this.remotingClient.isStarted()) {
            this.remotingClient.stop();
        }
        if (this.processor != null) {
            this.processor.dispose();
        }
    }
}