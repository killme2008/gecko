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
 * Shift or算法的匹配实现
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:22:44
 */
public class ShiftOrByteBufferMatcher implements ByteBufferMatcher {

    private int[] b;
    private int lim;

    private final int patternLen;


    public ShiftOrByteBufferMatcher(final IoBuffer pat) {
        if (pat == null || pat.remaining() == 0) {
            throw new IllegalArgumentException("blank buffer");
        }
        this.patternLen = pat.remaining();
        this.preprocess(pat);
    }


    /**
     * 预处理
     * 
     * @param pat
     */
    private void preprocess(final IoBuffer pat) {
        this.b = new int[256];
        this.lim = 0;
        for (int i = 0; i < 256; i++) {
            this.b[i] = ~0;

        }
        for (int i = 0, j = 1; i < this.patternLen; i++, j <<= 1) {
            this.b[ByteBufferUtils.uByte(pat.get(i))] &= ~j;
            this.lim |= j;
        }
        this.lim = ~(this.lim >> 1);

    }


    public final List<Integer> matchAll(final IoBuffer buffer) {
        final List<Integer> matches = new ArrayList<Integer>();
        final int bufferLimit = buffer.limit();
        int state = ~0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            state <<= 1;
            state |= this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if (state < this.lim) {
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
        int state = ~0;
        for (int pos = buffer.position(); pos < bufferLimit; pos++) {
            state = (state <<= 1) | this.b[ByteBufferUtils.uByte(buffer.get(pos))];
            if (state < this.lim) {
                return pos - this.patternLen + 1;
            }
        }
        return -1;
    }

}