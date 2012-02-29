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
package com.taobao.gecko.service;

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
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 连接的包装，提供更高层次的抽象
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午02:38:20
 */

public interface Connection {

    /**
     * 获取全局上下文
     * 
     * @return
     */
    public RemotingContext getRemotingContext();


    /**
     * 关闭连接
     * 
     * @param allowReconnect
     *            如果true，则允许自动重连
     * @throws NotifyRemotingException
     */
    public void close(boolean allowReconnect) throws NotifyRemotingException;


    /**
     * 连接是否有效
     * 
     * @return
     */
    public boolean isConnected();


    /**
     * 同步调用，指定超时时间
     * 
     * @param requestCommand
     * @param timeConnection
     * @param timeUnit
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public ResponseCommand invoke(final RequestCommand requestCommand, long time, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException, NotifyRemotingException;


    /**
     * 同步调用，默认超时1秒
     * 
     * @param request
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public ResponseCommand invoke(final RequestCommand request) throws InterruptedException, TimeoutException,
            NotifyRemotingException;


    /**
     * 异步发送，指定回调监听器，默认超时1秒，超时将返回一个超时应答给回调监听器
     * 
     * @param requestCommand
     * @param listener
     */
    public void send(final RequestCommand requestCommand, SingleRequestCallBackListener listener)
            throws NotifyRemotingException;


    /**
     * 异步发送，指定回调监听器和超时时间，超时将返回一个超时应答给回调监听器
     * 
     * @param requestCommand
     * @param listener
     */
    public void send(final RequestCommand requestCommand, SingleRequestCallBackListener listener, long time,
            TimeUnit timeUnit) throws NotifyRemotingException;


    /**
     * 异步单向发送
     * 
     * @param requestCommand
     */
    public void send(final RequestCommand requestCommand) throws NotifyRemotingException;


    /**
     * 异步发送，并返回可取消的future
     * 
     * @param requestCommand
     * @return
     * @throws NotifyRemotingException
     */
    public Future<Boolean> asyncSend(final RequestCommand requestCommand) throws NotifyRemotingException;


    /**
     * 单向异步应答
     * 
     * @param responseCommand
     */
    public void response(final Object responseCommand) throws NotifyRemotingException;


    /**
     * 清除连接的所有属性
     */
    public void clearAttributes();


    /**
     * 获取连接上的某个属性
     * 
     * @param key
     * @return
     */
    public Object getAttribute(String key);


    /**
     * 获取远端地址
     * 
     * @return
     */
    public InetSocketAddress getRemoteSocketAddress();


    /**
     * 获取本端IP地址
     * 
     * @return
     */
    public InetAddress getLocalAddress();


    /**
     * 移除属性
     * 
     * @param key
     */
    public void removeAttribute(String key);


    /**
     * 设置属性
     * 
     * @param key
     * @param value
     */
    public void setAttribute(String key, Object value);


    /**
     * 返回属性的key集合
     * 
     * @since 1.8.3
     * @return
     */
    public Set<String> attributeKeySet();


    /**
     * 设置连接的读缓冲区的字节序
     * 
     * @param byteOrder
     */
    public void readBufferOrder(ByteOrder byteOrder);


    /**
     * 获取连接的读缓冲区的字节序
     * 
     * @param byteOrder
     * @return TODO
     */
    public ByteOrder readBufferOrder();


    /**
     * 设置属性，类似ConcurrentHashMap.putIfAbsent方法
     * 
     * @param key
     * @param value
     * @return
     */
    public Object setAttributeIfAbsent(String key, Object value);


    /**
     * 返回该连接所在的分组集合
     * 
     * @return
     */
    public Set<String> getGroupSet();


    /**
     * 是否启用可中断写入操作，如果启用，则允许在用户线程写入socket
     * buffer提高数据的发送效率，但是用户线程的中断可能引起连接断开，需慎重使用。默认不启用。
     * 
     * @param writeInterruptibly
     *            true——启用 false——不启用
     */
    public void setWriteInterruptibly(boolean writeInterruptibly);


    /**
     * 单向传输，无超时
     * 
     * @see #transferFrom(IoBuffer, IoBuffer, FileChannel, long, long, Integer,
     *      SingleRequestCallBackListener, long, TimeUnit)
     * @param head
     * @param tail
     * @param channel
     * @param position
     * @param size
     * @since 1.8.3
     */
    public void transferFrom(IoBuffer head, IoBuffer tail, FileChannel channel, long position, long size);


    /**
     * 从指定FileChannel的position位置开始传输size个字节到socket,
     * remoting会负责保证将指定大小的数据传输给socket。如果file channel里的数据不足size大小，则以实际大小传输。
     * 。其中head和tail是指在传输文件之前或者之后需要写入的数据，可以为null，他们和文件数据作为一个整体来发送。
     * 超过指定的超时时间则取消传输(如果还没有开始传输的话,已经开始的无法中止)，并通知listener。
     * 
     * @param head
     * @param tail
     * @param channel
     * @param position
     * @param size
     * @param opaque
     * @param listener
     * @param time
     * @param unit
     * @since 1.1.0
     */
    public void transferFrom(IoBuffer head, IoBuffer tail, FileChannel channel, long position, long size,
            Integer opaque, SingleRequestCallBackListener listener, long time, TimeUnit unit)
            throws NotifyRemotingException;

}