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

import java.util.List;

import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;

import static org.junit.Assert.assertEquals;


public abstract class ByteBufferMatcherTest {
    @Test
    public void testMatchFirst() {
        String hello = "hel;lo";
        ByteBufferMatcher m = createByteBufferMatcher(hello);
        assertEquals(0, m.matchFirst(IoBuffer.wrap("hel;lo".getBytes())));
        assertEquals(-1, m.matchFirst(IoBuffer.wrap("hel;l0".getBytes())));
        assertEquals(6, m.matchFirst(IoBuffer.wrap("hello hel;lo".getBytes())));
        assertEquals(0, (m.matchFirst(IoBuffer.wrap("hel;lo good ".getBytes()))));
        assertEquals(7, m.matchFirst(IoBuffer.wrap("abcdefghel;lo good ".getBytes())));
        assertEquals(-1, m.matchFirst(IoBuffer.wrap("".getBytes())));

        assertEquals(6, m.matchFirst(IoBuffer.wrap("hello hel;lo".getBytes()).position(4)));
        assertEquals(6, m.matchFirst(IoBuffer.wrap("hello hel;lo".getBytes()).position(6)));
        assertEquals(-1, m.matchFirst(IoBuffer.wrap("hello hel;lo".getBytes()).limit(6)));

        assertEquals(-1, m.matchFirst(null));
        assertEquals(-1, m.matchFirst(IoBuffer.allocate(0)));
        try {
            new ShiftAndByteBufferMatcher(null);
            assert (false);
        }
        catch (IllegalArgumentException e) {
            assertEquals("blank buffer", e.getMessage());
        }
        try {
            new ShiftAndByteBufferMatcher(IoBuffer.allocate(0));
            assert (false);
        }
        catch (IllegalArgumentException e) {
            assertEquals("blank buffer", e.getMessage());
        }

        ByteBufferMatcher newline = new ShiftAndByteBufferMatcher(IoBuffer.wrap("\r\n".getBytes()));

        String memcachedGet = "VALUE test 0 0 100\r\nhello\r\n";
        assertEquals(memcachedGet.indexOf("\r\n"), newline.matchFirst(IoBuffer.wrap(memcachedGet.getBytes())));
        assertEquals(25, newline.matchFirst(IoBuffer.wrap(memcachedGet.getBytes()).position(20)));
    }


    public abstract ByteBufferMatcher createByteBufferMatcher(String hello);


    @Test
    public void testMatchAll() {
        String memcachedGet = "VALUE test 0 0 100\r\nhello\r\n\rtestgood\r\nh\rfasdfasd\n\rdfasdfad\r\n\r\n";
        ByteBufferMatcher newline = new ShiftOrByteBufferMatcher(IoBuffer.wrap("\r\n".getBytes()));
        List<Integer> list = newline.matchAll(IoBuffer.wrap(memcachedGet.getBytes()));
        for (int i : list) {
            System.out.println(i);
        }

    }
}