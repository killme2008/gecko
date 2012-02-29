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
package com.taobao.gecko.service.processor;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.junit.Test;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingContext;
import com.taobao.gecko.service.notify.OpCode;
import com.taobao.gecko.service.notify.request.NotifyHeartBeatCommand;
import com.taobao.gecko.service.notify.response.NotifyBooleanAckCommand;
import com.taobao.gecko.service.notify.response.NotifyResponseCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 ÏÂÎç05:39:48
 */

public class HeartBeatCommandProcessorUnitTest {
    private HeartBeatCommandProecssor heartBeatCommandProecssor;


    @Test
    public void testHandleRequest() throws Exception {
        final IMocksControl mocksControl = EasyMock.createControl();
        final Connection conn = mocksControl.createMock(Connection.class);
        final RemotingContext context = mocksControl.createMock(RemotingContext.class);
        final CommandFactory commandFactory = mocksControl.createMock(CommandFactory.class);
        final NotifyHeartBeatCommand request = new NotifyHeartBeatCommand();
        final BooleanAckCommand response = new NotifyBooleanAckCommand(request, ResponseStatus.NO_ERROR, null);

        EasyMock.expect(conn.getRemotingContext()).andReturn(context).once();
        EasyMock.expect(context.getCommandFactory()).andReturn(commandFactory).once();
        EasyMock
            .expect(commandFactory.createBooleanAckCommand(request.getRequestHeader(), ResponseStatus.NO_ERROR, null))
            .andReturn(response).once();
        conn.response(response);

        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {

            public Object answer() throws Throwable {
                final Object[] args = EasyMock.getCurrentArguments();
                final ResponseCommand responseCommand = (ResponseCommand) args[0];
                Assert.assertSame(responseCommand, response);
                Assert.assertEquals(ResponseStatus.NO_ERROR, responseCommand.getResponseStatus());
                Assert.assertEquals(OpCode.HEARTBEAT, ((NotifyResponseCommand) responseCommand).getOpCode());
                return null;
            }

        });

        mocksControl.replay();

        this.heartBeatCommandProecssor = new HeartBeatCommandProecssor();

        this.heartBeatCommandProecssor.handleRequest(request, conn);

        mocksControl.verify();

    }
}