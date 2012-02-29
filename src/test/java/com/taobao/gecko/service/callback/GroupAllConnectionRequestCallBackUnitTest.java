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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.GroupAllConnectionCallBackListener;
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
 * @since 1.0, 2009-12-22 下午03:49:55
 */

public class GroupAllConnectionRequestCallBackUnitTest {
    private GroupAllConnectionRequestCallBack requestCallBack;
    private DefaultRemotingContext remotingContext;


    @Before
    public void setUp() {
        this.remotingContext = new DefaultRemotingContext(new ServerConfig(), new NotifyCommandFactory());
    }


    @After
    public void tearDown() {
        this.remotingContext.dispose();
    }


    @Test
    public void testResponseSetException() {
        final CountDownLatch latch = new CountDownLatch(3);
        final long timeout = 2000;
        final ConcurrentHashMap<Connection, ResponseCommand> resultMap =
                new ConcurrentHashMap<Connection, ResponseCommand>();
        final GroupAllConnectionCallBackListener listener =
                EasyMock.createMock(GroupAllConnectionCallBackListener.class);
        listener.onResponse(resultMap);
        EasyMock.expectLastCall();
        EasyMock.expect(listener.getExecutor()).andReturn(null);
        EasyMock.replay(listener);

        this.requestCallBack =
                new GroupAllConnectionRequestCallBack(listener, latch, timeout, System.currentTimeMillis(), resultMap);
        final RequestCommand requestCommand = new NotifyDummyRequestCommand("test");
        Connection conn = new DefaultConnection(new MockSession(), this.remotingContext);
        this.requestCallBack.setException(new IOException("error"), conn, requestCommand);

        Assert.assertEquals(1, resultMap.size());
        Assert.assertEquals(2, latch.getCount());

        conn = new DefaultConnection(new MockSession(), this.remotingContext);
        this.requestCallBack.onResponse("group2", new NotifyDummyAckCommand((NotifyRequestCommand) requestCommand,
            "hello"), conn);
        Assert.assertEquals(2, resultMap.size());
        Assert.assertEquals(1, latch.getCount());

        conn = new DefaultConnection(new MockSession(), this.remotingContext);
        this.requestCallBack.setException(new IOException("error"), conn, requestCommand);
        Assert.assertEquals(3, resultMap.size());
        Assert.assertEquals(0, latch.getCount());

        for (final Map.Entry<Connection, ResponseCommand> entry : resultMap.entrySet()) {
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

        Assert.assertSame(resultMap, this.requestCallBack.getResultMap());
        EasyMock.verify(listener);
    }
}