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
package com.taobao.gecko.core.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.AbstractControllerUnitTest;
import com.taobao.gecko.core.core.impl.HandlerAdapter;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÉÏÎç11:24:30
 */

public class TCPConnectorControllerUnitTest extends AbstractControllerUnitTest {

    private static final int PORT = 8080;
    private TCPConnectorController connector;


    @Override
    @After
    public void tearDown() throws Exception {
        if (this.connector != null) {
            this.connector.disconnect();
        }
        super.tearDown();

    }


    @Test
    public void connectServerDown() throws Exception {
        TCPController server = new TCPController();
        server.setHandler(new HandlerAdapter());
        server.bind(8080);
        this.connector = new TCPConnectorController();
        this.connector.setHandler(new HandlerAdapter());
        this.connector.connect(new InetSocketAddress("localhost", PORT));
        this.connector.awaitConnectUnInterrupt();
        Assert.assertTrue(this.connector.isConnected());

        server.stop();
        Thread.sleep(2000);
        Assert.assertFalse(this.connector.isConnected());
    }


    @Test
    public void testConnectFail() throws Exception {
        this.connector = new TCPConnectorController();
        this.connector.setHandler(new HandlerAdapter());

        Future<Boolean> future = this.connector.connect(new InetSocketAddress("localhost", PORT));
        try {
            this.connector.awaitConnectUnInterrupt();
            Assert.fail();
        }
        catch (IOException e) {

        }
        try {
            future.get();
            Assert.fail();
        }
        catch (ExecutionException e) {

        }
    }


    @Test
    public void testConnectAndSendMessageDisconnect() throws Exception {
        TCPController server = new TCPController();
        try {
            final AtomicInteger connectedCount = new AtomicInteger();
            final AtomicInteger recvSize = new AtomicInteger();
            server.setHandler(new HandlerAdapter() {

                @Override
                public void onSessionCreated(Session session) {
                    connectedCount.incrementAndGet();
                }


                @Override
                public void onMessageReceived(Session session, Object message) {
                    recvSize.addAndGet(((IoBuffer) message).remaining());
                }

            });
            server.bind(8080);

            this.connector = new TCPConnectorController();
            this.connector.setHandler(new HandlerAdapter());
            Assert.assertFalse(this.connector.isConnected());
            try {
                this.connector.connect(null);
                Assert.fail();
            }
            catch (IllegalArgumentException e) {
                Assert.assertEquals("Null remote address", e.getMessage());

            }

            try {
                this.connector.send(IoBuffer.allocate(1));
                Assert.fail();
            }
            catch (IllegalStateException e) {
                Assert.assertEquals("SocketChannel has not been connected", e.getMessage());
            }

            Future<Boolean> future = this.connector.connect(new InetSocketAddress("localhost", PORT));
            this.connector.awaitConnectUnInterrupt();
            Assert.assertTrue(this.connector.isConnected());
            Assert.assertTrue(future.get());
            Assert.assertEquals(1, connectedCount.get());

            Assert.assertTrue(this.connector.send(IoBuffer.allocate(10)).get());
            Thread.sleep(1000);
            Assert.assertEquals(10, recvSize.get());

            this.connector.disconnect();

            Assert.assertFalse(this.connector.isConnected());

            try {
                this.connector.send(IoBuffer.allocate(1));
                Assert.fail();
            }
            catch (IllegalStateException e) {
                Assert.assertEquals("SocketChannel has not been connected", e.getMessage());
            }
        }
        finally {
            server.stop();
        }

    }

}