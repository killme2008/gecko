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

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.mock.MockSession;
import com.taobao.gecko.service.notify.NotifyCommandFactory;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-22 下午03:59:52
 */

public class GroupManagerUnitTest {
    private GroupManager groupManager;
    private DefaultRemotingContext remotingContext;


    @Before
    public void setUp() {
        this.groupManager = new GroupManager();
        this.remotingContext = new DefaultRemotingContext(new ClientConfig(), new NotifyCommandFactory());

    }


    @After
    public void tearDown() {
        this.remotingContext.dispose();
    }


    @Test
    public void testAddConnection() {
        final String group1 = "group1";
        final String group2 = "group2";
        Connection conn = this.createConn();

        Assert.assertEquals(0, this.groupManager.getGroupConnectionCount(group1));
        this.groupManager.addConnection(group1, conn);
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(1, conn.getGroupSet().size());
        // 重复添加
        this.groupManager.addConnection(group1, conn);
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(1, conn.getGroupSet().size());

        // 添加到其他组
        this.groupManager.addConnection(group2, conn);
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(2, conn.getGroupSet().size());
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group2));

        // 添加更多
        for (int i = 0; i < 10; i++) {
            conn = this.createConn();
            this.groupManager.addConnection(group1, conn);
        }
        Assert.assertEquals(11, this.groupManager.getGroupConnectionCount(group1));
    }


    @Test
    public void testRemoveConnection() {
        final String group1 = "group1";
        final String group2 = "group2";
        final Connection conn = this.createConn();
        this.groupManager.addConnection(group1, conn);
        this.groupManager.addConnection(group2, conn);
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group2));
        Assert.assertEquals(2, conn.getGroupSet().size());

        this.groupManager.removeConnection(group1, conn);
        Assert.assertEquals(0, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group2));
        Assert.assertEquals(1, conn.getGroupSet().size());
        Assert.assertFalse(conn.getGroupSet().contains(group1));
        Assert.assertTrue(conn.getGroupSet().contains(group2));

        this.groupManager.removeConnection(group2, conn);
        Assert.assertEquals(0, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(0, this.groupManager.getGroupConnectionCount(group2));
        Assert.assertEquals(0, conn.getGroupSet().size());
    }


    @Test
    public void testGroupSetAndGetConnectionsByGroup() {

        final String group1 = "group1";
        final String group2 = "group2";
        final Connection conn1 = this.createConn();
        final Connection conn2 = this.createConn();
        final Connection conn3 = this.createConn();
        this.groupManager.addConnection(group1, conn1);
        this.groupManager.addConnection(group2, conn1);
        this.groupManager.addConnection(group2, conn2);
        this.groupManager.addConnection(group2, conn3);

        Assert.assertEquals(1, this.groupManager.getGroupConnectionCount(group1));
        Assert.assertEquals(3, this.groupManager.getGroupConnectionCount(group2));
        Assert.assertEquals(2, conn1.getGroupSet().size());
        Assert.assertEquals(1, conn2.getGroupSet().size());
        Assert.assertEquals(1, conn3.getGroupSet().size());

        final Set<String> groupSet = this.groupManager.getGroupSet();
        Assert.assertEquals(2, groupSet.size());
        Assert.assertTrue(groupSet.contains(group1));
        Assert.assertTrue(groupSet.contains(group2));

        final List<Connection> list1 = this.groupManager.getConnectionsByGroup(group1);
        Assert.assertEquals(1, list1.size());
        Assert.assertTrue(list1.contains(conn1));

        final List<Connection> list2 = this.groupManager.getConnectionsByGroup(group2);
        Assert.assertEquals(3, list2.size());
        Assert.assertTrue(list2.contains(conn1));
        Assert.assertTrue(list2.contains(conn2));
        Assert.assertTrue(list2.contains(conn3));
    }


    private Connection createConn() {
        final Connection conn = new DefaultConnection(new MockSession(), this.remotingContext);
        return conn;
    }

}