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

import java.util.ArrayList;
import java.util.List;

import com.taobao.gecko.core.buffer.IoBuffer;


/**
 * Shift and算法实现的匹配器
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:22:20
 */
public class ShiftAndByteBufferMatcher implements ByteBufferMatcher {

    private int[] b;
    private final int mask;

    private final int patternLimit;
    private final int patternPos;
    private final int patternLen;


    public ShiftAndByteBufferMatcher(final IoBuffer pat) {
        if (pat == null || pat.remaining() == 0) {
            throw new IllegalArgumentException("blank buffer");
        }
        this.patternLimit = pat.limit();
        this.patternPos = pat.position();
        this.patternLen = pat.remaining();
        this.preprocess(pat);
        this.mask = 1 << this.patternLen - 1;
    }


    /**
     * 预处理
     * 
     * @param pat
     */
    private void preprocess(final IoBuffer pat) {
        this.b = new int[256];
        for (int i = this.patternPos; i < this.patternLimit; i++) {
            final int p = ByteBufferUtils.uByte(pat.get(i));
            this.b[p] = this.b[p] | 1 << i;
        }
    }


    public final List<Integer> matchAll(final IoBuffer buffer) {
        final List<Integer> matches = new ArrayList<Integer>();
        final int bufferLimit = buffer.limit();
        int d = 0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            d <<= 1;
            d |= 1;
            d &= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if ((d & this.mask) != 0) {
                matches.add(pos - this.patternLen + 1);
            }
        }
        return matches;
    }


    public final int matchFirst(final IoBuffer buffer) {
        if (buffer == null) {
            return -1;
        }
        final int bufferLimit = buffer.limit();
        int d = 0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            d <<= 1;
            d |= 1;
            d &= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if ((d & this.mask) != 0) {
                return pos - this.patternLen + 1;
            }
        }
        return -1;
    }

}