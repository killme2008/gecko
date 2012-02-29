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
package com.taobao.gecko.core.nio.impl;

import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.gecko.core.core.ServerController;
import com.taobao.gecko.core.core.impl.AbstractControllerUnitTest;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-25 ÉÏÎç11:09:06
 */

public abstract class NioServerControllerUnitTest extends AbstractControllerUnitTest {

    public abstract void newServer();


    @Test
    public void testNioControllerConfig() throws Exception {
        newServer();
        ((NioController) controller).setSelectorPoolSize(5);
        Assert.assertEquals(5, ((NioController) controller).getSelectorPoolSize());

        ((ServerController) controller).bind(8080);
        Assert.assertEquals(5, ((NioController) controller).getSelectorManager().getSelectorCount());
    }


    @Test
    public void testBindUnBind() throws Exception {
        newServer();
        try {
            controller.bind(null);
            Assert.fail();
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("Null inetSocketAddress", e.getMessage());
        }
        ((ServerController) controller).bind(8080);
        InetSocketAddress localAddress = controller.getLocalSocketAddress();
        Assert.assertEquals(8080, localAddress.getPort());
        Assert.assertTrue(controller.isStarted());

        ((ServerController) controller).unbind();
        Assert.assertFalse(controller.isStarted());
    }


    @Test
    public void testBindDuplicated() throws Exception {
        newServer();
        ((ServerController) controller).bind(8080);
        try {
            ((ServerController) controller).bind(8080);
            Assert.fail();
        }
        catch (IllegalStateException e) {
            Assert.assertTrue(true);
        }
    }

}