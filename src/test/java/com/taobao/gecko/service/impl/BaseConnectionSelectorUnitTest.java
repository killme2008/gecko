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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionSelector;
import com.taobao.gecko.service.mock.MockConnection;
import com.taobao.gecko.utils.ConcurrentTestCase;
import com.taobao.gecko.utils.ConcurrentTestTask;


public abstract class BaseConnectionSelectorUnitTest {

    ConnectionSelector selector;


    public abstract ConnectionSelector newConnectionSelector();


    @Test
    public void testPerformance() throws Exception {
        final List<Connection> connList = this.createConnList(10, true);
        this.selector = this.newConnectionSelector();
        final ConcurrentTestCase testCase = new ConcurrentTestCase(500, 100000, new ConcurrentTestTask() {

            public void run(final int index, final int times) throws Exception {
                if (BaseConnectionSelectorUnitTest.this.selector.select("test", null, connList) == null) {
                    throw new NullPointerException();
                }

            }
        });
        testCase.start();
        System.out.println("Duration:" + testCase.getDurationInMillis());
    }


    @Test
    public void testSelect() throws Exception {
        final String group = "test";
        this.selector = this.newConnectionSelector();

        // null list
        Assert.assertNull(this.selector.select(group, null, null));
        // empty
        Assert.assertNull(this.selector.select(group, null, this.createConnList(0, true)));
        // one connected
        List<Connection> connList = this.createConnList(1, true);
        final Connection conn = this.selector.select(group, null, connList);
        Assert.assertNotNull(conn);
        Assert.assertSame(conn, this.selector.select(group, null, connList));
        // one disconnected
        connList = this.createConnList(1, false);
        Assert.assertNull(this.selector.select(group, null, connList));

        // three connections
        connList = this.createConnList(3, true);
        for (int i = 0; i < 10; i++) {
            Assert.assertNotNull(this.selector.select(group, null, connList));
        }

        // some connect,some disconnect
        connList = this.createConnList(3, true);
        connList.addAll(this.createConnList(3, false));
        int nullCount = 0;
        for (int i = 0; i < 1000; i++) {
            if (this.selector.select(group, null, connList) == null) {
                nullCount++;
            }
        }
        System.out.println(nullCount);
    }


    public List<Connection> createConnList(final int num, final boolean connected) {
        final List<Connection> result = new ArrayList<Connection>(num);
        for (int i = 0; i < num; i++) {
            result.add(new MockConnection(connected));
        }
        return result;
    }
}