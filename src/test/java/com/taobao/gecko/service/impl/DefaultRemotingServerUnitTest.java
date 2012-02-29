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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.nio.TCPConnectorController;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.GroupAllConnectionCallBackListener;
import com.taobao.gecko.service.MultiGroupCallBackListener;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.callback.SingleRequestCallBack;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.notify.NotifyWireFormatType;
import com.taobao.gecko.service.notify.OpCode;
import com.taobao.gecko.service.notify.RequestCommandEncoder;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyBooleanAckCommand;
import com.taobao.gecko.service.notify.response.NotifyResponseCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-22 上午10:16:37
 */

public class DefaultRemotingServerUnitTest {
    private static final int PORT = 9191;
    private static final String GROUP = "test";
    private RemotingServer remotingServer;


    @Before
    public void setUp() throws Exception {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        serverConfig.setPort(PORT);
        this.remotingServer = RemotingFactory.bind(serverConfig);
    }


    @Test
    public void testAttributes() {
        final String key = "key";
        final Object value = 1000;
        Assert.assertNull(this.remotingServer.getAttribute("group1", key));
        this.remotingServer.setAttribute("group1", key, value);
        Assert.assertEquals(value, this.remotingServer.getAttribute("group1", key));
        Assert.assertEquals(value, this.remotingServer.removeAttribute("group1", key));
        Assert.assertNull(this.remotingServer.getAttribute("group1", key));
    }

    private static final class JoinGroupListener implements ConnectionLifeCycleListener {
        public void onConnectionClosed(final Connection conn) {

        }


        public void onConnectionReady(final Connection conn) {
            // TODO Auto-generated method stub

        }


        public ThreadPoolExecutor getExecutor() {
            return null;
        }


        public void onConnectionCreated(final Connection conn) {
            System.out.println("连接建立，并加入test分组");
            conn.getRemotingContext().addConnectionToGroup(GROUP, conn);

        }

    }


