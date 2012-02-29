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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


public class MultiGroupBenchmarkClient {

    static String body = null;
    static final int BODY_LEN = 128;
    static {
        final StringBuffer sb = new StringBuffer(BODY_LEN);
        for (int i = 0; i < BODY_LEN; i++) {
            sb.append("a");
        }
        body = sb.toString();
    }

    private static final class BenchmarkRequestProcessor implements RequestProcessor<NotifyDummyRequestCommand> {
        private final AtomicInteger counter = new AtomicInteger(0);

        private final ExecutorService executor = Executors.newCachedThreadPool();


        public ThreadPoolExecutor getExecutor() {

            return (ThreadPoolExecutor) this.executor;
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

            }
        }
    }


    public static void main(final String[] args) throws Exception {

        final BenchmarkRequestProcessor processor = new BenchmarkRequestProcessor();
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setCallBackExecutorPoolSize(20);
        // clientConfig.setMaxScheduleWrittenBytes(Runtime.getRuntime().maxMemory()
        // / 8 / 10);
        final RemotingClient client = RemotingFactory.connect(clientConfig);
        client.registerProcessor(NotifyDummyRequestCommand.class, processor);
        client.start();
        client.connect("tcp://localhost:8099", 1);

        client.awaitReadyInterrupt("tcp://localhost:8099");

        // server.stop();
        // client.stop();

    }
}