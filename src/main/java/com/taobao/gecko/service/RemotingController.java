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

import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.nio.impl.TimerRef;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * Notify Remoting服务基础接口
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午02:15:02
 */

public interface RemotingController {

    /**
     * 设置连接选择器，默认为随机选择器
     * 
     * @param selector
     */
    public void setConnectionSelector(ConnectionSelector selector);


    /**
     * 启动Remoting控制器
     * 
     * @throws NotifyRemotingException
     * 
     */
    public void start() throws NotifyRemotingException;


    /**
     * 关闭Remoting控制器
     * 
     * @throws NotifyRemotingException
     * 
     */
    public void stop() throws NotifyRemotingException;


    /**
     * 判断通讯组件是否启动
     * 
     * @return
     */
    public boolean isStarted();


    /**
     * 注册请求处理器
     * 
     * @param <T>
     * @param commandClazz
     * @param processor
     */
    public <T extends RequestCommand> void registerProcessor(Class<T> commandClazz, RequestProcessor<T> processor);


    /**
     * 获取command对应的处理器
     * 
     * @param clazz
     * @return
     */
    public RequestProcessor<? extends RequestCommand> getProcessor(Class<? extends RequestCommand> clazz);


    /**
     * 取消处理器的注册,返回被取消的处理器
     * 
     * @param clazz
     * @return
     */
    public RequestProcessor<? extends RequestCommand> unreigsterProcessor(Class<? extends RequestCommand> clazz);


    /**
     * 批量添加请求处理器
     * 
     * @param <T>
     * @param map
     */
    public void addAllProcessors(Map<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>> map);


    /**
     * 添加一个定时器
     * 
     * @param timeout
     *            超时的时间
     * @param timeUnit
     *            时间单位
     * @param runnable
     *            超时执行的任务
     */
    public void insertTimer(TimerRef timerRef);


    /**
     * 异步发送消息给多个分组，每个分组根据策略选一个连接发送，指定回调处理器和超时时间，超时将返回一个超时应答给回调监听器
     * 
     * @param groupObjects
     *            group->message
     * @param listener
     *            应答处理器
     * @param timeout
     *            超时时间
     * @param timeUnit
     *            时间单位
     * @param args
     *            附件
     */
    public void sendToGroups(Map<String, RequestCommand> groupObjects, MultiGroupCallBackListener listener,
            long timeout, TimeUnit timeUnit, Object... args) throws NotifyRemotingException;


    /**
     * 异步单向发送消息给多个分组
     * 
     * @param groupObjects
     */
    public void sendToGroups(Map<String, RequestCommand> groupObjects) throws NotifyRemotingException;


    /**
     * 异步单向发送给所有连接
     * 
     * @param command
     */
    public void sendToAllConnections(RequestCommand command) throws NotifyRemotingException;


    /**
     * 异步单向发送给指定分组中的一个连接，默认是随机策略
     * 
     * @param group
     * @param command
     */
    public void sendToGroup(String group, RequestCommand command) throws NotifyRemotingException;


    /**
     * 从指定FileChannel的position位置开始传输size个字节到指定group的一个socket,
     * remoting会负责保证将指定大小的数据传输给socket。如果file channel里的数据不足size大小，则以实际大小传输。
     * 其中head和tail是指在传输文件之前或者之后需要写入的数据，可以为null，他们和文件数据作为一个整体来发送。
     * 超过指定的超时时间则取消传输(如果还没有开始传输的话,已经开始的无法中止)，并通知listener。
     * 
     * @param group
     * @param head
     * @param tail
     * @param channel
     * @param position
     * @param size
     * @param opaque
     * @param listener
     * @param time
     * @param unit
     * @throws NotifyRemotingException
     */
    public void transferToGroup(String group, IoBuffer head, IoBuffer tail, FileChannel channel, long position,
            long size, Integer opaque, SingleRequestCallBackListener listener, long time, TimeUnit unit)
            throws NotifyRemotingException;


    /**
     * 单向传输数据到指定group的某个socket连接，传输需要使用的时间未知，也不可取消
     * 
     * @see #transferToGroup(String, IoBuffer, IoBuffer, FileChannel, long,
     *      long, Integer, SingleRequestCallBackListener, long, TimeUnit)
     * @param group
     * @param head
     * @param tail
     * @param channel
     * @param position
     * @param size
     */
    public void transferToGroup(String group, IoBuffer head, IoBuffer tail, FileChannel channel, long position,
            long size) throws NotifyRemotingException;


    /**
     * 异步单向发送给指定分组的所有连接
     * 
     * @param group
     * @param command
     */
    public void sendToGroupAllConnections(String group, RequestCommand command) throws NotifyRemotingException;


