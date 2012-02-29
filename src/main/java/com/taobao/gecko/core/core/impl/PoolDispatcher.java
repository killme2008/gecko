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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.taobao.gecko.core.core.Dispatcher;
import com.taobao.gecko.core.util.WorkerThreadFactory;


/**
 * 线程池派发器
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:05:09
 */
public class PoolDispatcher implements Dispatcher {
    public static final int POOL_QUEUE_SIZE_FACTOR = 1000;
    public static final float MAX_POOL_SIZE_FACTOR = 1.25f;
    private final ThreadPoolExecutor threadPool;


    public PoolDispatcher(final int poolSize) {
        this(poolSize, 60, TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
    }


    public PoolDispatcher(final int poolSize, final long keepAliveTime, final TimeUnit unit,
            final RejectedExecutionHandler rejectedExecutionHandler) {
        this.threadPool =
                new ThreadPoolExecutor(poolSize, (int) (MAX_POOL_SIZE_FACTOR * poolSize), keepAliveTime, unit,
                    new ArrayBlockingQueue<Runnable>(poolSize * POOL_QUEUE_SIZE_FACTOR), new WorkerThreadFactory());
        this.threadPool.setRejectedExecutionHandler(rejectedExecutionHandler);
    }


    public PoolDispatcher(final int poolSize, final long keepAliveTime, final TimeUnit unit, final String prefix,
            final RejectedExecutionHandler rejectedExecutionHandler) {
        this.threadPool =
                new ThreadPoolExecutor(poolSize, (int) (MAX_POOL_SIZE_FACTOR * poolSize), keepAliveTime, unit,
                    new ArrayBlockingQueue<Runnable>(poolSize * POOL_QUEUE_SIZE_FACTOR),
                    new WorkerThreadFactory(prefix));
        this.threadPool.setRejectedExecutionHandler(rejectedExecutionHandler);
    }


    public final void dispatch(final Runnable r) {
        if (!this.threadPool.isShutdown()) {
            this.threadPool.execute(r);
        }
    }


    public void stop() {
        this.threadPool.shutdown();
        try {
            this.threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}