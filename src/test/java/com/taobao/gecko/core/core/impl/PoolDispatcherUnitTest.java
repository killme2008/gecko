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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÉÏÎç11:34:43
 */

public class PoolDispatcherUnitTest {
    PoolDispatcher dispatcher;


    @Before
    public void setUp() {
        this.dispatcher = new PoolDispatcher(10, 60, TimeUnit.SECONDS, new ThreadPoolExecutor.AbortPolicy());
    }

    private static final class TestRunner implements Runnable {
        boolean ran;


        public void run() {
            this.ran = true;

        }
    }


    @Test
    public void testDispatch() throws Exception {
        TestRunner runner = new TestRunner();
        this.dispatcher.dispatch(runner);
        Thread.sleep(1000);
        Assert.assertTrue(runner.ran);

    }


    @Test
    public void testDispatchNull() throws Exception {
        try {
            this.dispatcher.dispatch(null);
            Assert.fail();
        }
        catch (NullPointerException e) {

        }
    }


    @Test
    public void testDispatcherStop() throws Exception {
        this.dispatcher.stop();
        TestRunner runner = new TestRunner();
        this.dispatcher.dispatch(runner);
        Thread.sleep(1000);
        Assert.assertFalse(runner.ran);
    }


    @After
    public void tearDown() {
        this.dispatcher.stop();

    }

}