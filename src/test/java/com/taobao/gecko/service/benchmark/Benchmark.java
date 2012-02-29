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

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * Service层的一个性能测试
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 下午06:26:00
 */

public class Benchmark {
    static final int BODY_LEN = 4096;
    static final int REPEAT = 10000;
    static final int THREAD_COUNT = 300;
    static final int CONN_COUNT = 1;
    static final long TIMEOUT = 5000;
    static String body = null;
    static {
        final StringBuffer sb = new StringBuffer(BODY_LEN);
        for (int i = 0; i < BODY_LEN; i++) {
            sb.append("a");
        }
        body = sb.toString();
    }

    private static final class BenchmarkRequestProcessor implements RequestProcessor<NotifyDummyRequestCommand> {
        private final AtomicInteger counter = new AtomicInteger(0);


        public ThreadPoolExecutor getExecutor() {

            return null;
        }


        public int getCounter() {
            return this.counter.get();
        }


        public void handleRequest(final NotifyDummyRequestCommand request, final Connection conn) {
            try {
                this.counter.incrementAndGet();
                conn.response(new NotifyDummyAckCommand(request, body));
            }
            catch (final NotifyRemotingException e) {
                e.printStackTrace();
            }
        }
    }

    private static final class AccessThread extends Thread {
        private final CyclicBarrier cyclicBarrier;
        private final RemotingClient client;
        private final String group;
        private final AtomicInteger timeoutCounter;


        public AccessThread(final CyclicBarrier cyclicBarrier, final RemotingClient client,
                final AtomicInteger timeoutCounter, final String group) {
            super();
            this.cyclicBarrier = cyclicBarrier;
            this.client = client;
            this.group = group;
            this.timeoutCounter = timeoutCounter;
        }


        @Override
        public void run() {
            try {
                this.cyclicBarrier.await();
                for (int i = 0; i < REPEAT; i++) {
                    ResponseCommand response = null;
                    try {
                        response =
                                this.client.invokeToGroup(this.group, new NotifyDummyRequestCommand(body), TIMEOUT,
                                    TimeUnit.MILLISECONDS);
                        if (response == null || response.getResponseStatus() != ResponseStatus.NO_ERROR) {
                            throw new IllegalStateException("调用结果非法" + response);
                        }
                    }
                    catch (final TimeoutException e) {
                        this.timeoutCounter.incrementAndGet();
                    }
                }
                this.cyclicBarrier.await();
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(final String[] args) throws Exception {
        final int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8080;
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(port);
        serverConfig.setMaxScheduleWrittenBytes(Long.MAX_VALUE);
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);
        final BenchmarkRequestProcessor processor = new BenchmarkRequestProcessor();
        final AtomicInteger timeoutCounter = new AtomicInteger(0);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("process count:" + processor.getCounter() + ",timeout:"
                                + timeoutCounter.get());
                    }
                    catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
        server.registerProcessor(NotifyDummyRequestCommand.class, processor);
        server.start();
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setCallBackExecutorPoolSize(20);
        clientConfig.setMaxScheduleWrittenBytes(Long.MAX_VALUE);
        final RemotingClient client = RemotingFactory.connect(clientConfig);
        client.start();
        final String group = server.getConnectURI().toString();
        client.connect(group, CONN_COUNT);
        client.awaitReadyInterrupt(group);
        System.out.println("连接数:" + client.getConnectionCount(group));
        System.out.println("开始测试...");

        final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT + 1);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new AccessThread(barrier, client, timeoutCounter, group).start();
        }
        final long start = System.currentTimeMillis();
        barrier.await();
        barrier.await();
        final long duration = System.currentTimeMillis() - start;
        final long throughtoutput = REPEAT * THREAD_COUNT * 1000L / duration;
        System.out.println(String.format("并发:%d，循环次数:%d,消息大小:%d，客户端连接数%d，耗时:%d ms,Throughtoutput:%d", THREAD_COUNT,
            REPEAT, BODY_LEN, CONN_COUNT, duration, throughtoutput));
        System.out.println("测试结束...");

        server.stop();
        client.stop();

    }
}