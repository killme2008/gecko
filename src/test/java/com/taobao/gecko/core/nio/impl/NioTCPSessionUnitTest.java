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
package com.taobao.gecko.core.nio.impl;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.core.impl.ByteBufferWriteMessage;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.core.impl.TextLineCodecFactory;
import com.taobao.gecko.core.nio.NioSessionConfig;
import com.taobao.gecko.core.nio.TCPController;
import com.taobao.gecko.core.statistics.impl.DefaultStatistics;


/**
 *
 *
 *
 * @author boyan
 *
 * @since 1.0, 2009-12-24 下午03:56:48
 */

public class NioTCPSessionUnitTest {
    private NioTCPSession session;
    private MockSelectableChannel channel;
    private MockHandler handler;
    private SelectorManager selectorManager;
    private Queue<WriteMessage> queue;

    static private class MockHandler implements Handler {
        private Object lastMessage;


        public void onExceptionCaught(Session session, Throwable throwable) {
            throwable.printStackTrace();

        }


        public void onMessageReceived(Session session, Object msg) {
            this.lastMessage = msg;
        }


        public void onMessageSent(Session session, Object msg) {
            // TODO Auto-generated method stub

        }


        public void onSessionClosed(Session session) {
            // TODO Auto-generated method stub

        }


        public void onSessionConnected(Session session, Object... args) {
            // TODO Auto-generated method stub

        }


        public void onSessionCreated(Session session) {
            // TODO Auto-generated method stub

        }


        public void onSessionExpired(Session session) {
            // TODO Auto-generated method stub

        }


        public void onSessionIdle(Session session) {
            // TODO Auto-generated method stub

        }


        public void onSessionStarted(Session session) {
            // TODO Auto-generated method stub

        }

    }


    @Before
    public void setUp() throws Exception {
        this.channel = new MockSelectableChannel();
        Configuration configuration = new Configuration();
        TCPController controller = new TCPController(configuration);
        this.selectorManager = new SelectorManager(1, controller, configuration);
        this.selectorManager.start();
        this.handler = new MockHandler();
        this.queue = new LinkedList<WriteMessage>();
        NioSessionConfig sessionConfig =
                new NioSessionConfig(this.channel, this.handler, this.selectorManager, new TextLineCodecFactory(),
                    new DefaultStatistics(), this.queue, null, true, -1, -1);

        this.session = new NioTCPSession(sessionConfig, 4096);
    }


    @Test
    public void testInsertTimer() throws Exception {
        final long start = System.currentTimeMillis();
        this.session.insertTimer(new TimerRef(2000, new Runnable() {

            public void run() {
                long duration = System.currentTimeMillis() - start;
                System.out.println(duration);
                Assert.assertEquals(2000, duration, 100);
            }
        }));
        Thread.sleep(2000);

    }


    @Test
    public void testInsertTimer_Cancel() throws Exception {
        TimerRef timerRef = new TimerRef(1000, new Runnable() {
            public void run() {
                Assert.fail();
            }
        });
        this.session.insertTimer(timerRef);
        timerRef.cancel();
        Thread.sleep(2000);
    }


    @Test
    public void testWriteToChannel_Complete() throws Exception {

        ByteBufferWriteMessage message = new ByteBufferWriteMessage("hello", new FutureImpl<Boolean>());
        message.setWriteBuffer(IoBuffer.wrap("hello".getBytes()));
        this.channel.written = 5;
        this.channel.writeTimesToReturnZero = Integer.MAX_VALUE;
        Assert.assertEquals("hello", this.session.writeToChannel0(message));
        Assert.assertTrue(message.getWriteFuture().get());
        Assert.assertFalse(message.getWriteBuffer().hasRemaining());

    }


