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
package com.taobao.gecko.service.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.core.util.MBeanUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingContext;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.BaseConfig;


/**
 * 
 * 通讯层的全局上下文
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午02:46:34
 */

public class DefaultRemotingContext implements RemotingContext, DefaultRemotingContextMBean {
    private final GroupManager groupManager;
    private final ConcurrentHashMap<Object, Object> attributes = new ConcurrentHashMap<Object, Object>();
    private final BaseConfig config;
    private final CommandFactory commandFactory;
    private final Semaphore callBackSemaphore;
    static final Log log = LogFactory.getLog(DefaultRemotingContext.class);
    /**
     * Session到connection的映射关系
     */
    protected final ConcurrentHashMap<NioSession, DefaultConnection> session2ConnectionMap =
            new ConcurrentHashMap<NioSession, DefaultConnection>();
    protected ConcurrentHashMap<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>> processorMap =
            new ConcurrentHashMap<Class<? extends RequestCommand>, RequestProcessor<? extends RequestCommand>>();

    protected final CopyOnWriteArrayList<ConnectionLifeCycleListener> connectionLifeCycleListenerList =
            new CopyOnWriteArrayList<ConnectionLifeCycleListener>();


    public DefaultRemotingContext(final BaseConfig config, final CommandFactory commandFactory) {
        this.groupManager = new GroupManager();
        this.config = config;
        if (commandFactory == null) {
            throw new IllegalArgumentException("CommandFactory不能为空");
        }
        this.commandFactory = commandFactory;
        this.callBackSemaphore = new Semaphore(this.config.getMaxCallBackCount());
        MBeanUtils.registerMBeanWithIdPrefix(this, null);
    }


    @Override
    public int getCallBackCountAvailablePermits() {
        return this.callBackSemaphore.availablePermits();
    }


    @Override
    public CommandFactory getCommandFactory() {
        return this.commandFactory;
    }


    @Override
    public BaseConfig getConfig() {
        return this.config;
    }


    /**
     * 请求允许加入callBack，做callBack总数限制
     * 
     * @return
     */
    boolean aquire() {
        return this.callBackSemaphore.tryAcquire();
    }


    /**
     * 在应答到达时释放许可
     */
    void release() {
        this.callBackSemaphore.release();
    }


    void release(final int n) {
        this.callBackSemaphore.release(n);
    }


    void notifyConnectionCreated(final Connection conn) {
        for (final ConnectionLifeCycleListener listener : this.connectionLifeCycleListenerList) {
            try {
                listener.onConnectionCreated(conn);
            }
            catch (final Throwable t) {
                log.error("NotifyRemoting-调用ConnectionLifeCycleListener.onConnectionCreated出错", t);
            }
        }
    }


    void notifyConnectionClosed(final Connection conn) {
        for (final ConnectionLifeCycleListener listener : this.connectionLifeCycleListenerList) {
            try {
                listener.onConnectionClosed(conn);
            }
            catch (final Throwable t) {
                log.error("NotifyRemoting-调用ConnectionLifeCycleListener.onConnectionClosed出错", t);
            }
        }
    }


    void addSession2ConnectionMapping(final NioSession session, final DefaultConnection conn) {
        this.session2ConnectionMap.put(session, conn);
    }


    DefaultConnection getConnectionBySession(final NioSession session) {
        return this.session2ConnectionMap.get(session);
    }


    DefaultConnection removeSession2ConnectionMapping(final NioSession session) {
        return this.session2ConnectionMap.remove(session);
    }


    @Override
    public Set<String> getGroupSet() {
        return this.groupManager.getGroupSet();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#addConnectionToGroup
     * (java.lang.String, com.taobao.gecko.service.Connection)
     */
    @Override
    public boolean addConnectionToGroup(final String group, final Connection connection) {
        return this.groupManager.addConnection(group, connection);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#addConnection
     * (com.taobao.gecko.service.Connection)
     */
    @Override
    public void addConnection(final Connection connection) {
        this.groupManager.addConnection(Constants.DEFAULT_GROUP, connection);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#removeConnection
     * (com.taobao.gecko.service.Connection)
     */
    @Override
    public void removeConnection(final Connection connection) {
        this.groupManager.removeConnection(Constants.DEFAULT_GROUP, connection);
    }


    /*
     * (non-Javadoc)
     * 
     * @seecom.taobao.notify.remoting.service.impl.RemotingContext#
     * getConnectionSetByGroup(java.lang.String)
     */
    @Override
    public List<Connection> getConnectionsByGroup(final String group) {
        return this.groupManager.getConnectionsByGroup(group);
    }


    public void awaitGroupConnectionsEmpty(String group, long time) throws InterruptedException, TimeoutException {
        this.groupManager.awaitGroupConnectionsEmpty(group, time);
    }


    /*
     * (non-Javadoc)
     * 
     * @seecom.taobao.notify.remoting.service.impl.RemotingContext#
     * removeConnectionFromGroup(java.lang.String,
     * com.taobao.gecko.service.Connection)
     */
    @Override
    public boolean removeConnectionFromGroup(final String group, final Connection connection) {
        return this.groupManager.removeConnection(group, connection);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#getAttribute(
     * java.lang.Object, java.lang.Object)
     */
    @Override
    public Object getAttribute(final Object key, final Object defaultValue) {
        final Object value = this.attributes.get(key);
        return value == null ? defaultValue : value;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#getAttribute(
     * java.lang.Object)
     */
    @Override
    public Object getAttribute(final Object key) {
        return this.attributes.get(key);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#getAttributeKeys
     * ()
     */
    @Override
    public Set<Object> getAttributeKeys() {
        return this.attributes.keySet();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#setAttribute(
     * java.lang.Object, java.lang.Object)
     */
    @Override
    public Object setAttribute(final Object key, final Object value) {
        return this.attributes.put(key, value);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#setAttribute(
     * java.lang.Object)
     */
    public Object setAttribute(final Object key) {
        return this.attributes.put(key, null);
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.taobao.gecko.service.impl.RemotingContext#dispose()
     */
    public void dispose() {
        this.groupManager.clear();
        this.attributes.clear();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#setAttributeIfAbsent
     * (java.lang.Object, java.lang.Object)
     */
    @Override
    public Object setAttributeIfAbsent(final Object key, final Object value) {
        return this.attributes.putIfAbsent(key, value);
    }


    public Object removeAttribute(final Object key) {
        return this.attributes.remove(key);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RemotingContext#setAttributeIfAbsent
     * (java.lang.Object)
     */
    @Override
    public Object setAttributeIfAbsent(final Object key) {
        return this.attributes.putIfAbsent(key, null);
    }

}