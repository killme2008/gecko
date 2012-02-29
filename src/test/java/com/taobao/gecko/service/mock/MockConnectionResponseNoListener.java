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
package com.taobao.gecko.service.mock;

import org.junit.Assert;

import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.service.exception.NotifyRemotingException;


public class MockConnectionResponseNoListener extends MockConnection {

    public MockConnectionResponseNoListener(boolean connected) {
        super(connected);
    }


    @Override
    public void response(Object responseCommand) throws NotifyRemotingException {
        Assert.assertEquals(ResponseStatus.UNKNOWN, ((ResponseCommand) responseCommand).getResponseStatus());
    }
}