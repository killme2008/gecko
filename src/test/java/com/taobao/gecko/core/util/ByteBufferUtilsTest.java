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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.core.config.Configuration;


public class ByteBufferUtilsTest {

    @Test
    public void testIncreaseBlankBufferCapatity() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer = ByteBufferUtils.increaseBufferCapatity(buffer);

        assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE, buffer.capacity());
        buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
        assertEquals(1024 + 2 * Configuration.DEFAULT_INCREASE_BUFF_SIZE, buffer.capacity());

    }


    @Test
    public void testDecreaseBufferCapacity() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(4);
        buffer.putLong(999);

        buffer = ByteBufferUtils.decreaseBufferCapatity(buffer, 512, 64);
        assertEquals(512, buffer.capacity());
        assertEquals(12, buffer.position());
        buffer.flip();
        assertEquals(12, buffer.remaining());
        assertEquals(4, buffer.getInt());
        assertEquals(999, buffer.getLong());
    }


    @Test
    public void testDecreaseBufferCapacity_LessThanMinSize() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(4);
        buffer.putLong(999);

        buffer = ByteBufferUtils.decreaseBufferCapatity(buffer, 512, 768);
        assertEquals(768, buffer.capacity());
        assertEquals(12, buffer.position());
        buffer.flip();
        assertEquals(12, buffer.remaining());
        assertEquals(4, buffer.getInt());
        assertEquals(999, buffer.getLong());
    }


    @Test
    public void testDecreaseBufferCapacity_EaualsMinSize() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(4);
        buffer.putLong(999);
        Assert.assertSame(buffer, ByteBufferUtils.decreaseBufferCapatity(buffer, 512, 1024));
    }


    @Test
    public void testIncreaseNotBlankBufferCapatity() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(100);
        buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
        assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE, buffer.capacity());
        assertEquals(4, buffer.position());
        assertEquals(1024 + Configuration.DEFAULT_INCREASE_BUFF_SIZE - 4, buffer.remaining());
        buffer.putLong(100l);
        assertEquals(12, buffer.position());
        buffer = ByteBufferUtils.increaseBufferCapatity(buffer);
        assertEquals(12, buffer.position());
        assertEquals(1024 + 2 * Configuration.DEFAULT_INCREASE_BUFF_SIZE - 4 - 8, buffer.remaining());

    }


    @Test
    public void testIncreaseNullBufferCapacity() {
        try {
            assertNull(ByteBufferUtils.increaseBufferCapatity(null));
        }
        catch (final IllegalArgumentException e) {
            assertEquals("buffer is null", e.getMessage());
        }
    }


    public void testFlip() {
        final ByteBuffer[] buffers = new ByteBuffer[2];
        ByteBufferUtils.flip(buffers);
        buffers[0] = ByteBuffer.allocate(4).putInt(4);
        buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());

        assertEquals(4, buffers[0].position());
        assertEquals(5, buffers[1].position());
        assertEquals(4, buffers[0].limit());
        assertEquals(10, buffers[1].limit());
        ByteBufferUtils.flip(buffers);
        assertEquals(0, buffers[0].position());
        assertEquals(0, buffers[1].position());
        assertEquals(4, buffers[0].limit());
        assertEquals(5, buffers[1].limit());

        ByteBufferUtils.flip(null);
    }


    public void testClear() {
        final ByteBuffer[] buffers = new ByteBuffer[2];
        ByteBufferUtils.clear(buffers);
        buffers[0] = ByteBuffer.allocate(4).putInt(4);
        buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());

        assertEquals(4, buffers[0].position());
        assertEquals(5, buffers[1].position());
        assertEquals(4, buffers[0].limit());
        assertEquals(10, buffers[1].limit());
        assertEquals(0, buffers[0].remaining());
        assertEquals(5, buffers[1].remaining());
        ByteBufferUtils.clear(buffers);
        assertEquals(0, buffers[0].position());
        assertEquals(0, buffers[1].position());
        assertEquals(4, buffers[0].limit());
        assertEquals(10, buffers[1].limit());
        assertEquals(4, buffers[0].remaining());
        assertEquals(10, buffers[1].remaining());
        ByteBufferUtils.clear(null);
    }


    public void testHasRemaining() {
        final ByteBuffer[] buffers = new ByteBuffer[2];

        assertFalse(ByteBufferUtils.hasRemaining(buffers));
        buffers[0] = ByteBuffer.allocate(4).putInt(4);
        buffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());
        assertTrue(ByteBufferUtils.hasRemaining(buffers));

        buffers[1].put("yanfj".getBytes());
        assertFalse(ByteBufferUtils.hasRemaining(buffers));

        ByteBufferUtils.clear(buffers);
        assertTrue(ByteBufferUtils.hasRemaining(buffers));

        final ByteBuffer[] moreBuffers = new ByteBuffer[3];
        moreBuffers[0] = ByteBuffer.allocate(4).putInt(4);
        moreBuffers[1] = ByteBuffer.allocate(10).put("hello".getBytes());
        moreBuffers[2] = ByteBuffer.allocate(12).putLong(9999);
        assertTrue(ByteBufferUtils.hasRemaining(moreBuffers));
        moreBuffers[2].putInt(4);
        assertTrue(ByteBufferUtils.hasRemaining(moreBuffers));
        moreBuffers[1].put("yanfj".getBytes());
        assertFalse(ByteBufferUtils.hasRemaining(moreBuffers));

        assertFalse(ByteBufferUtils.hasRemaining(null));
    }


    public void testIndexOf() {
        final String words = "hello world good hello";
        final ByteBuffer buffer = ByteBuffer.wrap(words.getBytes());

        final String world = "world";
        assertEquals(6, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap(world.getBytes())));
        assertEquals(0, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap("hello".getBytes())));
        final long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            assertEquals(17, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap("hello".getBytes()), 6));
        }
        System.out.println(System.currentTimeMillis() - start);
        assertEquals(-1, ByteBufferUtils.indexOf(buffer, ByteBuffer.wrap("test".getBytes())));
        assertEquals(-1, ByteBufferUtils.indexOf(buffer, (ByteBuffer) null));
        assertEquals(-1, ByteBufferUtils.indexOf(null, buffer));
    }


    public void testGather() {
        final ByteBuffer buffer1 = ByteBuffer.wrap("hello".getBytes());
        final ByteBuffer buffer2 = ByteBuffer.wrap(" dennis".getBytes());

        final ByteBuffer gather = ByteBufferUtils.gather(new ByteBuffer[] { buffer1, buffer2 });

        assertEquals("hello dennis", new String(gather.array()));

        assertNull(ByteBufferUtils.gather(null));
        assertNull(ByteBufferUtils.gather(new ByteBuffer[] {}));
    }

}