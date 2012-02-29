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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.nio.TCPConnectorController;
import com.taobao.gecko.core.nio.TCPController;


public class SessionTimeoutUnitTest {

    TCPController controller;

    final AtomicBoolean expired = new AtomicBoolean(false);


    @Before
    public void setUp() throws IOException {
        this.controller = new TCPController();
        this.controller.setSessionTimeout(2000);
        this.controller.setHandler(new HandlerAdapter() {

            @Override
            public void onSessionExpired(Session session) {
                System.out.println("Server End,session is expired");
                SessionTimeoutUnitTest.this.expired.set(true);
            }

        });
        this.controller.bind(1997);
    }


    @After
    public void tearDown() throws IOException {
        if (this.controller != null) {
            this.controller.stop();
        }
        this.expired.set(false);
    }


    @Test(timeout = 60 * 1000)
    public void testSessionTimeout() throws Exception {
        TCPConnectorController connector = new TCPConnectorController();
        final AtomicBoolean closed = new AtomicBoolean(false);
        connector.setHandler(new HandlerAdapter() {

            @Override
            public void onSessionClosed(Session session) {
                System.out.println("Client End,session is closed");
                closed.set(true);
            }

        });
        connector.connect(new InetSocketAddress("localhost", 1997));
        connector.awaitConnectUnInterrupt();
        synchronized (this) {
            while (!this.expired.get() || !closed.get()) {
                this.wait(1000);
            }
        }
    }

}