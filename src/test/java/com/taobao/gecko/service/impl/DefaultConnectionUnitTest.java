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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.callback.SingleRequestCallBack;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.mock.MockSession;
import com.taobao.gecko.service.notify.NotifyCommandFactory;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyBooleanAckCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 下午02:25:29
 */

public class DefaultConnectionUnitTest {
    private DefaultConnection connection;
    private MockSession mockSession;
    private DefaultRemotingContext remotingContext;


    @Before
    public void setUp() {
        this.mockSession = new MockSession();
        this.remotingContext = new DefaultRemotingContext(new ClientConfig(), new NotifyCommandFactory());
        this.connection = new DefaultConnection(this.mockSession, this.remotingContext);
    }


    @After
    public void tearDown() throws Exception {
        this.connection.close(false);
        this.remotingContext.dispose();
    }


    @Test
    public void testSendMessage() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("hello");
        this.connection.send(requestCommand);
        Assert.assertTrue(this.mockSession.getMessageList().contains(requestCommand));
        Assert.assertNull(this.connection.getRequestCallBack(requestCommand.getOpaque()));
    }


    @Test
    public void testSendMessageWithListener() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("hello");
        final NotifyDummyAckCommand response = new NotifyDummyAckCommand(requestCommand, "hello");
        final AtomicBoolean invoked = new AtomicBoolean(false);
        this.connection.send(requestCommand, new SingleRequestCallBackListener() {

            public void onException(final Exception e) {
                e.printStackTrace();
            }


            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                Assert.assertSame(responseCommand, response);
                Assert.assertSame(DefaultConnectionUnitTest.this.connection, conn);
                synchronized (DefaultConnectionUnitTest.this.connection) {
                    invoked.set(true);
                    DefaultConnectionUnitTest.this.connection.notifyAll();
                }

            }

        }, 5000, TimeUnit.MILLISECONDS);

        final RequestCallBack requestCallBack = this.connection.getRequestCallBack(requestCommand.getOpaque());
        Assert.assertNotNull(requestCallBack);
        requestCallBack.onResponse("test", response, this.connection);
        synchronized (this.connection) {
            while (!invoked.get()) {
                this.connection.wait();
            }
        }
        Assert.assertTrue(invoked.get());
    }


    @Test
    public void testSendMessageWithListenerTimeout() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("hello");
        final NotifyDummyAckCommand response = new NotifyDummyAckCommand(requestCommand, "hello");
        final AtomicBoolean invoked = new AtomicBoolean(false);
        this.connection.send(requestCommand, new SingleRequestCallBackListener() {

            public void onException(final Exception e) {
                e.printStackTrace();
            }


            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void onResponse(final ResponseCommand responseCommand, final Connection conn) {
                Assert.assertNotSame(response, responseCommand);
                Assert.assertEquals("等待响应超时", ((NotifyBooleanAckCommand) responseCommand).getErrorMsg());
                Assert.assertEquals(ResponseStatus.TIMEOUT, responseCommand.getResponseStatus());
                Assert.assertSame(DefaultConnectionUnitTest.this.connection, conn);
                synchronized (DefaultConnectionUnitTest.this.connection) {
                    invoked.set(true);
                    DefaultConnectionUnitTest.this.connection.notifyAll();
                }

            }

        }, 2000, TimeUnit.MILLISECONDS);

        final RequestCallBack requestCallBack = this.connection.getRequestCallBack(requestCommand.getOpaque());
        Assert.assertNotNull(requestCallBack);
        synchronized (this.connection) {
            while (!invoked.get()) {
                this.connection.wait();
            }
        }
        Assert.assertTrue(invoked.get());
    }

    private class InnerSetResultRunner implements Runnable {
        final NotifyDummyRequestCommand request;
        final NotifyDummyAckCommand response;


        public InnerSetResultRunner(final NotifyDummyRequestCommand requestCommand, final NotifyDummyAckCommand response) {
            super();
            this.request = requestCommand;
            this.response = response;
        }


        public void run() {
            try {
                while (DefaultConnectionUnitTest.this.connection.getRequestCallBack(this.request.getOpaque()) == null) {
                    Thread.sleep(100);
                }
                final RequestCallBack requestCallBack =
                        DefaultConnectionUnitTest.this.connection.getRequestCallBack(this.request.getOpaque());
                requestCallBack.onResponse("test", this.response, DefaultConnectionUnitTest.this.connection);
            }
            catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void testInvoke() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("hello");
        final NotifyDummyAckCommand response = new NotifyDummyAckCommand(requestCommand, "hello");
        new Thread(new InnerSetResultRunner(requestCommand, response)).start();
        final ResponseCommand responseCommand = this.connection.invoke(requestCommand);
        Assert.assertSame(response, responseCommand);

    }


    @Test
    public void testAddRemoveInvalidRequstCallback() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("test");
        RequestCallBack requestCallBack = new SingleRequestCallBack(requestCommand.getRequestHeader(), 2000);
        this.connection.addRequestCallBack(requestCommand.getOpaque(), requestCallBack);
        try {
            this.connection.addRequestCallBack(requestCommand.getOpaque(), requestCallBack);
        }
        catch (final NotifyRemotingException e) {
            Assert.assertEquals("请不要重复发送同一个命令到同一个连接", e.getMessage());
        }

        Assert.assertSame(requestCallBack, this.connection.getRequestCallBack(requestCommand.getOpaque()));
        Assert.assertSame(requestCallBack, this.connection.removeRequestCallBack(requestCommand.getOpaque()));
        Assert.assertNull(this.connection.getRequestCallBack(requestCommand.getOpaque()));
        requestCallBack = new SingleRequestCallBack(requestCommand.getRequestHeader(), 2000);
        this.connection.addRequestCallBack(requestCommand.getOpaque(), requestCallBack);
        Assert.assertSame(requestCallBack, this.connection.getRequestCallBack(requestCommand.getOpaque()));
        Thread.sleep(3000);
        this.connection.removeAllInvalidRequestCallBack();
        Assert.assertNull(this.connection.getRequestCallBack(requestCommand.getOpaque()));

    }


    @Test
    public void testInvokeTimeout() throws Exception {
        final NotifyDummyRequestCommand requestCommand = new NotifyDummyRequestCommand("hello");
        try {
            this.connection.invoke(requestCommand);
            Assert.fail();
        }
        catch (final java.util.concurrent.TimeoutException e) {
            Assert.assertEquals("Operation timeout", e.getMessage());
        }

    }
}