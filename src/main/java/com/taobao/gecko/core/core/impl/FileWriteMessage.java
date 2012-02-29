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
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.WriteMessage;


/**
 * 写文件消息
 * 
 * @author boyan
 * @Date 2011-4-20
 * 
 */
public class FileWriteMessage implements WriteMessage {
    private long writeOffset;
    private long writeSize;
    private final FutureImpl<Boolean> writeFuture;
    private final FileChannel fileChannel;
    protected volatile boolean writing;
    private final IoBuffer head, tail;


    public FileWriteMessage(final long writeOffset, final long writeSize, final FutureImpl<Boolean> writeFuture,
            final FileChannel fileChannel, final IoBuffer head, final IoBuffer tail) {
        super();
        this.writeOffset = writeOffset;
        this.writeSize = writeSize;
        this.writeFuture = writeFuture;
        this.fileChannel = fileChannel;
        this.head = head;
        this.tail = tail;
    }


    public Object getMessage() {
        return this;
    }


    public FutureImpl<Boolean> getWriteFuture() {
        return this.writeFuture;
    }


    private boolean hasHeadRemaining() {
        return this.head != null && this.head.hasRemaining();
    }


    private boolean hasTailRemaining() {
        return this.tail != null && this.tail.hasRemaining();
    }


    public boolean hasRemaining() {
        return this.hasHeadRemaining() || this.hasFileRemaining() || this.hasTailRemaining();
    }


    private boolean hasFileRemaining() {
        return this.writeSize > 0;
    }


    public final void writing() {
        this.writing = true;
    }


    public final boolean isWriting() {
        return this.writing;
    }


    public long remaining() {
        return (this.head == null ? 0 : this.head.remaining()) + this.writeSize
                + (this.tail == null ? 0 : this.tail.remaining());
    }


    public long write(final WritableByteChannel channel) throws IOException {
        long transfered = 0;
        if (this.hasHeadRemaining()) {
            transfered += channel.write(this.head.buf());
            // 头没有完全写入，直接返回
            if (this.hasHeadRemaining()) {
                return transfered;
            }
        }
        if (this.hasFileRemaining()) {
            final long count = this.transferTo(channel);
            this.writeSize -= count;
            this.writeOffset += count;
            transfered += count;
            // 文件没有传输完毕，直接返回
            if (this.hasFileRemaining()) {
                return transfered;
            }
        }
        if (this.hasTailRemaining()) {
            transfered += channel.write(this.tail.buf());
        }
        return transfered;
    }


    private long transferTo(final WritableByteChannel channel) throws IOException {
        try {
            return this.fileChannel.transferTo(this.writeOffset, this.writeSize, channel);
        }
        catch (final IOException e) {
            // Check to see if the IOException is being thrown due to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
            final String message = e.getMessage();
            if (message != null && message.contains("temporarily unavailable")) {
                return 0;
            }

            throw e;
        }
    }
}