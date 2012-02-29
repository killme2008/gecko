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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.nio.TCPConnectorController;
import com.taobao.gecko.core.nio.TCPController;
import com.taobao.gecko.core.nio.impl.NioTCPSession;


public class InputOutputStreamUnitTest extends TestCase {
    private static final int PORT = 11931;
    private static final int SENT_COUNT = 10000;
    TCPController controller;
    AtomicInteger serverReceivedBytes, clientReceivedBytes, serverSentBytes;


    @Override
    public void setUp() throws IOException {
        this.controller = new TCPController();
        this.controller.setHandler(new HandlerAdapter() {
            @Override
            public void onMessageReceived(Session session, Object msg) {
                try {
                    InputStream in = ((NioTCPSession) session).getInputStream(msg);
                    OutputStream out = ((NioTCPSession) session).getOutputStream(in.available(), false);
                    InputOutputStreamUnitTest.this.serverReceivedBytes.addAndGet(in.available());
                    int b = -1;
                    while ((b = in.read()) != -1) {
                        out.write(b);
                    }
                    out.flush();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onMessageSent(Session session, Object msg) {
                InputOutputStreamUnitTest.this.serverSentBytes.addAndGet(((IoBuffer) msg).capacity());
            }

        });

        this.controller.bind(new InetSocketAddress("localhost", PORT));
        assertTrue(this.controller.isStarted());
        this.serverReceivedBytes = new AtomicInteger();
        this.serverSentBytes = new AtomicInteger();
        this.clientReceivedBytes = new AtomicInteger();
    }


    public void testEcho() throws Exception {
        TCPConnectorController connector = new TCPConnectorController();
        connector.setHandler(new HandlerAdapter() {
            @Override
            public void onMessageReceived(Session session, Object msg) {
                if (InputOutputStreamUnitTest.this.clientReceivedBytes.addAndGet(((IoBuffer) msg).capacity()) == SENT_COUNT) {
                    synchronized (InputOutputStreamUnitTest.this) {
                        InputOutputStreamUnitTest.this.notifyAll();
                    }
                }

            }

        });
        connector.connect(new InetSocketAddress("localhost", PORT));
        connector.awaitConnectUnInterrupt();
        assertTrue(connector.isConnected());
        for (int i = 0; i < SENT_COUNT; i++) {
            assertTrue(connector.send(IoBuffer.wrap("hello".getBytes())).get());
        }
        synchronized (this) {
            while (this.clientReceivedBytes.get() != SENT_COUNT * 5) {
                this.wait(1000);
            }
        }
        assertEquals(5 * SENT_COUNT, this.serverReceivedBytes.get());
        assertEquals(5 * SENT_COUNT, this.serverSentBytes.get());
        assertEquals(5 * SENT_COUNT, this.clientReceivedBytes.get());
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