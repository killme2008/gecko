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
package com.taobao.gecko.core.nio.output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.nio.impl.NioTCPSession;


/**
 * Á÷Ê½APIÐ´
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ÏÂÎç06:18:42
 */
public class ChannelOutputStream extends OutputStream {
    private final ByteBuffer writeBuffer;
    private final NioTCPSession session;


    public ChannelOutputStream(final NioTCPSession session, final int capacity, final boolean direct) {
        if (direct) {
            this.writeBuffer = ByteBuffer.allocateDirect(capacity <= 0 ? 1024 : capacity);
        }
        else {
            this.writeBuffer = ByteBuffer.allocate(capacity <= 0 ? 1024 : capacity);
        }
        this.session = session;
    }


    @Override
    public void write(final int b) throws IOException {
        this.writeBuffer.put((byte) b);

    }


    @Override
    public void flush() throws IOException {
        this.writeBuffer.flip();
        this.session.write(IoBuffer.wrap(this.writeBuffer));
    }


    public Future<Boolean> asyncFlush() {
        this.writeBuffer.flip();
        return this.session.asyncWrite(IoBuffer.wrap(this.writeBuffer));
    }


    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Please use Session.close() to close iostream.");
    }
}