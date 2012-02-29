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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.MultiGroupCallBackListener;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;


public class MultiGroupBenchmarkServer {
    static final int BODY_LEN = 4096;
    static final long TIMEOUT = 10000;
    static final int REPEAT = 1500;
    static final String GROUP_PREFIX = "group_";
    static String body = null;
    static final int THREAD_COUNT = 10;
    static {
        final StringBuffer sb = new StringBuffer(BODY_LEN);
        for (int i = 0; i < BODY_LEN; i++) {
            sb.append("a");
        }
        body = sb.toString();
    }

    private static final class TestConnectionListener implements ConnectionLifeCycleListener {
        private final AtomicInteger count = new AtomicInteger(-1);


        public void onConnectionClosed(final Connection conn) {
            // TODO Auto-generated method stub

        }


        public void onConnectionCreated(final Connection conn) {
            final String group = GROUP_PREFIX + this.count.incrementAndGet();
            System.out.println("加入分组" + group);
            conn.getRemotingContext().addConnectionToGroup(group, conn);

        }


        public void onConnectionReady(final Connection conn) {
            // TODO Auto-generated method stub

        }

    }

    private static final class AccessThread extends Thread {
        private final CountDownLatch countDownLatch;
        private final RemotingServer server;
        private final AtomicInteger timeoutCounter;
        private final AtomicInteger sendErrorCounter;
        private final Executor executor;


        public AccessThread(final CountDownLatch countDownLatch, final RemotingServer server,
                final AtomicInteger timeoutCounter, final AtomicInteger sendErrorCounter) {
            super();
            this.countDownLatch = countDownLatch;
            this.server = server;
            this.timeoutCounter = timeoutCounter;
            this.sendErrorCounter = sendErrorCounter;
            this.executor = Executors.newCachedThreadPool();
        }


        @Override
        public void run() {
            try {
                final int groupCount = this.server.getRemotingContext().getGroupSet().size() - 1;
                for (int i = 0; i < REPEAT; i++) {
                    try {
                        this.server.sendToGroups(this.getRequestMap(groupCount), new MultiGroupCallBackListener() {

                            public void onResponse(final Map<String, ResponseCommand> groupResponses,
                                    final Object... args) {
                                AccessThread.this.countDownLatch.countDown();
                                if (groupResponses.size() != groupCount) {
                                    throw new RuntimeException("应答数目不匹配");
                                }
                                for (final Map.Entry<String, ResponseCommand> entry : groupResponses.entrySet()) {
                                    final ResponseCommand response = entry.getValue();
                                    if (response.getResponseStatus() == ResponseStatus.TIMEOUT) {
                                        AccessThread.this.timeoutCounter.incrementAndGet();
                                    }
                                    // if (response.getResponseStatus() !=
                                    // ResponseStatus.NO_ERROR) {
                                    // System.out.println("响应错误" +
                                    // response.getResponseStatus());
                                    // }
                                }
                            }


                            public ThreadPoolExecutor getExecutor() {
                                return (ThreadPoolExecutor) AccessThread.this.executor;
                            }
                        }, TIMEOUT, TimeUnit.MILLISECONDS);
                    }
                    catch (final Exception e) {
                        this.sendErrorCounter.incrementAndGet();
                    }
                }

            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }


        public Map<String, RequestCommand> getRequestMap(final int groupCount) {
            final Map<String, RequestCommand> result = new HashMap<String, RequestCommand>();
            for (int i = 0; i < groupCount; i++) {
                result.put(GROUP_PREFIX + i, new NotifyDummyRequestCommand(body));
            }
            return result;
        }

    }


    public static void main(final String[] args) throws Exception {
        final int port = args.length >= 1 ? Integer.parseInt(args[0]) : 8099;
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(port);
        // serverConfig.setMaxScheduleWrittenBytes(Runtime.getRuntime().maxMemory()
        // / 8 / 10);
        // serverConfig.setMaxCallBackCount(10000);
        serverConfig.setSelectorPoolSize(2);
        final RemotingServer server = RemotingFactory.newRemotingServer(serverConfig);

        final AtomicInteger timeoutCounter = new AtomicInteger(0);
        final AtomicInteger sendErrorCounter = new AtomicInteger(0);

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("timeout:" + timeoutCounter.get() + ",send error:" + sendErrorCounter.get());
                    }
                    catch (final Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
        server.addConnectionLifeCycleListener(new TestConnectionListener());

        server.start();

        while (server.getRemotingContext().getGroupSet().size() < 4) {
            Thread.sleep(1000);
        }

        final CountDownLatch latch = new CountDownLatch(REPEAT * THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            new AccessThread(latch, server, timeoutCounter, sendErrorCounter).start();
        }
        final long start = System.currentTimeMillis();
        latch.await();
        final long duration = System.currentTimeMillis() - start;
        final long throughtoutput = REPEAT * THREAD_COUNT * 1000L / duration;
        final int groupCount = server.getRemotingContext().getGroupSet().size() - 1;
        System.out.println(String.format(
            "并发:%d，循环次数:%d,消息大小:%d，分组数%d，耗时:%d ms,Throughtoutput:%d,send Error:%d,timeoutError:%d", THREAD_COUNT,
            REPEAT, BODY_LEN, groupCount, duration, throughtoutput, sendErrorCounter.get(), timeoutCounter.get()));
        System.out.println("测试结束...");

        // server.stop();
        // client.stop();

    }
}