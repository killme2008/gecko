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
package com.taobao.gecko.service.callback;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.service.MultiGroupCallBackListener;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.impl.DefaultConnection;
import com.taobao.gecko.service.impl.DefaultRemotingContext;
import com.taobao.gecko.service.mock.MockSession;
import com.taobao.gecko.service.notify.NotifyCommandFactory;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 下午05:52:21
 */

public class MultiGroupRequestCallBackUnitTest {
    private MultiGroupRequestCallBack requestCallBack;
    private DefaultRemotingContext remotingContext;
    private DefaultConnection conn;


    @Before
    public void setUp() {
        this.remotingContext = new DefaultRemotingContext(new ServerConfig(), new NotifyCommandFactory());
        this.conn = new DefaultConnection(new MockSession(), this.remotingContext);
    }


    @After
    public void tearDown() {
        this.remotingContext.dispose();
    }


    @Test
    public void testResponseSetException() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final long timeout = 2000;
        final ConcurrentHashMap<String, ResponseCommand> resultMap = new ConcurrentHashMap<String, ResponseCommand>();
        final String args = "hello";
        final MultiGroupCallBackListener listener = EasyMock.createMock(MultiGroupCallBackListener.class);
        EasyMock.expect(listener.getExecutor()).andReturn(null);
        listener.onResponse(resultMap, args);
        EasyMock.expectLastCall();
        EasyMock.replay(listener);

        final Method addOpaqueToGroupMappingMethod =
                DefaultConnection.class.getDeclaredMethod("addOpaqueToGroupMapping", Integer.class, String.class);
        addOpaqueToGroupMappingMethod.setAccessible(true);

        this.requestCallBack =
                new MultiGroupRequestCallBack(listener, latch, timeout, System.currentTimeMillis(), resultMap,
                    new AtomicBoolean(), args);
        final RequestCommand requestCommand = new NotifyDummyRequestCommand("test");
        addOpaqueToGroupMappingMethod.invoke(this.conn, requestCommand.getOpaque(), "group1");
        this.requestCallBack.setException(new IOException("error"), this.conn, requestCommand);

        Assert.assertEquals(1, resultMap.size());
        Assert.assertEquals(2, latch.getCount());
        Assert.assertNull(this.conn.removeOpaqueToGroupMapping(requestCommand.getOpaque()));

        addOpaqueToGroupMappingMethod.invoke(this.conn, requestCommand.getOpaque(), "group2");
        this.requestCallBack.onResponse("group2", new NotifyDummyAckCommand((NotifyRequestCommand) requestCommand,
            "hello"), this.conn);
        Assert.assertEquals(2, resultMap.size());
        Assert.assertEquals(1, latch.getCount());
        Assert.assertNull(this.conn.removeOpaqueToGroupMapping(requestCommand.getOpaque()));

        addOpaqueToGroupMappingMethod.invoke(this.conn, requestCommand.getOpaque(), "group3");
        this.requestCallBack.setException(new IOException("error"), this.conn, requestCommand);
        Assert.assertEquals(3, resultMap.size());
        Assert.assertEquals(0, latch.getCount());
        Assert.assertNull(this.conn.removeOpaqueToGroupMapping(requestCommand.getOpaque()));

        final Set<String> groupSet = new HashSet<String>();
        groupSet.add("group1");
        groupSet.add("group2");
        groupSet.add("group3");
        for (final Map.Entry<String, ResponseCommand> entry : resultMap.entrySet()) {
            Assert.assertTrue(groupSet.contains(entry.getKey()));
            final ResponseCommand response = entry.getValue();
            if (response.getResponseStatus() == ResponseStatus.NO_ERROR) {
                Assert.assertEquals("hello", ((NotifyDummyAckCommand) response).getDummy());
            }
            else if (response.getResponseStatus() == ResponseStatus.ERROR_COMM) {
                Assert.assertEquals("error", ((BooleanAckCommand) response).getErrorMsg());
            }
            else {
                throw new RuntimeException("无效结果");
            }
        }

        Assert.assertSame(resultMap, this.requestCallBack.getResponseCommandMap());
        EasyMock.verify(listener);
    }
}