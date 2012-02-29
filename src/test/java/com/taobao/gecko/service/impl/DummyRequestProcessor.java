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
/**
 * 
 */

package com.taobao.gecko.service.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * 用于单元测试的processor
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-22 下午01:17:13
 */

final class DummyRequestProcessor implements RequestProcessor<NotifyDummyRequestCommand> {

    public AtomicInteger recvCount = new AtomicInteger();

    public long sleepTime = 0;

    public ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, Integer.MAX_VALUE, 60,
        TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());


    public void dispose() {
        this.threadPoolExecutor.shutdown();
    }


    public void handleRequest(final NotifyDummyRequestCommand request, final Connection conn) {
        try {
            this.recvCount.incrementAndGet();
            if (this.sleepTime > 0) {
                Thread.sleep(this.sleepTime);
            }
            conn.response(new NotifyDummyAckCommand(request, null));
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }


    public ThreadPoolExecutor getExecutor() {
        return this.threadPoolExecutor;
    }

}