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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.core.nio.NioSession;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.mock.MockSession;
import com.taobao.gecko.service.notify.NotifyCommandFactory;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 下午02:28:37
 */

public class DefaultRemotingContextUnitTest {
    private static final class MockTask implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {

            }
        }
    }

    private DefaultRemotingContext remotingContext;


    @Before
    public void setUp() {
        final ClientConfig config = new ClientConfig();
        config.setCallBackExecutorPoolSize(1);
        config.setCallBackExecutorQueueSize(1);
        config.setMaxCallBackExecutorPoolSize(1);
        this.remotingContext = new DefaultRemotingContext(config, new NotifyCommandFactory());
    }


    @After
    public void tearDown() {
        if (this.remotingContext != null) {
            this.remotingContext.dispose();
        }
    }


    @Test
    public void testAttribute() {
        assertNull(this.remotingContext.getAttribute("a"));
        assertEquals(1, this.remotingContext.getAttribute("a", 1));
        this.remotingContext.setAttribute("a", 2);
        assertEquals(2, this.remotingContext.getAttribute("a", 1));
        Set<Object> keys = this.remotingContext.getAttributeKeys();
        assertTrue(keys.contains("a"));
        assertEquals(1, keys.size());

        this.remotingContext.setAttributeIfAbsent("b", 3);
        keys = this.remotingContext.getAttributeKeys();
        assertEquals(2, keys.size());
        assertEquals(3, this.remotingContext.getAttribute("b"));

        this.remotingContext.removeAttribute("a");
        assertNull(this.remotingContext.getAttribute("a"));
        this.remotingContext.removeAttribute("b");
        assertNull(this.remotingContext.getAttribute("b"));
        keys = this.remotingContext.getAttributeKeys();
        assertTrue(keys.isEmpty());
    }


    @Test
    public void testSession2ConnectionMapping() {
        final NioSession session = new MockSession();
        final String groupName = "group1";
        final DefaultConnection conn = new DefaultConnection(session, this.remotingContext);

        assertNull(this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP));
        assertNull(this.remotingContext.getConnectionsByGroup(groupName));
        // 加入默认分组
        this.remotingContext.addConnection(conn);
        assertNotNull(this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP));

        assertEquals(1, this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP).size());
        assertTrue(this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP).contains(conn));
        assertNull(this.remotingContext.getConnectionsByGroup(groupName));

        // 加入groupName
        this.remotingContext.addConnectionToGroup(groupName, conn);
        assertEquals(1, this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP).size());
        assertEquals(1, this.remotingContext.getConnectionsByGroup(groupName).size());
        assertTrue(this.remotingContext.getConnectionsByGroup(groupName).contains(conn));

        final Set<String> groupSet = this.remotingContext.getGroupSet();
        assertEquals(2, groupSet.size());
        assertTrue(groupSet.contains(Constants.DEFAULT_GROUP));
        assertTrue(groupSet.contains(groupName));

        // 从默认分组移除
        this.remotingContext.removeConnection(conn);
        assertNull(this.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP));
        // 从groupName移除
        this.remotingContext.removeConnectionFromGroup(groupName, conn);
        assertNull(this.remotingContext.getConnectionsByGroup(groupName));
        assertTrue(this.remotingContext.getGroupSet().isEmpty());

        // 测试session到conn的映射
        this.remotingContext.addSession2ConnectionMapping(session, conn);
        assertSame(conn, this.remotingContext.getConnectionBySession(session));
        assertSame(conn, this.remotingContext.removeSession2ConnectionMapping(session));
        assertNull(this.remotingContext.getConnectionBySession(session));
    }

}