    @Test
    public void testWriteToChannel_Twice_Complete() throws Exception {
        ByteBufferWriteMessage message = new ByteBufferWriteMessage("hello good", new FutureImpl<Boolean>());
        message.setWriteBuffer(IoBuffer.wrap("hello good".getBytes()));
        message.getWriteBuffer().position(5);
        // 分两次写入，每次写入5个字节
        this.channel.written = 5;
        this.channel.writeTimesToReturnZero = Integer.MAX_VALUE;
        Assert.assertEquals("hello good", this.session.writeToChannel0(message));
        Assert.assertTrue(message.getWriteFuture().get());
        Assert.assertFalse(message.getWriteBuffer().hasRemaining());
    }


    @Test
    public void testWriteToChannel_Not_Complete() throws Exception {
        ByteBufferWriteMessage message = new ByteBufferWriteMessage("hello good", new FutureImpl<Boolean>());
        message.setWriteBuffer(IoBuffer.wrap("hello good".getBytes()));
        // 先写入3个字节，没有完全写入
        this.channel.written = 3;
        this.channel.writeTimesToReturnZero = 1;
        Assert.assertNull(this.session.writeToChannel0(message));
        Assert.assertFalse(message.getWriteFuture().isDone());
        Assert.assertTrue(message.getWriteBuffer().hasRemaining());

        // 下次应该完全写入剩下的7个字节
        this.channel.written = 7;
        this.channel.writeTimesToReturnZero = Integer.MAX_VALUE;
        Assert.assertEquals("hello good", this.session.writeToChannel0(message));
        Assert.assertTrue(message.getWriteFuture().get());
        Assert.assertFalse(message.getWriteBuffer().hasRemaining());
    }


    @Test
    public void testWriteToChannel_NullBuffer() throws Exception {
        ByteBufferWriteMessage message = new ByteBufferWriteMessage("hello good", new FutureImpl<Boolean>());
        Assert.assertNull(message.getWriteBuffer());
        Assert.assertEquals("hello good", this.session.writeToChannel0(message));
        Assert.assertTrue(message.getWriteFuture().get());
        Assert.assertNull(message.getWriteBuffer());
    }


    @Test(timeout = 20000)
    public void testReadFromBuffer_Normal() throws Exception {
        this.channel.readBytes = "hello\r\n".getBytes();
        this.channel.readTimesToReturnZero = 1;
        this.session.readFromBuffer();
        Assert.assertEquals("hello", this.handler.lastMessage);
        Thread.sleep(2000);
        // 确认是否继续注册
        Assert.assertSame(this.session, this.channel.attch);
        Assert.assertEquals(SelectionKey.OP_READ, this.channel.ops);
    }


    @Test(timeout = 20000)
    public void testReadFromBuffer_UnComplete_Then_Complete() throws Exception {
        this.channel.readBytes = "hell".getBytes();
        this.channel.readTimesToReturnZero = 1;
        this.session.readFromBuffer();
        Assert.assertNull(this.handler.lastMessage);
        Thread.sleep(2000);
        // 确认是否继续注册
        Assert.assertSame(this.session, this.channel.attch);
        Assert.assertEquals(SelectionKey.OP_READ, this.channel.ops);

        // 计数清0，再次读，可以完全decode并派发给handler
        this.channel.readTimes = 0;
        this.channel.readBytes = "o\r\n".getBytes();
        this.session.readFromBuffer();
        Assert.assertEquals("hello", this.handler.lastMessage);
        Thread.sleep(2000);
        // 确认是否继续注册
        Assert.assertSame(this.session, this.channel.attch);
        Assert.assertEquals(SelectionKey.OP_READ, this.channel.ops);
    }


    @Test(timeout = 20000)
    public void testReadFromBuffer_CloseChannel()throws Exception {
        // 测试读到-1的情况，应该关闭连接
        this.channel.readTimesToReturnZero = Integer.MAX_VALUE;
        Assert.assertTrue(this.channel.isOpen());
        this.session.readFromBuffer();
        Thread.sleep(1000);
        Assert.assertFalse(this.channel.isOpen());
    }


    @After
    public void tearDown() throws Exception {
        this.selectorManager.stop();
        Thread.sleep(1000);
    }
}