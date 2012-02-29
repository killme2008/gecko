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
package com.taobao.gecko.core.core;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.taobao.gecko.core.core.impl.StandardSocketOption;


public class SocketOptionUnitTest {
    @Test
    public void testType() {
        Assert.assertEquals(Integer.class, StandardSocketOption.SO_LINGER.type());
        Assert.assertEquals(Boolean.class, StandardSocketOption.SO_KEEPALIVE.type());
        Assert.assertEquals(Integer.class, StandardSocketOption.SO_RCVBUF.type());
        Assert.assertEquals(Integer.class, StandardSocketOption.SO_SNDBUF.type());
        Assert.assertEquals(Boolean.class, StandardSocketOption.SO_REUSEADDR.type());
        Assert.assertEquals(Boolean.class, StandardSocketOption.TCP_NODELAY.type());
    }


    @Test
    public void testPutInMap() {
        Map<SocketOption, Object> map = new HashMap<SocketOption, Object>();
        map.put(StandardSocketOption.SO_KEEPALIVE, true);
        map.put(StandardSocketOption.SO_RCVBUF, 4096);
        map.put(StandardSocketOption.SO_SNDBUF, 4096);
        map.put(StandardSocketOption.TCP_NODELAY, false);

        Assert.assertEquals(4096, map.get(StandardSocketOption.SO_RCVBUF));
        Assert.assertEquals(4096, map.get(StandardSocketOption.SO_SNDBUF));
        Assert.assertEquals(false, map.get(StandardSocketOption.TCP_NODELAY));
        Assert.assertEquals(true, map.get(StandardSocketOption.SO_KEEPALIVE));
    }
}