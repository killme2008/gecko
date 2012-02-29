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
package com.taobao.gecko.core.intergration.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.junit.Ignore;

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.core.impl.TextLineCodecFactory;
import com.taobao.gecko.core.nio.TCPConnectorController;
import com.taobao.gecko.core.nio.TCPController;


@Ignore
public class TCPControllerTest extends TestCase {
    private static final int PORT = 11931;
    private static final int SENT_COUNT = 10000;
    TCPController controller;
    AtomicInteger serverReceived, clientReceived, serverSent;


    @Override
    public void setUp() throws IOException {
        this.controller = new TCPController();
        this.controller.setCodecFactory(new TextLineCodecFactory());
        this.controller.setHandler(new HandlerAdapter() {

            @Override
            public void onMessageReceived(Session session, Object msg) {
                TCPControllerTest.this.serverReceived.incrementAndGet();
                try {
                    assertTrue(session.asyncWrite(msg).get());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
                assertEquals("hello", msg);
            }


            @Override
            public void onMessageSent(Session session, Object msg) {
                TCPControllerTest.this.serverSent.incrementAndGet();
            }

        });

        this.controller.bind(PORT);
        assertTrue(this.controller.isStarted());
        this.serverReceived = new AtomicInteger();
        this.serverSent = new AtomicInteger();
        this.clientReceived = new AtomicInteger();
    }


    public void testEcho() throws Exception {
        TCPConnectorController connector = new TCPConnectorController();
        connector.setCodecFactory(new TextLineCodecFactory());
        connector.setHandler(new HandlerAdapter() {
            @Override
            public void onMessageReceived(Session session, Object msg) {
                if (TCPControllerTest.this.clientReceived.incrementAndGet() == SENT_COUNT) {
                    synchronized (TCPControllerTest.this) {
                        TCPControllerTest.this.notifyAll();
                    }
                }

            }

        });
        connector.connect(new InetSocketAddress(PORT));
        connector.awaitConnectUnInterrupt();
        assertTrue(connector.isConnected());
        for (int i = 0; i < SENT_COUNT; i++) {
            assertTrue(connector.send("hello").get());
        }
        synchronized (this) {
            while (this.clientReceived.get() != SENT_COUNT) {
                this.wait(1000);
            }
        }
        assertEquals(SENT_COUNT, this.serverReceived.get());
        assertEquals(SENT_COUNT, this.serverSent.get());
        assertEquals(SENT_COUNT, this.clientReceived.get());
        connector.stop();
        assertFalse(connector.isStarted());
        assertFalse(connector.isConnected());
    }


    @Override
    public void tearDown() throws IOException {
        this.controller.stop();
        assertTrue(!this.controller.isStarted());
    }
}