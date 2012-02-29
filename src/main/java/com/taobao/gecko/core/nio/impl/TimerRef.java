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

/**
 * 定时器事件引用
 * 
 * @author dennis
 * 
 */
public class TimerRef implements Comparable<TimerRef> {
    long timeoutTimestamp;
    final long timeout;
    volatile Runnable runnable;
    volatile boolean canceled;
    volatile TimerRefQueue queue;
    TimerRef next;
    TimerRef prev;
    volatile long addTimestamp;


    public TimerRef(Runnable runnable, TimerRefQueue queue, TimerRef next, TimerRef prev) {
        super();
        this.runnable = runnable;
        this.queue = queue;
        this.next = next;
        this.prev = prev;
        this.timeout = -1;
    }


    public TimerRef(long timeout, Runnable runnable) {
        this.timeout = timeout;
        this.runnable = runnable;
    }


    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }


    synchronized long getTimeoutTimestamp() {
        return this.timeoutTimestamp;
    }


    synchronized void setTimeoutTimestamp(long timestamp) {
        if (this.timeoutTimestamp == 0) {
            this.timeoutTimestamp = timestamp;
        }
    }


    public long getTimeout() {
        return this.timeout;
    }


    public Runnable getRunnable() {
        return this.runnable;
    }


    public boolean isCanceled() {
        return this.canceled;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.canceled ? 1231 : 1237);
        result = prime * result + (this.runnable == null ? 0 : this.runnable.hashCode());
        result = prime * result + (int) (this.timeout ^ this.timeout >>> 32);
        result = prime * result + (int) (this.timeoutTimestamp ^ this.timeoutTimestamp >>> 32);
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        TimerRef other = (TimerRef) obj;
        if (this.canceled != other.canceled) {
            return false;
        }
        if (this.runnable == null) {
            if (other.runnable != null) {
                return false;
            }
        }
        else if (!this.runnable.equals(other.runnable)) {
            return false;
        }
        if (this.timeout != other.timeout) {
            return false;
        }
        if (this.timeoutTimestamp != other.timeoutTimestamp) {
            return false;
        }
        return true;
    }


    public int compareTo(TimerRef o) {
        if (o == null) {
            return 1;
        }
        if (this.timeoutTimestamp <= 0) {
            return -1;
        }
        if (this.timeoutTimestamp > o.timeoutTimestamp) {
            return 1;
        }
        else if (this.timeoutTimestamp < o.timeoutTimestamp) {
            return -1;
        }
        else {
            return 0;
        }
    }


    public void cancel() {
        this.canceled = true;
        this.timeoutTimestamp = -1;
        if (this.queue != null) {
            this.queue.remove(this);
        }
    }
}