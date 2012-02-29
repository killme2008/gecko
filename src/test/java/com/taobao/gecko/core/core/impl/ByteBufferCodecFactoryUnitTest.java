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
package com.taobao.gecko.core.core.impl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory.Encoder;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÉÏÎç10:49:54
 */

public class ByteBufferCodecFactoryUnitTest {
    ByteBufferCodecFactory codecFactory;


    @Before
    public void setUp() {
        codecFactory = new ByteBufferCodecFactory();
    }


    @Test
    public void testEncodeNormal() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode(IoBuffer.wrap("hello".getBytes("utf-8")), null);
        Assert.assertNotNull(buffer);
        Assert.assertTrue(buffer.hasRemaining());
        Assert.assertArrayEquals("hello".getBytes("utf-8"), buffer.array());

    }


    @Test
    public void testEncodeEmpty() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNull(encoder.encode(null, null));
        Assert.assertEquals(IoBuffer.allocate(0), encoder.encode(IoBuffer.allocate(0), null));
    }


    @Test
    public void decodeNormal() throws Exception {
        Encoder encoder = this.codecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode(IoBuffer.wrap("hello".getBytes("utf-8")), null);

        IoBuffer decodeBuffer = (IoBuffer) this.codecFactory.getDecoder().decode(buffer, null);
        Assert.assertEquals(IoBuffer.wrap("hello".getBytes("utf-8")), decodeBuffer);
    }


    @Test
    public void decodeEmpty() throws Exception {
        Assert.assertNull(this.codecFactory.getDecoder().decode(null, null));
        Assert.assertEquals(IoBuffer.allocate(0), this.codecFactory.getDecoder().decode(IoBuffer.allocate(0), null));
    }


    @Test
    public void testDirectEncoder() throws Exception {
        this.codecFactory = new ByteBufferCodecFactory(true);
        IoBuffer msg = IoBuffer.allocate(100);
        IoBuffer buffer = codecFactory.getEncoder().encode(msg, null);
        Assert.assertTrue(buffer.isDirect());
    }

}