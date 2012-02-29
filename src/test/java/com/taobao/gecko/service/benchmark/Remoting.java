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
package com.taobao.gecko.service.benchmark;

import java.net.InetSocketAddress;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.core.impl.StandardSocketOption;
import com.taobao.gecko.core.nio.TCPController;


/**
 * NotifyRemoting的Echo服务器测�? *
 * 
 * @author boyan
 * @Date 2010-8-16
 * 
 */
public class Remoting {
    public static void main(String[] args) throws Exception {
        boolean threadPoolDisabled = args.length > 0 && args[0].equals("nothreadpool");

        Configuration configuration = new Configuration();
        configuration.setSessionReadBufferSize(1024);
        configuration.setStatisticsServer(false);
        configuration.setStatisticsInterval(-1);

        TCPController acceptor = new TCPController(configuration);
        acceptor.setSocketOption(StandardSocketOption.TCP_NODELAY, true);
        acceptor.setSelectorPoolSize(2 * Runtime.getRuntime().availableProcessors() + 1);

        if (!threadPoolDisabled) {
            // Throttling has been disabled because it causes a dead lock.
            // Also, it doesn't have per-channel memory limit.
            acceptor.setDispatchMessageThreadCount(10);
        }

        acceptor.setHandler(new EchoHandler());
        acceptor.bind(new InetSocketAddress(8080));

        System.out.println("Yanf4j EchoServer is ready to serve at port " + 8080 + ".");
        System.out.println("Enter 'ant benchmark' on the client side to begin.");
        System.out.println("Thread pool: " + (threadPoolDisabled ? "DISABLED" : "ENABLED"));
    }

    private static class EchoHandler extends HandlerAdapter {

        EchoHandler() {
            super();
        }


        @Override
        public void onExceptionCaught(Session session, Throwable throwable) {
            session.close();
        }


        @Override
        public void onMessageReceived(Session session, Object message) {
            session.write(((IoBuffer) message).duplicate());
        }

    }

}