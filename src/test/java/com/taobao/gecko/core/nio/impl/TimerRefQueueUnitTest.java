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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.taobao.gecko.core.nio.impl.TimerRefQueue.TimerQueueVisitor;
import com.taobao.gecko.utils.ConcurrentTestCase;
import com.taobao.gecko.utils.ConcurrentTestTask;


public class TimerRefQueueUnitTest {

    private TimerRefQueue queue;


    @Before
    public void setUp() {
        this.queue = new TimerRefQueue();
    }


    @Test
    public void testAddRemove() {
        assertTrue(this.queue.isEmpty());
        assertEquals(0, this.queue.size());
        final TimerRef timerRef = new TimerRef(1000, null);
        this.queue.add(timerRef);
        assertFalse(this.queue.isEmpty());
        assertEquals(1, this.queue.size());
        assertTrue(this.queue.contains(timerRef));

        this.queue.remove(timerRef);
        assertTrue(this.queue.isEmpty());
        assertEquals(0, this.queue.size());
        assertFalse(this.queue.contains(timerRef));

        final TimerRef timerRef1 = new TimerRef(1000, null);
        this.queue.add(timerRef1);
        final TimerRef timerRef2 = new TimerRef(1000, null);
        this.queue.add(timerRef2);
        assertEquals(2, this.queue.size());
        assertTrue(this.queue.contains(timerRef1));
        assertTrue(this.queue.contains(timerRef2));

        try {
            this.queue.add(timerRef1);
        }
        catch (final IllegalArgumentException e) {
            assertEquals("定时器已经被加入队列", e.getMessage());
        }

        this.queue.remove(timerRef1);
        assertEquals(1, this.queue.size());
        assertFalse(this.queue.contains(timerRef1));
        assertTrue(this.queue.contains(timerRef2));

        this.queue.remove(timerRef2);
        assertEquals(0, this.queue.size());
        assertFalse(this.queue.contains(timerRef1));
        assertFalse(this.queue.contains(timerRef2));
    }


