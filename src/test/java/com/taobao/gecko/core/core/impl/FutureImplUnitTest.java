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

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import org.junit.Test;

import com.taobao.gecko.core.core.impl.FutureImpl;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÉÏÎç11:04:22
 */

public class FutureImplUnitTest {

    private static final class NotifyFutureRunner implements Runnable {
        FutureImpl<Boolean> future;
        long sleepTime;
        Throwable throwable;


        public NotifyFutureRunner(FutureImpl<Boolean> future, long sleepTime, Throwable throwable) {
            super();
            this.future = future;
            this.sleepTime = sleepTime;
            this.throwable = throwable;
        }


        public void run() {
            try {
                Thread.sleep(sleepTime);
                if (this.throwable != null) {
                    future.failure(throwable);
                }
                else {
                    future.setResult(true);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void testGet() throws Exception {
        FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        new Thread(new NotifyFutureRunner(future, 2000, null)).start();
        boolean result = future.get();
        Assert.assertTrue(result);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
    }


    @Test
    public void testGetImmediately() throws Exception {
        FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        future.setResult(true);
        boolean result = future.get();
        Assert.assertTrue(result);
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
    }


    @Test
    public void testGetException() throws Exception {
        FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        new Thread(new NotifyFutureRunner(future, 2000, new IOException("hello"))).start();
        try {
            future.get();
            Assert.fail();
        }
        catch (ExecutionException e) {
            Assert.assertEquals("hello", e.getCause().getMessage());

        }
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());

    }


    @Test
    public void testCancel() throws Exception {
        final FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                    future.cancel(true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        try {
            future.get();
            Assert.fail();
        }
        catch (CancellationException e) {
            Assert.assertTrue(true);

        }
        Assert.assertTrue(future.isDone());
        Assert.assertTrue(future.isCancelled());
    }


    @Test
    public void testGetTimeout() throws Exception {
        FutureImpl<Boolean> future = new FutureImpl<Boolean>();
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
            Assert.fail();
        }
        catch (TimeoutException e) {
            Assert.assertTrue(true);
        }
    }
}