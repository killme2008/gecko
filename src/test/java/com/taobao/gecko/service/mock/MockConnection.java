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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingContext;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.impl.RequestCallBack;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-23 ÏÂÎç01:37:39
 */

public class MockConnection implements Connection {

    public ByteOrder readBufferOrder() {
        // TODO Auto-generated method stub
        return null;
    }


    public void setWriteInterruptibly(final boolean writeInterruptibly) {
        // TODO Auto-generated method stub

    }


    public Set<String> attributeKeySet() {
        // TODO Auto-generated method stub
        return null;
    }


    public void readBufferOrder(final ByteOrder byteOrder) {
        // TODO Auto-generated method stub

    }

    private final boolean connected;


    public MockConnection(final boolean connected) {
        super();
        this.connected = connected;
    }


    public void transferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel channel, final long position,
            final long size) {
        // TODO Auto-generated method stub

    }


    public void clearAttributes() {
        // TODO Auto-generated method stub

    }


    public void close(final boolean allowReconnect) throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }


    public Object getAttribute(final String key) {
        // TODO Auto-generated method stub
        return null;
    }


    public Future<Boolean> asyncSend(final RequestCommand requestCommand) throws NotifyRemotingException {
        // TODO Auto-generated method stub
        return null;
    }


    public Set<String> getGroupSet() {
        // TODO Auto-generated method stub
        return null;
    }


    public InetSocketAddress getRemoteSocketAddress() {
        // TODO Auto-generated method stub
        return null;
    }


    public InetAddress getLocalAddress() {
        // TODO Auto-generated method stub
        return null;
    }


    public RemotingContext getRemotingContext() {
        // TODO Auto-generated method stub
        return null;
    }


    public RequestCallBack getRequestCallBack(final Integer opaque) {
        // TODO Auto-generated method stub
        return null;
    }


    public ResponseCommand invoke(final RequestCommand requestCommand, final long time, final TimeUnit timeUnit)
            throws InterruptedException, TimeoutException, NotifyRemotingException {
        // TODO Auto-generated method stub
        return null;
    }


    public ResponseCommand invoke(final RequestCommand request) throws InterruptedException, TimeoutException,
            NotifyRemotingException {
        // TODO Auto-generated method stub
        return null;
    }


    public boolean isConnected() {
        return this.connected;
    }


    public void removeAttribute(final String key) {
        // TODO Auto-generated method stub

    }


    public void response(final Object responseCommand) throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }


    public void send(final RequestCommand requestCommand, final SingleRequestCallBackListener listener,
            final long time, final TimeUnit timeUnit) throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }


    public void send(final RequestCommand requestCommand, final SingleRequestCallBackListener listener)
            throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }


    public void send(final RequestCommand requestCommand) throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }


    public void setAttribute(final String key, final Object value) {
        // TODO Auto-generated method stub

    }


    public Object setAttributeIfAbsent(final String key, final Object value) {
        // TODO Auto-generated method stub
        return null;
    }


    public void transferFrom(final IoBuffer head, final IoBuffer tail, final FileChannel channel, final long position,
            final long size, final Integer opaque, final SingleRequestCallBackListener listener, final long time,
            final TimeUnit unit) throws NotifyRemotingException {
        // TODO Auto-generated method stub

    }

}