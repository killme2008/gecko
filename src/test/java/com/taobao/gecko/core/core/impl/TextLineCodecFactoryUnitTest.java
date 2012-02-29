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
 * @since 1.0, 2009-12-24 ÉÏÎç10:33:59
 */

public class TextLineCodecFactoryUnitTest {
    TextLineCodecFactory textLineCodecFactory;


    @Before
    public void setUp() {
        this.textLineCodecFactory = new TextLineCodecFactory();
    }


    @Test
    public void testEncodeNormal() throws Exception {
        Encoder encoder = this.textLineCodecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode("hello", null);
        Assert.assertNotNull(buffer);
        Assert.assertTrue(buffer.hasRemaining());
        Assert.assertArrayEquals("hello\r\n".getBytes("utf-8"), buffer.array());

    }


    @Test
    public void testEncodeEmpty() throws Exception {
        Encoder encoder = this.textLineCodecFactory.getEncoder();
        Assert.assertNull(encoder.encode(null, null));
        Assert.assertEquals(TextLineCodecFactory.SPLIT, encoder.encode("", null));
    }


    @Test
    public void decodeNormal() throws Exception {
        Encoder encoder = this.textLineCodecFactory.getEncoder();
        Assert.assertNotNull(encoder);
        IoBuffer buffer = encoder.encode("hello", null);

        String str = (String) this.textLineCodecFactory.getDecoder().decode(buffer, null);
        Assert.assertEquals("hello", str);
    }


    @Test
    public void decodeEmpty() throws Exception {
        Assert.assertNull(this.textLineCodecFactory.getDecoder().decode(null, null));
        Assert.assertEquals("", this.textLineCodecFactory.getDecoder().decode(TextLineCodecFactory.SPLIT, null));
    }

}