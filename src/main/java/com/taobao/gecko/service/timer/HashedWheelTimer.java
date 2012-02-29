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
/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.taobao.gecko.service.timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * A {@link Timer} optimized for approximated I/O timeout scheduling.
 * 
 * <h3>Tick Duration</h3>
 * 
 * As described with 'approximated', this timer does not execute the scheduled
 * {@link TimerTask} on time. {@link HashedWheelTimer}, on every tick, will
 * check if there are any {@link TimerTask}s behind the schedule and execute
 * them.
 * <p>
 * You can increase or decrease the accuracy of the execution timing by
 * specifying smaller or larger tick duration in the constructor. In most
 * network applications, I/O timeout does not need to be accurate. Therefore,
 * the default tick duration is 100 milliseconds and you will not need to try
 * different configurations in most cases.
 * 
 * <h3>Ticks per Wheel (Wheel Size)</h3>
 * 
 * {@link HashedWheelTimer} maintains a data structure called 'wheel'. To put
 * simply, a wheel is a hash table of {@link TimerTask}s whose hash function is
 * 'dead line of the task'. The default number of ticks per wheel (i.e. the size
 * of the wheel) is 512. You could specify a larger value if you are going to
 * schedule a lot of timeouts.
 * 
 * <h3>Implementation Details</h3>
 * 
 * {@link HashedWheelTimer} is based on <a
 * href="http://cseweb.ucsd.edu/users/varghese/">George Varghese</a> and Tony
 * Lauck's paper, <a
 * href="http://cseweb.ucsd.edu/users/varghese/PAPERS/twheel.ps.Z">'Hashed and
 * Hierarchical Timing Wheels: data structures to efficiently implement a timer
 * facility'</a>. More comprehensive slides are located <a
 * href="http://www.cse.wustl.edu/~cdgill/courses/cs6874/TimingWheels.ppt"
 * >here</a>.
 * 
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 */
public class HashedWheelTimer implements Timer {

    static final Log logger = LogFactory.getLog(HashedWheelTimer.class);
    private static final AtomicInteger id = new AtomicInteger();

    // I'd say 64 active timer threads are obvious misuse.
    private static final int MISUSE_WARNING_THRESHOLD = 64;
    private static final AtomicInteger activeInstances = new AtomicInteger();
    private static final AtomicBoolean loggedMisuseWarning = new AtomicBoolean();

    private final Worker worker = new Worker();
    final Thread workerThread;
    final AtomicBoolean shutdown = new AtomicBoolean();

    private final long roundDuration;
    final long tickDuration;
    final Set<HashedWheelTimeout>[] wheel;
    final ReusableIterator<HashedWheelTimeout>[] iterators;
    final int mask;
    final ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int wheelCursor;

    private final AtomicInteger size = new AtomicInteger(0);

    final int maxTimerCapacity;