    @Test
    public void testAddRemoveMore() {
        final int count = 1000;
        final List<TimerRef> list = new ArrayList<TimerRef>();
        for (int i = 0; i < count; i++) {
            final TimerRef timer = new TimerRef(1000, null);
            this.queue.add(timer);
            list.add(timer);
        }

        assertEquals(count, this.queue.size());
        for (final TimerRef ref : list) {
            this.queue.remove(ref);
        }
        assertEquals(0, this.queue.size());
        this.queue.iterateQueue(new TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                throw new RuntimeException("没有元素可以迭代");
            }
        });

    }


    @Test
    public void testAddRemoveAddRemove() {
        final int count = 1000;
        final List<TimerRef> list = new ArrayList<TimerRef>();
        for (int i = 0; i < count; i++) {
            final TimerRef timer = new TimerRef(i, null);
            this.queue.add(timer);
            list.add(timer);
        }
        assertEquals(count, this.queue.size());
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                final TimerRef ref = list.get(i);
                this.queue.remove(ref);
            }
        }
        assertEquals(count / 2, this.queue.size());
        this.queue.iterateQueue(new TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                if (timerRef.getTimeout() % 2 == 0) {
                    throw new RuntimeException("没有删除干净偶数");
                }
                return true;
            }
        });
        for (final TimerRef ref : list) {
            this.queue.remove(ref);
        }
        assertEquals(0, this.queue.size());
        this.queue.iterateQueue(new TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                throw new RuntimeException("没有元素可以迭代");
            }
        });

    }


    @Test
    public void testIterate() {
        final int count = 1000;
        final List<TimerRef> list = new ArrayList<TimerRef>();
        for (int i = 0; i < count; i++) {
            final TimerRef timer = new TimerRef(1000, null);
            this.queue.add(timer);
            list.add(timer);
        }
        final AtomicInteger counter = new AtomicInteger();

        this.queue.iterateQueue(new TimerRefQueue.TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                counter.incrementAndGet();
                return true;
            }
        });
        assertEquals(count, counter.get());
    }


    @Test
    public void concurrentTest() throws Exception {
        final AtomicInteger duplicateDeleteCounter = new AtomicInteger(0);
        final ConcurrentTestCase testCase = new ConcurrentTestCase(100, 10000, new ConcurrentTestTask() {

            volatile TimerRef prevRef;


            public void run(final int index, final int times) throws Exception {
                final TimerRef timerRef = new TimerRef(index * times, null);
                this.prevRef = timerRef;
                TimerRefQueueUnitTest.this.queue.add(timerRef);
                if (times % 2 == 0) {
                    if (!TimerRefQueueUnitTest.this.queue.remove(this.prevRef)) {
                        duplicateDeleteCounter.incrementAndGet();
                    }
                }
            }
        });

        testCase.start();
        assertEquals(500000 + duplicateDeleteCounter.get(), this.queue.size());
        assertFalse(this.queue.isEmpty());
        final long tps = 15000000 * 1000 / testCase.getDurationInMillis();
        System.out.println(tps);
    }


    @Test
    public void removeNext() {
        final int count = 1000;
        final List<TimerRef> list = new ArrayList<TimerRef>();
        for (int i = 0; i < count; i++) {
            final TimerRef timer = new TimerRef(i, null);
            this.queue.add(timer);
            list.add(timer);
        }

        this.queue.iterateQueue(new TimerRefQueue.TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                int i = (int) timerRef.getTimeout();
                if (i == count - 1) {
                    i = -1;
                }
                TimerRefQueueUnitTest.this.queue.remove(list.get(i + 1));
                return true;
            }
        });
        assertEquals(0, this.queue.size());
    }

    class IterateThread extends Thread {
        private final CyclicBarrier barrier;
        private boolean end;
        int count;
        long iterateCount;


        public IterateThread(final CyclicBarrier barrier, final int count) {
            super();
            this.barrier = barrier;
            this.count = count;
        }


        @Override
        public void run() {
            try {
                this.barrier.await();
                TimerRefQueueUnitTest.this.queue.iterateQueue(new TimerRefQueue.TimerQueueVisitor() {

                    public boolean visit(final TimerRef timerRef) {
                        IterateThread.this.iterateCount = timerRef.getTimeout();
                        if (timerRef.getTimeout() == IterateThread.this.count - 1) {
                            IterateThread.this.end = true;
                        }
                        return true;
                    }
                });
                this.barrier.await();
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class CancelThread extends Thread {
        final List<TimerRef> list;
        final CyclicBarrier barrier;
        final Random rand;


        public CancelThread(final List<TimerRef> list, final CyclicBarrier barrier, final Random rand) {
            super();
            this.list = list;
            this.barrier = barrier;
            this.rand = rand;
        }


        @Override
        public void run() {
            try {
                this.barrier.await();
                for (int i = 0; i < 1000; i++) {
                    final int index = this.rand.nextInt(this.list.size() - 1);
                    final TimerRef removed = this.list.get(index);
                    removed.cancel();
                }
                this.barrier.await();
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void testCancel() throws Exception {
        // 插入10万个
        final int count = 100000;
        final List<TimerRef> list = new ArrayList<TimerRef>();
        for (int i = 0; i < count; i++) {
            final TimerRef timer = new TimerRef(i, null);
            this.queue.add(timer);
            list.add(timer);
        }
        // 启动101个线程，一个线程迭代，100个线程随机取消10000个TimerRef
        // 确保能迭代到尾部

        final CyclicBarrier barrier = new CyclicBarrier(2001);
        final List<IterateThread> iterateThreads = new ArrayList<IterateThread>();
        for (int i = 0; i < 1000; i++) {
            final IterateThread iterateThread = new IterateThread(barrier, count);
            iterateThreads.add(iterateThread);
            iterateThread.start();
        }

        final Random rand = new SecureRandom();

        for (int i = 0; i < 1000; i++) {
            final Thread thread = new CancelThread(list, barrier, rand);
            thread.start();
        }
        barrier.await();
        barrier.await();
        for (final IterateThread iterateThread : iterateThreads) {
            assertTrue(iterateThread.end);
        }
    }


    @Ignore
    @Test
    public void testPerformance() {
        final int count = 5000000;
        final Random rand = new Random();
        for (int i = 0; i < count; i++) {
            this.queue.add(new TimerRef(rand.nextInt(30000), null));
        }
        assertEquals(count, this.queue.size());
        final long start = System.currentTimeMillis();
        final PriorityQueue<TimerRef> pqueue = new PriorityQueue<TimerRef>();
        this.queue.iterateQueue(new TimerRefQueue.TimerQueueVisitor() {

            public boolean visit(final TimerRef timerRef) {
                if (timerRef.getTimeout() > 15000) {
                    pqueue.offer(timerRef);
                    TimerRefQueueUnitTest.this.queue.remove(timerRef);
                }
                return true;
            }
        });
        System.out.println(pqueue.size());
        System.out.println(this.queue.size());
        System.out.println(System.currentTimeMillis() - start);
    }

}