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
package com.taobao.gecko.core.nio.impl;

import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Ignore;


@Ignore
public class PriorityQueuePerformanceTest {
    public static void main(String[] args) {
        final int count = 10000000;
        final PriorityQueue<TimerRef> queue = new PriorityQueue<TimerRef>();
        final Random rand = new Random();

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            int timeout = rand.nextInt(10000);
            TimerRef e = new TimerRef(timeout, null);
            e.setTimeoutTimestamp(System.currentTimeMillis() + timeout);
            queue.offer(e);
        }

        System.out.println("insert rate:" + count * 1000L / (System.currentTimeMillis() - start));

        int miss = 0;
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            queue.poll();

        }
        System.out.println("poll rate:" + count * 1000L / (System.currentTimeMillis() - start));
        System.out.println(miss);
    }
}