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

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.nio.TCPConnectorController;
import com.taobao.gecko.core.nio.TCPController;


public class SessionIdleUnitTest extends TestCase {
    TCPController controller;

    final AtomicInteger serverIdleCount = new AtomicInteger(0);


    @Override
    public void setUp() throws IOException {
        controller = new TCPController();
        controller.setSessionIdleTimeout(1000);
        controller.setHandler(new HandlerAdapter() {

            @Override
            public void onSessionIdle(Session session) {
                System.out.println("Session is idle");
                serverIdleCount.incrementAndGet();
            }

        });
        controller.bind(1999);
    }


    @Override
    public void tearDown() throws IOException {
        if (this.controller != null) {
            this.controller.stop();
        }
        serverIdleCount.set(0);
    }


    public void testSessionIdle() throws Exception {
        TCPConnectorController connector = new TCPConnectorController();

        connector.setHandler(new HandlerAdapter() {

        });
        connector.connect(new InetSocketAddress("localhost", 1999));
        connector.awaitConnectUnInterrupt();
        synchronized (this) {
            while (serverIdleCount.get() < 5) {
                this.wait(1000);
            }
        }
        connector.stop();
    }

}