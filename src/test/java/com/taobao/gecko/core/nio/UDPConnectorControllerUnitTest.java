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
import java.net.DatagramPacket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.core.impl.UDPHandlerAdapter;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÏÂÎç01:46:11
 */

public class UDPConnectorControllerUnitTest {
    private UDPConnectorController connector;


    @After
    public void tearDown() throws IOException {
        if (this.connector != null) {
            this.connector.disconnect();
        }
    }


    @Test
    public void testConnectAndSendThenDisconnect() throws Exception {
        UDPController server = new UDPController();
        final AtomicInteger recvSize = new AtomicInteger();
        server.setHandler(new UDPHandlerAdapter() {

            @Override
            public void onMessageReceived(Session session, Object message) {
                DatagramPacket packet = (DatagramPacket) message;
                recvSize.addAndGet(packet.getLength());
            }

        });
        server.start();

        this.connector = new UDPConnectorController();
        this.connector.setHandler(new HandlerAdapter());
        Assert.assertFalse(this.connector.isConnected());
        this.connector.connect(server.getLocalSocketAddress());
        Assert.assertTrue(this.connector.isConnected());

        this.connector.send(new DatagramPacket("test".getBytes(), 4)).get(3000,TimeUnit.MILLISECONDS);
        Thread.sleep(1000);
        Assert.assertEquals(4, recvSize.get());

        this.connector.disconnect();
        try {
            this.connector.send(new DatagramPacket("test".getBytes(), 4)).get(3000,TimeUnit.MILLISECONDS);
            Assert.fail();
        }
        catch (IllegalStateException e) {

        }
        Thread.sleep(1000);
        Assert.assertEquals(4, recvSize.get());

        // reconnect
        this.connector.connect(server.getLocalSocketAddress());
        Assert.assertTrue(this.connector.isConnected());
        this.connector.send(new DatagramPacket("test".getBytes(), 4)).get();
        Thread.sleep(1000);
        Assert.assertEquals(8, recvSize.get());

        server.stop();
    }
}