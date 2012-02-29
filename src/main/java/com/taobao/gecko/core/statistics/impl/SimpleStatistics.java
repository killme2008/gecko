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
package com.taobao.gecko.core.statistics.impl;

import java.util.concurrent.atomic.AtomicLong;

import com.taobao.gecko.core.statistics.Statistics;


/**
 * 简单统计类
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:19:09
 */
public class SimpleStatistics implements Statistics {
    private boolean started = false;


    public boolean isStatistics() {
        return this.started;
    }


    public synchronized void reset() {
        if (this.started) {
            throw new IllegalStateException();
        }
        this.startTime = this.stopTime = -1;
        this.recvMessageCount.set(0);
        this.recvMessageTotalSize.set(0);
        this.writeMessageCount.set(0);
        this.writeMessageTotalSize.set(0);
        this.processMessageCount.set(0);
        this.processMessageTotalTime.set(0);
        this.acceptCount.set(0);
    }

    private double receiveThroughputLimit = -1.0; // receive bytes per second
    private double sendThroughputLimit = -1.0; // send bytes per second


    public void setReceiveThroughputLimit(final double receivePacketRate) {
        this.receiveThroughputLimit = receivePacketRate;
    }


    /**
     * Check session if receive bytes per second is over flow controll
     * 
     * @return
     */
    public boolean isReceiveOverFlow() {
        if (this.getReceiveThroughputLimit() < 0.0000f) {
            return false;
        }
        return this.getReceiveBytesPerSecond() > this.getReceiveThroughputLimit();
    }


    /**
     * Check session if receive bytes per second is over flow controll
     * 
     * @return
     */
    public boolean isSendOverFlow() {
        if (this.getSendThroughputLimit() < 0.0000f) {
            return false;
        }
        return this.getSendBytesPerSecond() > this.getSendThroughputLimit();
    }


    public double getSendThroughputLimit() {
        return this.sendThroughputLimit;
    }


    public void setSendThroughputLimit(final double sendThroughputLimit) {
        this.sendThroughputLimit = sendThroughputLimit;
    }


    public final double getReceiveThroughputLimit() {
        return this.receiveThroughputLimit;
    }


    public synchronized void restart() {
        this.stop();
        this.reset();
        this.start();
    }

    private long startTime, stopTime = -1;

    private final AtomicLong recvMessageCount = new AtomicLong();

    private final AtomicLong recvMessageTotalSize = new AtomicLong();

    private final AtomicLong writeMessageCount = new AtomicLong();

    private final AtomicLong writeMessageTotalSize = new AtomicLong();

    private final AtomicLong processMessageCount = new AtomicLong();

    private final AtomicLong acceptCount = new AtomicLong();

    private final AtomicLong processMessageTotalTime = new AtomicLong();


    public long getStartedTime() {
        return this.startTime;
    }


    public double getProcessedMessageAverageTime() {
        return this.processMessageCount.get() == 0 ? 0 : (double) this.processMessageTotalTime.get()
                / this.processMessageCount.get();
    }


    public long getProcessedMessageCount() {
        return this.processMessageCount.get();
    }


    public void statisticsProcess(final long n) {
        if (!this.started) {
            return;
        }
        if (n < 0) {
            return;
        }
        this.processMessageTotalTime.addAndGet(n);
        this.processMessageCount.incrementAndGet();
    }


    public SimpleStatistics() {

    }


    public synchronized void start() {
        this.startTime = System.currentTimeMillis();
        this.started = true;
    }


    public synchronized void stop() {
        this.stopTime = System.currentTimeMillis();
        this.started = false;
    }


    public void statisticsRead(final long n) {
        if (!this.started) {
            return;
        }
        if (n <= 0) {
            return;
        }
        this.recvMessageCount.incrementAndGet();
        this.recvMessageTotalSize.addAndGet(n);
    }


    public long getRecvMessageCount() {
        return this.recvMessageCount.get();
    }


    public long getRecvMessageTotalSize() {
        return this.recvMessageTotalSize.get();
    }


    public long getWriteMessageCount() {
        return this.writeMessageCount.get();
    }


    public long getWriteMessageTotalSize() {
        return this.writeMessageTotalSize.get();
    }


    public void statisticsWrite(final long n) {
        if (!this.started) {
            return;
        }
        if (n <= 0) {
            return;
        }
        this.writeMessageCount.incrementAndGet();
        this.writeMessageTotalSize.addAndGet(n);
    }


    public long getRecvMessageAverageSize() {
        return this.recvMessageCount.get() == 0 ? 0 : this.recvMessageTotalSize.get() / this.recvMessageCount.get();
    }


    public double getRecvMessageCountPerSecond() {
        final long duration =
                this.stopTime == -1 ? System.currentTimeMillis() - this.startTime : this.stopTime - this.startTime;
        return duration == 0 ? 0 : (double) this.recvMessageCount.get() * 1000 / duration;
    }


    public double getWriteMessageCountPerSecond() {
        final long duration =
                this.stopTime == -1 ? System.currentTimeMillis() - this.startTime : this.stopTime - this.startTime;
        return duration == 0 ? 0 : (double) this.writeMessageCount.get() * 1000 / duration;
    }


    public long getWriteMessageAverageSize() {
        return this.writeMessageCount.get() == 0 ? 0 : this.writeMessageTotalSize.get() / this.writeMessageCount.get();
    }


    public double getAcceptCountPerSecond() {
        final long duration =
                this.stopTime == -1 ? System.currentTimeMillis() - this.startTime : this.stopTime - this.startTime;
        return duration == 0 ? 0 : (double) this.acceptCount.get() * 1000 / duration;
    }


    public double getReceiveBytesPerSecond() {
        final long duration =
                this.stopTime == -1 ? System.currentTimeMillis() - this.startTime : this.stopTime - this.startTime;
        return duration == 0 ? 0 : (double) this.recvMessageTotalSize.get() * 1000 / duration;
    }


    public double getSendBytesPerSecond() {
        final long duration =
                this.stopTime == -1 ? System.currentTimeMillis() - this.startTime : this.stopTime - this.startTime;
        return duration == 0 ? 0 : (double) this.writeMessageTotalSize.get() * 1000 / duration;
    }


    public void statisticsAccept() {
        if (!this.started) {
            return;
        }
        this.acceptCount.incrementAndGet();
    }

}