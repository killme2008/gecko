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

import java.util.concurrent.locks.ReentrantLock;


/**
 * 定时器队列，基于双向链表，所有的比较都基于引用
 * 
 * @author boyan
 * @Date 2010-5-20
 * 
 */
public class TimerRefQueue {
    // 哨兵元素
    private final TimerRef head = new TimerRef(null, this, null, null);
    private int size;
    private final ReentrantLock lock = new ReentrantLock();


    public TimerRefQueue() {
        this.head.prev = this.head.next = this.head;
    }


    public void add(TimerRef timerRef) {
        if (timerRef == null) {
            throw new NullPointerException("Null timer");
        }
        this.lock.lock();
        try {
            if (timerRef.isCanceled()) {
                return;
            }
            if (timerRef.queue != null) {
                throw new IllegalArgumentException("定时器已经被加入队列");
            }
            else {
                timerRef.queue = this;
            }
            timerRef.prev = this.head.prev;
            timerRef.next = this.head;
            this.head.prev.next = timerRef;
            this.head.prev = timerRef;

            this.size++;
        }
        finally {
            this.lock.unlock();
        }
    }


    public boolean remove(TimerRef timerRef) {
        if (timerRef == null) {
            return false;
        }

        this.lock.lock();
        try {

            if (timerRef.queue == null) {
                return false;
            }
            if (timerRef.queue != this) {
                throw new IllegalArgumentException("该定时器不在本队列中" + timerRef.queue);
            }
            timerRef.prev.next = timerRef.next;
            timerRef.next.prev = timerRef.prev;

            timerRef.next = timerRef.prev = null;
            timerRef.queue = null;
            this.size--;
            return true;
        }
        finally {
            this.lock.unlock();
        }

    }

    /**
     * 访问Queue中元素的visitor
     * 
     * @author boyan
     * @Date 2010-5-20
     * 
     */
    public static interface TimerQueueVisitor {
        /**
         * 
         * @param timerRef
         * @return 是否继续访问
         */
        public boolean visit(TimerRef timerRef);
    }


    public int size() {
        this.lock.lock();
        try {
            return this.size;
        }
        finally {
            this.lock.unlock();
        }
    }


    public boolean isEmpty() {
        this.lock.lock();
        try {
            return this.size == 0;
        }
        finally {
            this.lock.unlock();
        }
    }


    public boolean contains(TimerRef ref) {
        this.lock.lock();
        try {
            for (TimerRef timer = this.head.next; timer != this.head; timer = timer.next) {
                if (timer == ref) {
                    return true;
                }
            }
            return false;
        }
        finally {
            this.lock.unlock();
        }
    }


    public void iterateQueue(TimerQueueVisitor visitor) {
        TimerRef[] snapshot = null;
        this.lock.lock();
        try {
            if (this.size > 0) {
                snapshot = new TimerRef[this.size];
                int index = 0;
                for (TimerRef timer = this.head.next; timer != this.head; timer = timer.next) {
                    if (!timer.isCanceled()) {
                        snapshot[index++] = timer;
                    }
                }
            }
        }
        finally {
            this.lock.unlock();
        }
        if (snapshot != null) {
            for (TimerRef timerRef : snapshot) {
                if (timerRef != null) {
                    if (!visitor.visit(timerRef)) {
                        break;
                    }
                }
            }
        }
    }
}