    /**
     * Creates a new timer with the default thread factory (
     * {@link Executors#defaultThreadFactory()}), default tick duration, and
     * default number of ticks per wheel.
     */
    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }


    /**
     * Creates a new timer with the default thread factory (
     * {@link Executors#defaultThreadFactory()}) and default number of ticks per
     * wheel.
     * 
     * @param tickDuration
     *            the duration between tick
     * @param unit
     *            the time unit of the {@code tickDuration}
     */
    public HashedWheelTimer(final long tickDuration, final TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }


    /**
     * Creates a new timer with the default thread factory (
     * {@link Executors#defaultThreadFactory()}).
     * 
     * @param tickDuration
     *            the duration between tick
     * @param unit
     *            the time unit of the {@code tickDuration}
     * @param ticksPerWheel
     *            the size of the wheel
     */
    public HashedWheelTimer(final long tickDuration, final TimeUnit unit, final int ticksPerWheel,
            final int maxTimerCapacity) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel, maxTimerCapacity);
    }


    /**
     * Creates a new timer with the default tick duration and default number of
     * ticks per wheel.
     * 
     * @param threadFactory
     *            a {@link ThreadFactory} that creates a background
     *            {@link Thread} which is dedicated to {@link TimerTask}
     *            execution.
     */
    public HashedWheelTimer(final ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }


    /**
     * 返回当前timer个数
     * 
     * @return
     */
    public int size() {
        return this.size.get();
    }


    /**
     * Creates a new timer with the default number of ticks per wheel.
     * 
     * @param threadFactory
     *            a {@link ThreadFactory} that creates a background
     *            {@link Thread} which is dedicated to {@link TimerTask}
     *            execution.
     * @param tickDuration
     *            the duration between tick
     * @param unit
     *            the time unit of the {@code tickDuration}
     */
    public HashedWheelTimer(final ThreadFactory threadFactory, final long tickDuration, final TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512, 50000);
    }


    /**
     * Creates a new timer.
     * 
     * @param threadFactory
     *            a {@link ThreadFactory} that creates a background
     *            {@link Thread} which is dedicated to {@link TimerTask}
     *            execution.
     * @param tickDuration
     *            the duration between tick
     * @param unit
     *            the time unit of the {@code tickDuration}
     * @param ticksPerWheel
     * @param maxTimerCapacity
     *            the size of the wheel
     */
    public HashedWheelTimer(final ThreadFactory threadFactory, long tickDuration, final TimeUnit unit,
            final int ticksPerWheel, final int maxTimerCapacity) {

        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (maxTimerCapacity <= 0) {
            throw new IllegalArgumentException("maxTimerCapacity must be greater than 0: " + maxTimerCapacity);
        }

        // Normalize ticksPerWheel to power of two and initialize the wheel.
        this.wheel = createWheel(ticksPerWheel);
        this.iterators = createIterators(this.wheel);
        this.maxTimerCapacity = maxTimerCapacity;
        this.mask = this.wheel.length - 1;

        // Convert tickDuration to milliseconds.
        this.tickDuration = tickDuration = unit.toMillis(tickDuration);

        // Prevent overflow.
        if (tickDuration == Long.MAX_VALUE || tickDuration >= Long.MAX_VALUE / this.wheel.length) {
            throw new IllegalArgumentException("tickDuration is too long: " + tickDuration + ' ' + unit);
        }

        this.roundDuration = tickDuration * this.wheel.length;

        this.workerThread =
                threadFactory.newThread(new ThreadRenamingRunnable(this.worker, "Hashed wheel timer #"
                        + id.incrementAndGet()));

        // Misuse check
        final int activeInstances = HashedWheelTimer.activeInstances.incrementAndGet();
        if (activeInstances >= MISUSE_WARNING_THRESHOLD && loggedMisuseWarning.compareAndSet(false, true)) {
            logger.debug("There are too many active " + HashedWheelTimer.class.getSimpleName() + " instances ("
                    + activeInstances + ") - you should share the small number "
                    + "of instances to avoid excessive resource consumption.");
        }
    }


    @SuppressWarnings("unchecked")
    private static Set<HashedWheelTimeout>[] createWheel(int ticksPerWheel) {
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException("ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }

        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        final Set<HashedWheelTimeout>[] wheel = new Set[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] =
                    new MapBackedSet<HashedWheelTimeout>(new ConcurrentIdentityHashMap<HashedWheelTimeout, Boolean>(16,
                        0.95f, 4));
        }
        return wheel;
    }


    @SuppressWarnings("unchecked")
    private static ReusableIterator<HashedWheelTimeout>[] createIterators(final Set<HashedWheelTimeout>[] wheel) {
        final ReusableIterator<HashedWheelTimeout>[] iterators = new ReusableIterator[wheel.length];
        for (int i = 0; i < wheel.length; i++) {
            iterators[i] = (ReusableIterator<HashedWheelTimeout>) wheel[i].iterator();
        }
        return iterators;
    }


    private static int normalizeTicksPerWheel(final int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }


    /**
     * Starts the background thread explicitly. The background thread will start
     * automatically on demand even if you did not call this method.
     * 
     * @throws IllegalStateException
     *             if this timer has been {@linkplain #stop() stopped} already
     */
    public synchronized void start() {
        if (this.shutdown.get()) {
            throw new IllegalStateException("cannot be started once stopped");
        }

        if (!this.workerThread.isAlive()) {
            this.workerThread.start();
        }
    }


    public synchronized Set<Timeout> stop() {
        if (!this.shutdown.compareAndSet(false, true)) {
            return Collections.emptySet();
        }

        boolean interrupted = false;
        while (this.workerThread.isAlive()) {
            this.workerThread.interrupt();
            try {
                this.workerThread.join(100);
            }
            catch (final InterruptedException e) {
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        activeInstances.decrementAndGet();

        final Set<Timeout> unprocessedTimeouts = new HashSet<Timeout>();
        for (final Set<HashedWheelTimeout> bucket : this.wheel) {
            unprocessedTimeouts.addAll(bucket);
            bucket.clear();
        }

        return Collections.unmodifiableSet(unprocessedTimeouts);
    }


    public Timeout newTimeout(final TimerTask task, long delay, final TimeUnit unit) {
        final long currentTime = System.currentTimeMillis();

        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        delay = unit.toMillis(delay);
        if (delay < this.tickDuration) {
            delay = this.tickDuration;
        }

        if (!this.workerThread.isAlive()) {
            this.start();
        }

        if (this.size.get() >= this.maxTimerCapacity) {
            throw new RejectedExecutionException("Timer size " + this.size + " is great than maxTimerCapacity "
                    + this.maxTimerCapacity);
        }

        // Prepare the required parameters to create the timeout object.
        HashedWheelTimeout timeout;
        final long lastRoundDelay = delay % this.roundDuration;
        final long lastTickDelay = delay % this.tickDuration;

        final long relativeIndex = lastRoundDelay / this.tickDuration + (lastTickDelay != 0 ? 1 : 0);

        final long deadline = currentTime + delay;

        final long remainingRounds = delay / this.roundDuration - (delay % this.roundDuration == 0 ? 1 : 0);

        // Add the timeout to the wheel.
        this.lock.readLock().lock();
        try {
            timeout =
                    new HashedWheelTimeout(task, deadline, (int) (this.wheelCursor + relativeIndex & this.mask),
                        remainingRounds);

            this.wheel[timeout.stopIndex].add(timeout);
        }
        finally {
            this.lock.readLock().unlock();
        }
        this.size.incrementAndGet();

        return timeout;
    }

    private final class Worker implements Runnable {

        private long startTime;
        private long tick;


        Worker() {
            super();
        }


        public void run() {
            final List<HashedWheelTimeout> expiredTimeouts = new ArrayList<HashedWheelTimeout>();

            this.startTime = System.currentTimeMillis();
            this.tick = 1;

            while (!HashedWheelTimer.this.shutdown.get()) {
                this.waitForNextTick();
                this.fetchExpiredTimeouts(expiredTimeouts);
                this.notifyExpiredTimeouts(expiredTimeouts);
            }
        }


        private void fetchExpiredTimeouts(final List<HashedWheelTimeout> expiredTimeouts) {

            // Find the expired timeouts and decrease the round counter
            // if necessary. Note that we don't send the notification
            // immediately to make sure the listeners are called without
            // an exclusive lock.
            HashedWheelTimer.this.lock.writeLock().lock();
            try {
                final int oldBucketHead = HashedWheelTimer.this.wheelCursor;

                final int newBucketHead = oldBucketHead + 1 & HashedWheelTimer.this.mask;
                HashedWheelTimer.this.wheelCursor = newBucketHead;

                final ReusableIterator<HashedWheelTimeout> i = HashedWheelTimer.this.iterators[oldBucketHead];
                this.fetchExpiredTimeouts(expiredTimeouts, i);
            }
            finally {
                HashedWheelTimer.this.lock.writeLock().unlock();
            }
        }


        private void fetchExpiredTimeouts(final List<HashedWheelTimeout> expiredTimeouts,
                final ReusableIterator<HashedWheelTimeout> i) {

            final long currentDeadline = System.currentTimeMillis() + HashedWheelTimer.this.tickDuration;
            i.rewind();
            while (i.hasNext()) {
                final HashedWheelTimeout timeout = i.next();
                if (timeout.remainingRounds <= 0) {
                    if (timeout.deadline < currentDeadline) {
                        i.remove();
                        expiredTimeouts.add(timeout);
                    }
                    else {
                        // A rare case where a timeout is put for the next
                        // round: just wait for the next round.
                    }
                }
                else {
                    timeout.remainingRounds--;
                }
            }
        }


        private void notifyExpiredTimeouts(final List<HashedWheelTimeout> expiredTimeouts) {
            // Notify the expired timeouts.
            for (int i = expiredTimeouts.size() - 1; i >= 0; i--) {
                expiredTimeouts.get(i).expire();
                HashedWheelTimer.this.size.decrementAndGet();
            }

            // Clean up the temporary list.
            expiredTimeouts.clear();

        }


        private void waitForNextTick() {
            for (;;) {
                final long currentTime = System.currentTimeMillis();
                final long sleepTime = HashedWheelTimer.this.tickDuration * this.tick - (currentTime - this.startTime);

                if (sleepTime <= 0) {
                    break;
                }

                try {
                    Thread.sleep(sleepTime);
                }
                catch (final InterruptedException e) {
                    if (HashedWheelTimer.this.shutdown.get()) {
                        return;
                    }
                }
            }

            // Reset the tick if overflow is expected.
            if (HashedWheelTimer.this.tickDuration * this.tick > Long.MAX_VALUE - HashedWheelTimer.this.tickDuration) {
                this.startTime = System.currentTimeMillis();
                this.tick = 1;
            }
            else {
                // Increase the tick if overflow is not likely to happen.
                this.tick++;
            }
        }
    }

    private final class HashedWheelTimeout implements Timeout {

        private final TimerTask task;
        final int stopIndex;
        final long deadline;
        volatile long remainingRounds;
        private volatile boolean cancelled;


        HashedWheelTimeout(final TimerTask task, final long deadline, final int stopIndex, final long remainingRounds) {
            this.task = task;
            this.deadline = deadline;
            this.stopIndex = stopIndex;
            this.remainingRounds = remainingRounds;
        }


        public Timer getTimer() {
            return HashedWheelTimer.this;
        }


        public TimerTask getTask() {
            return this.task;
        }


        public void cancel() {
            if (this.isExpired()) {
                return;
            }

            this.cancelled = true;
            // Might be called more than once, but doesn't matter.
            if (HashedWheelTimer.this.wheel[this.stopIndex].remove(this)) {
                HashedWheelTimer.this.size.decrementAndGet();
            }
        }


        public boolean isCancelled() {
            return this.cancelled;
        }


        public boolean isExpired() {
            return this.cancelled || System.currentTimeMillis() > this.deadline;
        }


        public void expire() {
            if (this.cancelled) {
                return;
            }

            try {
                this.task.run(this);
            }
            catch (final Throwable t) {
                logger.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + ".", t);
            }
        }


        @Override
        public String toString() {
            final long currentTime = System.currentTimeMillis();
            final long remaining = this.deadline - currentTime;

            final StringBuilder buf = new StringBuilder(192);
            buf.append(this.getClass().getSimpleName());
            buf.append('(');

            buf.append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining);
                buf.append(" ms later, ");
            }
            else if (remaining < 0) {
                buf.append(-remaining);
                buf.append(" ms ago, ");
            }
            else {
                buf.append("now, ");
            }

            if (this.isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(')').toString();
        }
    }
}