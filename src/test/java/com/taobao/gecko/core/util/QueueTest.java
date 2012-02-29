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

import java.util.NoSuchElementException;
import java.util.Queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class QueueTest {
    private Queue<String> queue;


    @Before
    public void setUp() throws Exception {
        queue = new SimpleQueue<String>();
    }


    @Test
    public void testADD() {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        queue.add("a");
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());

        queue.add("a");
        assertEquals(2, queue.size());
        assertFalse(queue.isEmpty());

        queue.add("b");
        assertEquals(3, queue.size());
        assertFalse(queue.isEmpty());
    }


    @Test
    public void testOffer() {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        queue.offer("a");
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());

        queue.offer("a");
        assertEquals(2, queue.size());
        assertFalse(queue.isEmpty());

        queue.offer("b");
        assertEquals(3, queue.size());
        assertFalse(queue.isEmpty());
    }


    @Test
    public void testPoll() {
        assertNull(queue.poll());
        queue.add("a");
        assertEquals("a", queue.poll());
        assertNull(queue.poll());
        queue.add("a");
        queue.add("b");
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());
        assertNull(queue.poll());
    }


    @Test
    public void testPeek() {
        assertNull(queue.peek());
        queue.add("a");
        assertEquals("a", queue.peek());
        queue.add("b");
        assertEquals("a", queue.peek());
        queue.add("c");
        assertEquals("a", queue.peek());
        queue.poll();
        assertEquals("b", queue.peek());
        queue.poll();
        assertEquals("c", queue.peek());
        queue.poll();
        assertNull(queue.peek());
    }


    @Test
    public void testRemove() {
        try {
            this.queue.remove();
            fail();
        }
        catch (NoSuchElementException e) {

        }
        queue.add("a");
        assertEquals("a", queue.remove());
        try {
            this.queue.remove();
            fail();
        }
        catch (NoSuchElementException e) {

        }
        queue.add("b");
        queue.add("c");
        assertEquals("b", queue.remove());
        assertEquals("c", queue.remove());
        try {
            this.queue.remove();
            fail();
        }
        catch (NoSuchElementException e) {

        }
    }


    @After
    public void tearDown() throws Exception {
        queue.clear();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

}