    /**
     * 异步发送给指定分组中的一个连接，指定回调监听器RequestCallBackListener，默认策略是随机，默认超时为1秒,
     * 超过超时时间将返回一个超时应答给回调监听器
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @param listener
     *            响应处理器
     */
    public void sendToGroup(String group, RequestCommand command, SingleRequestCallBackListener listener)
            throws NotifyRemotingException;


    /**
     * 异步发送给指定分组中的一个连接，默认策略是随机，指定超时,超过超时时间将返回一个超时应答给回调监听器
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @param listener
     *            响应处理器
     */
    public void sendToGroup(String group, RequestCommand command, SingleRequestCallBackListener listener, long time,
            TimeUnit timeunut) throws NotifyRemotingException;


    /**
     * 同步调用分组中的一个连接，默认超时1秒
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public ResponseCommand invokeToGroup(String group, RequestCommand command) throws InterruptedException,
            TimeoutException, NotifyRemotingException;


    /**
     * 同步调用分组中的一个连接，指定超时时间
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @param time
     *            超时时间
     * @param timeUnit
     *            时间单位
     * @return
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public ResponseCommand invokeToGroup(String group, RequestCommand command, long time, TimeUnit timeUnit)
            throws InterruptedException, TimeoutException, NotifyRemotingException;


    /**
     * 异步发送给指定分组的所有连接，默认超时1秒,超过超时时间将返回一个超时应答给回调监听器
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @param listener
     *            响应处理器
     */
    public void sendToGroupAllConnections(String group, RequestCommand command,
            GroupAllConnectionCallBackListener listener) throws NotifyRemotingException;


    /**
     * 同步调用分组内的所有连接，
     * 超时响应的连接将放入一个BooleanResponseCommand作为结果并且设置responseStatus为TIMEOUT
     * ,如果分组内没有连接将返回null
     * 
     * @param group
     * @param command
     * @return
     * @throws InterruptedException
     * @throws NotifyRemotingException
     */
    public Map<Connection, ResponseCommand> invokeToGroupAllConnections(String group, RequestCommand command)
            throws InterruptedException, NotifyRemotingException;


    /**
     * 同步调用分组内的所有连接，
     * 超时响应的连接将放入一个BooleanResponseCommand作为结果并且设置responseStatus为TIMEOUT
     * ,如果分组内没有连接将返回null
     * 
     * @param group
     * @param command
     * @return
     * @throws InterruptedException
     * @throws NotifyRemotingException
     */
    public Map<Connection, ResponseCommand> invokeToGroupAllConnections(String group, RequestCommand command,
            long time, TimeUnit timeUnit) throws InterruptedException, NotifyRemotingException;


    /**
     * 异步发送给指定分组的所有连接，指定超时时间，超过超时时间将返回一个超时应答给回调监听器
     * 
     * @param group
     *            分组名称
     * @param command
     *            请求命令
     * @param listener
     *            响应处理器
     */
    public void sendToGroupAllConnections(String group, RequestCommand command,
            GroupAllConnectionCallBackListener listener, long time, TimeUnit timeUnit) throws NotifyRemotingException;


    /**
     * 获取group对应的连接数
     * 
     * @param group
     * @return
     */
    public int getConnectionCount(String group);


    /**
     * 获取group集合
     * 
     * @return
     */
    public Set<String> getGroupSet();


    /**
     * 设置属性
     * 
     * @param group
     * 
     * @param key
     * @param value
     */
    public void setAttribute(String group, String key, Object value);


    /**
     * 设置属性，类似ConcurrentHashMap.putIfAbsent
     * 
     * @param group
     * 
     * @param key
     * @param value
     * @return
     */
    public Object setAttributeIfAbsent(String group, String key, Object value);


    /**
     * 获取属性
     * 
     * @param group
     * 
     * @param key
     * @return
     */
    public Object getAttribute(String group, String key);


    /**
     * 添加连接生命周期监听器
     * 
     * @param connectionLifeCycleListener
     */
    public void addConnectionLifeCycleListener(ConnectionLifeCycleListener connectionLifeCycleListener);


    /**
     * 添加连接生命周期监听器
     * 
     * @param connectionLifeCycleListener
     */
    public void removeConnectionLifeCycleListener(ConnectionLifeCycleListener connectionLifeCycleListener);


    /**
     * 移除属性
     * 
     * @param group
     * 
     * @param key
     * @return
     */
    public Object removeAttribute(String group, String key);


    /**
     * 获取全局上下文
     * 
     * @return
     */
    public RemotingContext getRemotingContext();


    /**
     * 根据策略从分组中的连接选择一个
     * 
     * @param group
     * @param connectionSelector
     *            连接选择器
     * @param request
     *            发送的命令
     * @return
     */
    public Connection selectConnectionForGroup(String group, ConnectionSelector connectionSelector,
            RequestCommand request) throws NotifyRemotingException;

}