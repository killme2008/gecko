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
package com.taobao.gecko.core.performance;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.nio.TCPController;
import com.taobao.gecko.core.nio.impl.SelectorManager;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.utils.ConcurrentTestCase;
import com.taobao.gecko.utils.ConcurrentTestTask;


/**
 * ≤‚ ‘timer–‘ƒ‹
 * 
 * @author dennis
 * 
 */
@Ignore
public class TimerPerformanceTest {
    private static final int REPEAT = 100000;
    private static final int THREAD = 50;


    public static void main(final String[] args) throws Exception {
        final Configuration configuration = new Configuration();
        final TCPController controller = new TCPController(configuration);
        final SelectorManager selectorManager = new SelectorManager(2, controller, configuration);
        selectorManager.start();
        final AtomicLong durations = new AtomicLong();
        final CountDownLatch latch = new CountDownLatch(THREAD * REPEAT);
        final Random rand = new Random();
        final ConcurrentTestCase testCase = new ConcurrentTestCase(THREAD, REPEAT, new ConcurrentTestTask() {

            public void run(final int index, final int times) throws Exception {
                final long start = System.currentTimeMillis();
                int time = rand.nextInt(10000);
                while (time == 0) {
                    time = rand.nextInt(10000);
                }

                selectorManager.insertTimer(new TimerRef(time, new Runnable() {

                    public void run() {
                        durations.addAndGet(System.currentTimeMillis() - start);
                        latch.countDown();
                    }
                }));
            }
        });
        testCase.start();
        final long start = System.currentTimeMillis();
        latch.await();
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(testCase.getDurationInMillis());
        System.out.println(durations.get() / THREAD / REPEAT);
        selectorManager.stop();
    }
}