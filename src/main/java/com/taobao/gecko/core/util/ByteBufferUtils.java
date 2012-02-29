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

/*
 * Copyright 2004-2005 the original author or authors.
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
 * 鏉ヨ嚜浜巆indy2.4鐨勫伐鍏风被锛屽仛浜嗙畝鍖栧拰鏂板
 */
import java.nio.ByteBuffer;

import com.taobao.gecko.core.config.Configuration;


public class ByteBufferUtils {
    /**
     * 
     * @param byteBuffer
     * @return *
     */
    public static final ByteBuffer increaseBufferCapatity(final ByteBuffer byteBuffer) {
        return increaseBufferCapatity(byteBuffer, Configuration.DEFAULT_INCREASE_BUFF_SIZE);
    }


    /**
     * 
     * @param byteBuffer
     * @param size
     *            递减的幅度
     * @param minSize
     *            最小大小
     * @return
     */
    public static final ByteBuffer decreaseBufferCapatity(final ByteBuffer byteBuffer, final int size, final int minSize) {
        if (byteBuffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }

        if (minSize <= 0) {
            throw new IllegalArgumentException("minSize must be great than zero");
        }

        if (byteBuffer.capacity() == minSize) {
            return byteBuffer;
        }

        int capacity = byteBuffer.capacity() - size;
        // 不许小于最小大小
        if (capacity < minSize) {
            capacity = minSize;
        }
        // 理论上不会有这种情况，新的缓冲区不够放入原有的数据，则直接返回原始buffer
        if (capacity < byteBuffer.position()) {
            return byteBuffer;
        }
        final ByteBuffer result =
                byteBuffer.isDirect() ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
        result.order(byteBuffer.order());
        byteBuffer.flip();
        result.put(byteBuffer);
        return result;
    }


    /**
     * 
     * @param byteBuffer
     * @return *
     */
    public static final ByteBuffer increaseBufferCapatity(final ByteBuffer byteBuffer, final int increaseSize) {
        if (byteBuffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }

        if (increaseSize <= 0) {
            throw new IllegalArgumentException("increaseSize<=0");
        }

        final int capacity = byteBuffer.capacity() + increaseSize;
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity can't be negative");
        }
        final ByteBuffer result =
                byteBuffer.isDirect() ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
        result.order(byteBuffer.order());
        byteBuffer.flip();
        result.put(byteBuffer);
        return result;
    }


    public static final void flip(final ByteBuffer[] buffers) {
        if (buffers == null) {
            return;
        }
        for (final ByteBuffer buffer : buffers) {
            if (buffer != null) {
                buffer.flip();
            }
        }
    }


    public static final ByteBuffer gather(final ByteBuffer[] buffers) {
        if (buffers == null || buffers.length == 0) {
            return null;
        }
        final ByteBuffer result = ByteBuffer.allocate(remaining(buffers));
        result.order(buffers[0].order());
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null) {
                result.put(buffers[i]);
            }
        }
        result.flip();
        return result;
    }


    public static final int remaining(final ByteBuffer[] buffers) {
        if (buffers == null) {
            return 0;
        }
        int remaining = 0;
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null) {
                remaining += buffers[i].remaining();
            }
        }
        return remaining;
    }


    public static final void clear(final ByteBuffer[] buffers) {
        if (buffers == null) {
            return;
        }
        for (final ByteBuffer buffer : buffers) {
            if (buffer != null) {
                buffer.clear();
            }
        }
    }


    public static final String toHex(final byte b) {
        return "" + "0123456789ABCDEF".charAt(0xf & b >> 4) + "0123456789ABCDEF".charAt(b & 0xf);
    }


    public static final int indexOf(final ByteBuffer buffer, final ByteBuffer pattern) {
        if (pattern == null || buffer == null) {
            return -1;
        }
        final int n = buffer.remaining();
        final int m = pattern.remaining();
        final int patternPos = pattern.position();
        final int bufferPos = buffer.position();
        if (n < m) {
            return -1;
        }
        for (int s = 0; s <= n - m; s++) {
            boolean match = true;
            for (int i = 0; i < m; i++) {
                if (buffer.get(s + i + bufferPos) != pattern.get(patternPos + i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return bufferPos + s;
            }
        }
        return -1;
    }


    public static final int indexOf(final ByteBuffer buffer, final ByteBuffer pattern, final int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be greater than 0");
        }
        if (pattern == null || buffer == null) {
            return -1;
        }
        final int patternPos = pattern.position();
        final int n = buffer.remaining();
        final int m = pattern.remaining();
        if (n < m) {
            return -1;
        }
        if (offset < buffer.position() || offset > buffer.limit()) {
            return -1;
        }
        for (int s = 0; s <= n - m; s++) {
            boolean match = true;
            for (int i = 0; i < m; i++) {
                if (buffer.get(s + i + offset) != pattern.get(patternPos + i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return offset + s;
            }
        }
        return -1;
    }


    /**
     * 鏌ョ湅ByteBuffer鏁扮粍鏄惁杩樻湁鍓╀綑
     * 
     * @param buffers
     *            ByteBuffers
     * @return have remaining
     */
    public static final boolean hasRemaining(final ByteBuffer[] buffers) {
        if (buffers == null) {
            return false;
        }
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null && buffers[i].hasRemaining()) {
                return true;
            }
        }
        return false;
    }


    public static final int uByte(final byte b) {
        return b & 0xFF;
    }
}