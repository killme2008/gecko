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
package com.taobao.gecko.service.mock;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory.Decoder;
import com.taobao.gecko.core.core.CodecFactory.Encoder;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.impl.FutureImpl;
import com.taobao.gecko.core.core.impl.HandlerAdapter;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.nio.TCPController;
import com.taobao.gecko.core.nio.impl.SelectorManager;
import com.taobao.gecko.core.nio.impl.TimerRef;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 ÏÂÎç02:27:15
 */

public class MockSession implements NioSession {

    public MockSession() {
        try {
            this.controller = new TCPController();
            this.controller.setHandler(new HandlerAdapter());
            this.controller.start();
            this.selectorManager = this.controller.getSelectorManager();

        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final SelectorManager selectorManager;

    private final TCPController controller;


    public Set<String> attributeKeySet() {
        // TODO Auto-generated method stub
        return null;
    }


    public SelectableChannel channel() {
        // TODO Auto-generated method stub
        return null;
    }


    public Future<Boolean> asyncTransferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel src,
            final long position, final long size) {
        // TODO Auto-generated method stub
        return null;
    }


    public Future<Boolean> transferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel src,
            final long position, final long size) {
        // TODO Auto-generated method stub
        return null;
    }


    public Future<Boolean> asyncWriteInterruptibly(final Object message) {
        // TODO Auto-generated method stub
        return null;
    }


    public void writeInterruptibly(final Object message) {
        // TODO Auto-generated method stub

    }


    public InetAddress getLocalAddress() {
        // TODO Auto-generated method stub
        return null;
    }


    public boolean isIdle() {
        // TODO Auto-generated method stub
        return false;
    }


    public void enableRead(final Selector selector) {
        // TODO Auto-generated method stub

    }


    public void insertTimer(final TimerRef ref) {
        this.selectorManager.insertTimer(ref);

    }


    public void enableWrite(final Selector selector) {
        // TODO Auto-generated method stub

    }


    public void onEvent(final EventType event, final Selector selector) {
        // TODO Auto-generated method stub

    }


    public Future<Boolean> asyncWrite(final Object packet) {
        return new FutureImpl<Boolean>();
    }


    public void clearAttributes() {
        // TODO Auto-generated method stub

    }


    public void close() {
        try {
            this.controller.stop();
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void flush() {
        // TODO Auto-generated method stub

    }


    public Object getAttribute(final String key) {
        // TODO Auto-generated method stub
        return null;
    }


    public Decoder getDecoder() {
        // TODO Auto-generated method stub
        return null;
    }


    public Encoder getEncoder() {
        // TODO Auto-generated method stub
        return null;
    }


    public Handler getHandler() {
        // TODO Auto-generated method stub
        return null;
    }


    public long getLastOperationTimeStamp() {
        // TODO Auto-generated method stub
        return 0;
    }


    public ByteOrder getReadBufferByteOrder() {
        // TODO Auto-generated method stub
        return null;
    }


    public InetSocketAddress getRemoteSocketAddress() {
        // TODO Auto-generated method stub
        return null;
    }


    public long getScheduleWritenBytes() {
        // TODO Auto-generated method stub
        return 0;
    }


    public long getSessionIdleTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }


    public long getSessionTimeout() {
        // TODO Auto-generated method stub
        return 0;
    }


    public boolean isClosed() {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isExpired() {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isHandleReadWriteConcurrently() {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isLoopbackConnection() {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isUseBlockingRead() {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isUseBlockingWrite() {
        // TODO Auto-generated method stub
        return false;
    }


    public void removeAttribute(final String key) {
        // TODO Auto-generated method stub

    }


    public void setAttribute(final String key, final Object value) {
        // TODO Auto-generated method stub

    }


    public Object setAttributeIfAbsent(final String key, final Object value) {
        // TODO Auto-generated method stub
        return null;
    }


    public void setDecoder(final Decoder decoder) {
        // TODO Auto-generated method stub

    }


    public void setEncoder(final Encoder encoder) {
        // TODO Auto-generated method stub

    }


    public void setHandleReadWriteConcurrently(final boolean handleReadWriteConcurrently) {
        // TODO Auto-generated method stub

    }


    public void setReadBufferByteOrder(final ByteOrder readBufferByteOrder) {
        // TODO Auto-generated method stub

    }


    public void setSessionIdleTimeout(final long sessionIdleTimeout) {
        // TODO Auto-generated method stub

    }


    public void setSessionTimeout(final long sessionTimeout) {
        // TODO Auto-generated method stub

    }


    public void setUseBlockingRead(final boolean useBlockingRead) {
        // TODO Auto-generated method stub

    }


    public void setUseBlockingWrite(final boolean useBlockingWrite) {
        // TODO Auto-generated method stub

    }


    public void start() {
        // TODO Auto-generated method stub

    }

    private List<Object> messageList = new ArrayList<Object>();


    public List<Object> getMessageList() {
        return this.messageList;
    }


    public void setMessageList(final List<Object> messageList) {
        this.messageList = messageList;
    }


    public void write(final Object packet) {
        this.messageList.add(packet);

    }

}