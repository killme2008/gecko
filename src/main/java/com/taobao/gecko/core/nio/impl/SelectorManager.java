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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.EventType;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.util.PositiveAtomicCounter;


/**
 * Selector管理器，管理多个reactor
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:10:59
 */
public class SelectorManager {
    private final Reactor[] reactorSet;
    private final PositiveAtomicCounter sets = new PositiveAtomicCounter();
    private final NioController controller;
    private final int dividend;

    /**
     * Reactor准备就绪的个数
     */
    private int reactorReadyCount;


    public SelectorManager(final int selectorPoolSize, final NioController controller, final Configuration conf)
            throws IOException {
        if (selectorPoolSize <= 0) {
            throw new IllegalArgumentException("selectorPoolSize<=0");
        }
        log.info("Creating " + selectorPoolSize + " rectors...");
        this.reactorSet = new Reactor[selectorPoolSize];
        this.controller = controller;
        // 创建selectorPoolSize个selector
        for (int i = 0; i < selectorPoolSize; i++) {
            this.reactorSet[i] = new Reactor(this, conf, i);
        }
        this.dividend = this.reactorSet.length - 1;
    }

    private volatile boolean started;


    public int getSelectorCount() {
        return this.reactorSet == null ? 0 : this.reactorSet.length;
    }


    public synchronized void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        for (final Reactor reactor : this.reactorSet) {
            reactor.start();
        }
    }


    /**
     * 仅用于测试
     * 
     * @param index
     * @return
     */
    Reactor getReactorByIndex(final int index) {
        if (index < 0 || index > this.reactorSet.length - 1) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.reactorSet[index];
    }


    public synchronized void stop() {
        if (!this.started) {
            return;
        }
        this.started = false;
        for (final Reactor reactor : this.reactorSet) {
            reactor.interrupt();
        }
    }

    public static final String REACTOR_ATTRIBUTE = System.currentTimeMillis() + "_Reactor_Attribute";


    /**
     * 注册channel
     * 
     * @param channel
     * @param ops
     * @param attachment
     * @return
     */
    public final Reactor registerChannel(final SelectableChannel channel, final int ops, final Object attachment) {
        this.awaitReady();
        int index = 0;
        // Accept单独一个Reactor
        if (ops == SelectionKey.OP_ACCEPT || ops == SelectionKey.OP_CONNECT) {
            index = 0;
        }
        else {
            if (this.dividend > 0) {
                index = this.sets.incrementAndGet() % this.dividend + 1;
            }
            else {
                index = 0;
            }
        }
        final Reactor reactor = this.reactorSet[index];
        reactor.registerChannel(channel, ops, attachment);
        return reactor;

    }


    void awaitReady() {
        synchronized (this) {
            while (!this.started || this.reactorReadyCount != this.reactorSet.length) {
                try {
                    this.wait(1000);
                }
                catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();// reset interrupt status
                }
            }
        }
    }


    /**
     * 查找下一个reactor
     * 
     * @return
     */
    final Reactor nextReactor() {
        if (this.dividend > 0) {
            return this.reactorSet[this.sets.incrementAndGet() % this.dividend + 1];
        }
        else {
            return this.reactorSet[0];
        }
    }


    /**
     * 注册连接事件
     * 
     * @param session
     * @param event
     */
    public final void registerSession(final Session session, final EventType event) {
        // if (session.isClosed() && event != EventType.UNREGISTER) {
        // return;
        // }
        final Reactor reactor = this.getReactorFromSession(session);
        reactor.registerSession(session, event);
    }


    Reactor getReactorFromSession(final Session session) {
        Reactor reactor = (Reactor) session.getAttribute(REACTOR_ATTRIBUTE);

        if (reactor == null) {
            reactor = this.nextReactor();
            final Reactor oldReactor = (Reactor) session.setAttributeIfAbsent(REACTOR_ATTRIBUTE, reactor);
            if (oldReactor != null) {
                reactor = oldReactor;
            }
        }
        return reactor;
    }


    /**
     * 插入定时器到session关联的reactor，返回当前时间
     * 
     * @param session
     * @param timeout
     * @param runnable
     * @return 当前时间
     */
    public final void insertTimer(final Session session, final TimerRef timerRef) {
        final Reactor reactor = this.getReactorFromSession(session);
        reactor.insertTimer(timerRef);
    }


    /**
     * 插入定时器并返回当前时间，随机选择一个reactor
     * 
     * @param timeout
     * @param runnable
     */
    public final void insertTimer(final TimerRef timerRef) {
        this.nextReactor().insertTimer(timerRef);
    }


    public NioController getController() {
        return this.controller;
    }


    synchronized void notifyReady() {
        this.reactorReadyCount++;
        if (this.reactorReadyCount == this.reactorSet.length) {
            this.controller.notifyReady();
            this.notifyAll();
        }

    }

    private static final Log log = LogFactory.getLog(SelectorManager.class);


    public final boolean isStarted() {
        return this.started;
    }
}