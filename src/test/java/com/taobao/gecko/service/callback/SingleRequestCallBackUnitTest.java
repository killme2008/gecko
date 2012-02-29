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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-21 下午05:23:04
 */

public class SingleRequestCallBackUnitTest {
    private SingleRequestCallBack requestCallBack;
    private final NotifyDummyRequestCommand request = new NotifyDummyRequestCommand("test");
    private final NotifyDummyAckCommand response = new NotifyDummyAckCommand(this.request, "test");


    @Test
    public void testGetResult() throws Exception {
        this.requestCallBack = new SingleRequestCallBack(this.request.getRequestHeader(), 1000);

        try {
            this.requestCallBack.getResult(1000, TimeUnit.MILLISECONDS, null);
            Assert.fail();
        }
        catch (final TimeoutException e) {
            Assert.assertEquals("Operation timeout", e.getMessage());
        }

        this.requestCallBack = new SingleRequestCallBack(this.request.getRequestHeader(), 1000);
        this.requestCallBack.onResponse("test", this.response, null);

        Assert.assertSame(this.response, this.requestCallBack.getResult());

    }


    @Test
    public void testSetException() throws Exception {
        this.requestCallBack = new SingleRequestCallBack(this.request.getRequestHeader(), 1000);
        this.requestCallBack.setException(new NotifyRemotingException("test"), null, this.request);
        try {
            this.requestCallBack.getResult();
            Assert.fail();
        }
        catch (final NotifyRemotingException e) {
            Assert.assertEquals("同步调用失败", e.getMessage());
            Assert.assertEquals("test", e.getCause().getMessage());
        }
    }


    @Test
    public void testIsInValid() throws Exception {
        this.requestCallBack = new SingleRequestCallBack(this.request.getRequestHeader(), 1000);
        Assert.assertFalse(this.requestCallBack.isInvalid(System.currentTimeMillis()));
        Thread.sleep(2000);
        Assert.assertTrue(this.requestCallBack.isInvalid(System.currentTimeMillis()));

    }

}