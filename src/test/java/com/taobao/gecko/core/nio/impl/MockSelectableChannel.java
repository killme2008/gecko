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
package com.taobao.gecko.core.nio.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.spi.SelectorProvider;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç01:40:15
 */

public class MockSelectableChannel extends SelectableChannel implements WritableByteChannel, ReadableByteChannel {
    Selector selector;
    int ops;
    Object attch;
    MockSelectionKey selectionKey = new MockSelectionKey();
    int written;
    int writeTimesToReturnZero = 1;
    int writeTimes;
    byte[] readBytes;
    int readTimes;
    int readTimesToReturnZero;


    @Override
    public Object blockingLock() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectableChannel configureBlocking(boolean block) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isBlocking() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isRegistered() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public SelectionKey keyFor(Selector sel) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectorProvider provider() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        this.selector = sel;
        this.ops = ops;
        this.attch = att;
        this.selectionKey.channel = this;
        this.selectionKey.selector = sel;
        return this.selectionKey;
    }


    @Override
    public int validOps() {
        // TODO Auto-generated method stub
        return 0;
    }


    public int write(ByteBuffer src) throws IOException {
        if (writeTimes == writeTimesToReturnZero) {
            return 0;
        }
        src.position(src.position() + this.written);
        writeTimes++;
        return written;
    }


    @Override
    protected void implCloseChannel() throws IOException {
        // TODO Auto-generated method stub

    }


    public int read(ByteBuffer dst) throws IOException {
        if (readTimes == readTimesToReturnZero) {
            return 0;
        }
        readTimes++;
        if (readBytes != null) {
            dst.put(readBytes);
            return readBytes.length;
        }
        return -1;
    }

}