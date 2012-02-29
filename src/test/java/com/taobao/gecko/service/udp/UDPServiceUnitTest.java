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
package com.taobao.gecko.service.udp;

import static org.junit.Assert.*;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;

import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.udp.impl.DefaultUDPClient;
import com.taobao.gecko.service.udp.impl.DefaultUDPServer;

@Ignore
public class UDPServiceUnitTest {

    static class MockHandler implements UDPServiceHandler {
        byte[] data;


        public void onMessageReceived(DatagramPacket datagramPacket) {
            System.out.println("udp received...");
            this.data = datagramPacket.getData();
        }

    }


    @Test
    public void testStartStop() throws Exception {
        UDPServer server = new DefaultUDPServer(new MockHandler(), 7903);
        assertTrue(server.isStarted());
        server.stop();
        assertFalse(server.isStarted());

    }


    @Test
    public void testSendProcessMessage() throws Exception {
        final MockHandler serverHandler = new MockHandler();
        UDPServer server = new DefaultUDPServer(serverHandler, 7902);
        UDPClient client = new DefaultUDPClient(serverHandler);

        client.send(new InetSocketAddress(7902), ByteBuffer.wrap("hello".getBytes()));
        Thread.sleep(1000);
        assertEquals("hello", new String(serverHandler.data));

        server.stop();
        client.stop();

    }


    @Test
    public void testReuseSelectorManager() throws Exception {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        RemotingServer remotingServer = RemotingFactory.bind(serverConfig);

        final MockHandler serverHandler = new MockHandler();
        UDPServer server = new DefaultUDPServer(remotingServer, serverHandler, 7901);
        UDPClient client = new DefaultUDPClient(remotingServer, serverHandler);

        client.send(new InetSocketAddress(7901), ByteBuffer.wrap("hello".getBytes()));
        Thread.sleep(1000);
        assertEquals("hello", new String(serverHandler.data));

        server.stop();
        client.stop();
        remotingServer.stop();
    }
}