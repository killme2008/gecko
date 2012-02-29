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
package com.taobao.gecko.core.core.impl;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.WriteMessage;


/**
 * 发送消息包装实现
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:06:01
 */
public class ByteBufferWriteMessage implements WriteMessage {

    protected Object message;

    protected IoBuffer buffer;

    protected FutureImpl<Boolean> writeFuture;

    protected volatile boolean writing;


    public final void writing() {
        this.writing = true;
    }


    public final boolean isWriting() {
        return this.writing;
    }


    public ByteBufferWriteMessage(final Object message, final FutureImpl<Boolean> writeFuture) {
        this.message = message;
        this.writeFuture = writeFuture;
    }


    public long remaining() {
        return this.buffer == null ? 0 : this.buffer.remaining();
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.IWriteMessage#getBuffers()
     */
    public synchronized final IoBuffer getWriteBuffer() {
        return this.buffer;
    }


    public boolean hasRemaining() {
        return this.buffer != null && this.buffer.hasRemaining();
    }


    public long write(final WritableByteChannel channel) throws IOException {
        return channel.write(this.buffer.buf());
    }


    public synchronized final void setWriteBuffer(final IoBuffer buffers) {
        this.buffer = buffers;

    }


    public final FutureImpl<Boolean> getWriteFuture() {
        return this.writeFuture;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.google.code.yanf4j.nio.IWriteMessage#getMessage()
     */
    public final Object getMessage() {
        return this.message;
    }
}