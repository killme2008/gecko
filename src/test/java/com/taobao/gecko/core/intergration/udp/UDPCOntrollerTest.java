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
package com.taobao.gecko.core.intergration.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.UDPSession;
import com.taobao.gecko.core.core.impl.TextLineCodecFactory;
import com.taobao.gecko.core.core.impl.UDPHandlerAdapter;
import com.taobao.gecko.core.nio.UDPConnectorController;
import com.taobao.gecko.core.nio.UDPController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@Ignore
public class UDPCOntrollerTest {
    private static final int PORT = 11932;

    private static final InetSocketAddress INET_SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", PORT);
    private static final int SENT_COUNT = 10;
    UDPController controller;
    AtomicInteger serverReceived, clientReceived, serverSent;


    @Before
    public void setUp() throws IOException {
        this.controller = new UDPController();
        this.controller.setCodecFactory(new TextLineCodecFactory());
        this.controller.setHandler(new UDPHandlerAdapter() {

            @Override
            public void onMessageReceived(UDPSession udpSession, SocketAddress address, Object message) {
                System.out.println(address);
                UDPCOntrollerTest.this.serverReceived.incrementAndGet();
                try {
                    assertTrue(udpSession.asyncWrite(address, message).get());
                }
                catch (Exception e) {
                    fail();
                }
                System.out.println("send back");
            }


            @Override
            public void onMessageSent(Session session, Object msg) {
                UDPCOntrollerTest.this.serverSent.incrementAndGet();
            }

        });

        this.controller.bind(INET_SOCKET_ADDRESS);
        assertTrue(this.controller.isStarted());
        this.serverReceived = new AtomicInteger();
        this.serverSent = new AtomicInteger();
        this.clientReceived = new AtomicInteger();
    }


    @Test
    public void testEcho() throws Exception {
        UDPConnectorController connector = new UDPConnectorController();
        connector.setCodecFactory(new TextLineCodecFactory());
        connector.setHandler(new UDPHandlerAdapter() {

            @Override
            public void onMessageReceived(UDPSession udpSession, SocketAddress address, Object message) {
                System.out.println("recv ...");
                if (UDPCOntrollerTest.this.clientReceived.incrementAndGet() == SENT_COUNT) {
                    synchronized (UDPCOntrollerTest.this) {
                        UDPCOntrollerTest.this.notifyAll();
                    }
                }

            }

        });
        connector.start();
        assertTrue(connector.isStarted());
        connector.connect(new InetSocketAddress("127.0.0.1", PORT));
        System.out.println("localAddr:" + connector.getLocalSocketAddress());
        for (int i = 0; i < SENT_COUNT; i++) {
            assertTrue(connector.send(new DatagramPacket("hello\r\n".getBytes(), 7)).get());
        }
        synchronized (this) {
            while (this.clientReceived.get() != SENT_COUNT) {
                System.out.println(serverSent.get());
                this.wait(1000);
            }
        }
        assertEquals(SENT_COUNT, this.serverReceived.get());
        assertEquals(SENT_COUNT, this.serverSent.get());
        assertEquals(SENT_COUNT, this.clientReceived.get());
        // test disconnect
        connector.disconnect();
        try {
            connector.send(new DatagramPacket("hello\r\n".getBytes(), 7));
            fail();
        }
        catch (IllegalStateException e) {
            assertEquals("Controller has been stopped", e.getMessage());
        }
        connector.connect(new InetSocketAddress("127.0.0.1", PORT));
        assertTrue(connector.send(new DatagramPacket("hello\r\n".getBytes(), 7)).get());
        Thread.sleep(1000);
        assertEquals(SENT_COUNT + 1, this.serverReceived.get());
        connector.stop();
        assertFalse(connector.isStarted());
    }


    @After
    public void tearDown() throws IOException {
        this.controller.stop();
        assertTrue(!this.controller.isStarted());
    }
}