    @Test(timeout = 30000)
    public void testSendToGroup() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            Thread.sleep(3000);
            Assert.assertEquals(3, processor.recvCount.get());

            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null),
                new SingleRequestCallBackListener() {

                    public void onException(final Exception e) {

                    }


                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                        Assert.assertNotNull(responseCommand);
                        Assert.assertEquals(OpCode.DUMMY, ((NotifyResponseCommand) responseCommand).getOpCode());
                        Assert.assertEquals(ResponseStatus.NO_ERROR, responseCommand.getResponseStatus());
                        invoked.set(true);
                        synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                            DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                        }
                    }

                });

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(4, processor.recvCount.get());

            try {
                this.remotingServer.sendToGroup(null, new NotifyDummyRequestCommand((String) null));
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("Null group", e.getMessage());
            }
            try {
                this.remotingServer.sendToGroup(GROUP, null);
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("Null command", e.getMessage());
            }
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    @Test
    public void testTransferToGroup() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);
        final File tmpFile = File.createTempFile("testTransferToGroup", "test");
        final FileChannel channel = new RandomAccessFile(tmpFile, "rw").getChannel();

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            this.writeCmd2Channel(channel, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.transferToGroup(GROUP, null, null, channel, 0, channel.size());
            this.writeCmd2Channel(channel, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.transferToGroup(GROUP, null, null, channel, 0, channel.size());
            this.writeCmd2Channel(channel, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.transferToGroup(GROUP, null, null, channel, 0, channel.size());
            Thread.sleep(3000);
            Assert.assertEquals(3, processor.recvCount.get());

            final AtomicBoolean invoked = new AtomicBoolean(false);
            final NotifyDummyRequestCommand cmd = new NotifyDummyRequestCommand((String) null);
            this.writeCmd2Channel(channel, cmd);
            this.remotingServer.transferToGroup(GROUP, null, null, channel, 0, channel.size(), cmd.getOpaque(),
                new SingleRequestCallBackListener() {

                    public void onException(final Exception e) {

                    }


                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                        Assert.assertNotNull(responseCommand);
                        Assert.assertEquals(OpCode.DUMMY, ((NotifyResponseCommand) responseCommand).getOpCode());
                        Assert.assertEquals(ResponseStatus.NO_ERROR, responseCommand.getResponseStatus());
                        invoked.set(true);
                        synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                            DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                        }
                    }

                }, 10000, TimeUnit.MILLISECONDS);

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(4, processor.recvCount.get());

            try {
                this.remotingServer.transferToGroup(null, null, null, channel, 0, channel.position());
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("Null group", e.getMessage());
            }

        }
        finally {
            remotingClient.stop();
            processor.dispose();
            channel.close();
            tmpFile.delete();

        }
    }


    private void writeCmd2Channel(final FileChannel channel, final NotifyDummyRequestCommand command)
            throws IOException {
        final IoBuffer buf = new RequestCommandEncoder().encode(command, null);
        channel.position(0);
        while (buf.hasRemaining()) {
            channel.write(buf.buf());
        }
        channel.truncate(channel.size());
    }


    private void assertCallBackClear() {
        final List<Connection> connetionList =
                this.remotingServer.getRemotingContext().getConnectionsByGroup(Constants.DEFAULT_GROUP);
        for (final Connection conn : connetionList) {
            Assert.assertEquals(0, ((DefaultConnection) conn).getRequstCallBackCount());
        }
    }


    @Test(timeout = 10000)
    public void testSendToGroupTimeout() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        // 延迟应答
        processor.sleepTime = 2000;
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null),
                new SingleRequestCallBackListener() {

                    public void onException(final Exception e) {

                    }


                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                        Assert.assertNotNull(responseCommand);
                        // 确定是超时
                        Assert.assertEquals(ResponseStatus.TIMEOUT, responseCommand.getResponseStatus());
                        invoked.set(true);
                        synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                            DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                        }
                    }

                });

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(1, processor.recvCount.get());
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    @Test(timeout = 10000)
    public void testInvoke() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            final ResponseCommand response =
                    this.remotingServer.invokeToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            Assert.assertNotNull(response);
            Assert.assertEquals(OpCode.DUMMY, ((NotifyResponseCommand) response).getOpCode());
            Assert.assertEquals(ResponseStatus.NO_ERROR, response.getResponseStatus());
            Assert.assertEquals(1, processor.recvCount.get());
            this.assertCallBackClear();
            try {
                this.remotingServer.invokeToGroup(null, new NotifyDummyRequestCommand((String) null));
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("Null group", e.getMessage());
            }
            try {
                this.remotingServer.invokeToGroup(GROUP, null);
            }
            catch (final NotifyRemotingException e) {
                Assert.assertEquals("Null command", e.getMessage());
            }

        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    private ClientConfig newNotifyClientConfig() {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new NotifyWireFormatType());
        return clientConfig;
    }


    @Test(timeout = 10000)
    public void testInvokeTimeout() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        // 延迟响应
        processor.sleepTime = 2000;
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {

            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());

            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            try {
                this.remotingServer.invokeToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
                Assert.fail();
            }
            catch (final TimeoutException e) {
                Assert.assertEquals("Operation timeout", e.getMessage());
            }
            Assert.assertEquals(1, processor.recvCount.get());

        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    @Test(timeout = 10000)
    public void testSendToGroupAllConnections() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            // 测试单向发给分组的所有连接
            this.remotingServer.sendToGroupAllConnections(GROUP, new NotifyDummyRequestCommand((String) null));

            while (processor.recvCount.get() != 5) {
                Thread.sleep(500);
            }
            Assert.assertEquals(5, processor.recvCount.get());

            final AtomicBoolean invoked = new AtomicBoolean(false);

            // 测试设置监听器
            this.remotingServer.sendToGroupAllConnections(GROUP, new NotifyDummyRequestCommand((String) null),
                new GroupAllConnectionCallBackListener() {

                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final Map<Connection, ResponseCommand> resultMap) {
                        Assert.assertEquals(5, resultMap.size());
                        for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                            Assert.assertEquals(ResponseStatus.NO_ERROR, entry.getValue().getResponseStatus());
                        }
                        synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                            invoked.set(true);
                            DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                        }
                    }

                });

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }

    }


    @Test(timeout = 20000)
    public void testSendToGroupAllConnectionsTimeout() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        // 延时响应
        processor.sleepTime = 5000;
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }

            final AtomicBoolean invoked = new AtomicBoolean(false);

            this.remotingServer.sendToGroupAllConnections(GROUP, new NotifyDummyRequestCommand((String) null),
                new GroupAllConnectionCallBackListener() {

                    public ThreadPoolExecutor getExecutor() {
                        return null;
                    }


                    public void onResponse(final Map<Connection, ResponseCommand> resultMap) {
                        Assert.assertEquals(5, resultMap.size());
                        for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
                            Assert.assertEquals(ResponseStatus.TIMEOUT, entry.getValue().getResponseStatus());
                            Assert.assertEquals("等待响应超时", ((NotifyBooleanAckCommand) entry.getValue()).getErrorMsg());
                        }
                        synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                            invoked.set(true);
                            DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                        }
                    }

                });

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }

    }

    final static String GROUP2 = "test2";

    private static final class JoinDiffGroupListener implements ConnectionLifeCycleListener {

        int count = 0;


        public void onConnectionClosed(final Connection conn) {
            // TODO Auto-generated method stub

        }


        public void onConnectionReady(final Connection conn) {
            // TODO Auto-generated method stub

        }


        public void onConnectionCreated(final Connection conn) {
            this.count++;
            if (this.count % 2 == 0) {
                conn.getRemotingContext().addConnectionToGroup(GROUP, conn);
            }
            else {
                conn.getRemotingContext().addConnectionToGroup(GROUP2, conn);
            }
        }

    }


    @Test(timeout = 10000)
    public void testSendToGroups() throws Exception {
        // 加入不同分组
        this.remotingServer.addConnectionLifeCycleListener(new JoinDiffGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 10);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }
            while (this.remotingServer.getConnectionCount(GROUP2) < 5) {
                Thread.sleep(500);
            }
            // 测试单向发送
            this.remotingServer.sendToGroups(this.createGroupObjects());
            Thread.sleep(2000);
            Assert.assertEquals(2, processor.recvCount.get());

            // 测试回调监听器
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingServer.sendToGroups(this.createGroupObjects(), new MultiGroupCallBackListener() {

                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                    Assert.assertEquals("hello", args[0]);
                    Assert.assertEquals(2, groupResponses.size());
                    for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                        Assert.assertTrue(entry.getKey().equals(GROUP) || entry.getKey().equals(GROUP2));
                        Assert.assertTrue(entry.getValue().getResponseStatus() == ResponseStatus.NO_ERROR);
                    }
                    synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                        invoked.set(true);
                        DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                    }
                }

            }, 2000, TimeUnit.MILLISECONDS, "hello");

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
            Assert.assertEquals(4, processor.recvCount.get());
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    @Test(timeout = 20000)
    public void testSendToGroupsTimeout() throws Exception {
        // 加入不同分组
        this.remotingServer.addConnectionLifeCycleListener(new JoinDiffGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        processor.sleepTime = 5000;
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 10);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }
            while (this.remotingServer.getConnectionCount(GROUP2) < 5) {
                Thread.sleep(500);
            }
            // 测试回调监听器
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingServer.sendToGroups(this.createGroupObjects(), new MultiGroupCallBackListener() {

                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                    Assert.assertEquals("hello", args[0]);
                    Assert.assertEquals(2, groupResponses.size());
                    for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                        Assert.assertTrue(entry.getKey().equals(GROUP) || entry.getKey().equals(GROUP2));
                        Assert.assertTrue(entry.getValue().getResponseStatus() == ResponseStatus.TIMEOUT);
                    }
                    synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                        invoked.set(true);
                        DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                    }
                }

            }, 1000, TimeUnit.MILLISECONDS, "hello");

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            Thread.sleep(7000);
            this.assertCallBackClear();
            Assert.assertEquals(2, processor.recvCount.get());
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }
    }


    @Test(timeout = 60000)
    public void testSendToGroupsThreadPoolBusy() throws Exception {
        // 加入不同分组
        this.remotingServer.addConnectionLifeCycleListener(new JoinDiffGroupListener());
        final ClientConfig clientConfig = this.newNotifyClientConfig();
        clientConfig.setIdleTime(Integer.MAX_VALUE);

        final RemotingClient remotingClient = RemotingFactory.connect(clientConfig);
        // 限制连接池大小
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        processor.threadPoolExecutor.shutdown();
        processor.threadPoolExecutor =
                new ThreadPoolExecutor(1, 1, 60, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1));
        processor.sleepTime = 15000;
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 10);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }
            while (this.remotingServer.getConnectionCount(GROUP2) < 5) {
                Thread.sleep(500);
            }
            // 预先发送几次，让线程池繁忙
            this.remotingServer.sendToGroups(this.createGroupObjects());
            this.remotingServer.sendToGroups(this.createGroupObjects());

            Thread.sleep(5000);

            // 测试回调监听器
            final AtomicBoolean invoked = new AtomicBoolean(false);
            this.remotingServer.sendToGroups(this.createGroupObjects(), new MultiGroupCallBackListener() {

                public ThreadPoolExecutor getExecutor() {
                    return null;
                }


                public void onResponse(final Map<String, ResponseCommand> groupResponses, final Object... args) {
                    Assert.assertEquals("hello", args[0]);
                    System.out.println(groupResponses);
                    Assert.assertEquals(2, groupResponses.size());
                    for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                        Assert.assertTrue(entry.getKey().equals(GROUP) || entry.getKey().equals(GROUP2));
                        Assert.assertTrue(entry.getValue().getResponseStatus() == ResponseStatus.THREADPOOL_BUSY);
                    }
                    synchronized (DefaultRemotingServerUnitTest.this.remotingServer) {
                        invoked.set(true);
                        DefaultRemotingServerUnitTest.this.remotingServer.notifyAll();
                    }
                }

            }, 5000, TimeUnit.MILLISECONDS, "hello");

            synchronized (this.remotingServer) {
                while (!invoked.get()) {
                    this.remotingServer.wait();
                }
            }
            this.assertCallBackClear();
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }

    }


    @Test(timeout = 10000)
    public void testStartStop() throws Exception {
        Assert.assertTrue(this.remotingServer.isStarted());
        try {
            this.remotingServer.setServerConfig(new ServerConfig());
            Assert.fail();
        }
        catch (final IllegalStateException e) {

        }
        // 重新启动
        this.remotingServer.start();
        Assert.assertTrue(this.remotingServer.isStarted());

        // 尝试发送消息
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        final DummyRequestProcessor processor = new DummyRequestProcessor();
        remotingClient.registerProcessor(NotifyDummyRequestCommand.class, processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());
            while (this.remotingServer.getConnectionCount(GROUP) < 5) {
                Thread.sleep(500);
            }
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            this.remotingServer.sendToGroup(GROUP, new NotifyDummyRequestCommand((String) null));
            Thread.sleep(3000);
            Assert.assertEquals(3, processor.recvCount.get());
        }
        finally {
            remotingClient.stop();
            processor.dispose();
        }

    }


    @Test(timeout = 10000)
    public void testNoProcessor() throws Exception {
        this.remotingServer.addConnectionLifeCycleListener(new JoinGroupListener());
        final RemotingClient remotingClient = RemotingFactory.connect(this.newNotifyClientConfig());
        // 客户端没有注册请求处理器
        // remotingClient.registerProcessor(DummyRequestCommand.class,
        // processor);

        try {
            remotingClient.connect(this.remotingServer.getConnectURI().toString(), 5);
            remotingClient.awaitReadyInterrupt(this.remotingServer.getConnectURI().toString());

            final ResponseCommand response =
                    this.remotingServer.invokeToGroup(GROUP, new NotifyDummyRequestCommand("test"), 5000,
                        TimeUnit.MILLISECONDS);
            Assert.assertNotNull(response);
            Assert.assertEquals(ResponseStatus.NO_PROCESSOR, response.getResponseStatus());
        }
        finally {
            remotingClient.stop();
        }
    }


    private Map<String, RequestCommand> createGroupObjects() {
        final Map<String, RequestCommand> groupObjects = new HashMap<String, RequestCommand>();
        groupObjects.put(GROUP, new NotifyDummyRequestCommand((String) null));
        groupObjects.put(GROUP2, new NotifyDummyRequestCommand((String) null));
        return groupObjects;
    }


    @Test(timeout = 60000)
    public void testRemoveInvalidConnection() throws Exception {
        this.remotingServer.stop();

        InvalidConnectionScanTask.TIMEOUT_THRESHOLD = 4000L;
        try {
            final ServerConfig serverConfig = new ServerConfig();
            serverConfig.setPort(PORT);
            serverConfig.setScanAllConnectionInterval(2);
            this.remotingServer = RemotingFactory.bind(serverConfig);
            final TCPConnectorController connector = new TCPConnectorController();
            connector.setHandler(new HandlerAdapter());
            connector.connect(new InetSocketAddress(PORT));
            connector.awaitConnectUnInterrupt();
            Assert.assertTrue(connector.isConnected());
            Thread.sleep(1000);
            Assert.assertEquals(1, this.remotingServer.getConnectionCount(Constants.DEFAULT_GROUP));
            Thread.sleep(10000);
            Assert.assertEquals(0, this.remotingServer.getConnectionCount(Constants.DEFAULT_GROUP));
            Assert.assertFalse(connector.isConnected());
            connector.stop();
        }
        finally {
            InvalidConnectionScanTask.TIMEOUT_THRESHOLD = 300000L;
        }
    }


    @Test
    public void testRemoveInvalidCallBack() throws Exception {
        this.remotingServer.stop();

        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(PORT);
        serverConfig.setScanAllConnectionInterval(2);
        this.remotingServer = RemotingFactory.bind(serverConfig);

        final TCPConnectorController connector = new TCPConnectorController();
        connector.setHandler(new HandlerAdapter());
        connector.connect(new InetSocketAddress(PORT));
        connector.awaitConnectUnInterrupt();
        Assert.assertTrue(connector.isConnected());
        Thread.sleep(1000);
        final DefaultConnection conn =
                (DefaultConnection) this.remotingServer.getRemotingContext()
                    .getConnectionsByGroup(Constants.DEFAULT_GROUP).iterator().next();
        Assert.assertNotNull(conn);
        // callback超时1秒
        conn.addRequestCallBack(0, new SingleRequestCallBack(null, 1000));
        Assert.assertNotNull(conn.getRequestCallBack(0));
        // 等待两个扫描周期
        Thread.sleep(4000);
        Assert.assertNull(conn.getRequestCallBack(0));
        // 确认callBack已经被移除
        connector.stop();

    }


    @After
    public void tearDown() throws Exception {
        this.remotingServer.stop();
    }
}