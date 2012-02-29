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
package com.taobao.gecko.core.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.taobao.gecko.core.core.Dispatcher;


public class DispatcherFactoryUnitTest {
    @Test
    public void testNewDispatcher() throws Exception {
        Dispatcher dispatcher = DispatcherFactory.newDispatcher(1, new ThreadPoolExecutor.CallerRunsPolicy());
        Assert.assertNotNull(dispatcher);
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        dispatcher.dispatch(new Runnable() {
            public void run() {
                count.incrementAndGet();
                latch.countDown();
            }
        });
        latch.await();
        Assert.assertEquals(1, count.get());

        Assert.assertNull(DispatcherFactory.newDispatcher(0, new ThreadPoolExecutor.CallerRunsPolicy()));
        Assert.assertNull(DispatcherFactory.newDispatcher(-1, new ThreadPoolExecutor.CallerRunsPolicy()));
        dispatcher.stop();
        try {
            dispatcher.dispatch(new Runnable() {
                public void run() {
                    Assert.fail();
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}