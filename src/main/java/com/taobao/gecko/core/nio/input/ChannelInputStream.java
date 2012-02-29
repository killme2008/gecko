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
package com.taobao.gecko.core.nio.input;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.taobao.gecko.core.util.ByteBufferUtils;


/**
 * Á÷Ê½API¶Á
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ÏÂÎç06:18:27
 */
public class ChannelInputStream extends InputStream {
    public final ByteBuffer messageBuffer;


    public ChannelInputStream(final ByteBuffer messageBuffer) throws IOException {
        super();
        if (messageBuffer == null) {
            throw new IOException("Null messageBuffer");
        }
        this.messageBuffer = messageBuffer;
    }


    @Override
    public int read() throws IOException {
        if (this.messageBuffer.remaining() == 0) {
            return -1;
        }
        return ByteBufferUtils.uByte(this.messageBuffer.get());
    }


    @Override
    public int available() throws IOException {
        return this.messageBuffer.remaining();
    }


    @Override
    public synchronized void mark(final int readlimit) {
        this.messageBuffer.mark();
    }


    @Override
    public boolean markSupported() {
        return true;
    }


    @Override
    public synchronized void reset() throws IOException {
        this.messageBuffer.reset();
    }


    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Please use Session.close() to close iostream.");
    }

}