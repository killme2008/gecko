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
package com.taobao.gecko.core.util;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-23 ÏÂÎç04:18:39
 */

public class OpaqueGeneratorUnitTest {
    int currentOpaque;


    @Before
    public void setUp() {
        currentOpaque = OpaqueGenerator.getCurrentOpaque();
    }


    @Test
    public void testGetNextValue() {
        OpaqueGenerator.resetOpaque();
        Assert.assertEquals(Integer.MIN_VALUE, OpaqueGenerator.getNextOpaque());

        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(Integer.MIN_VALUE + i + 1, OpaqueGenerator.getNextOpaque());
        }

        // ³¬¹ý·¶Î§£¬reset
        OpaqueGenerator.setOpaque(Integer.MAX_VALUE - 10);
        Assert.assertEquals(Integer.MAX_VALUE - 10, OpaqueGenerator.getCurrentOpaque());
        Assert.assertEquals(Integer.MIN_VALUE, OpaqueGenerator.getNextOpaque());
        for (int i = 0; i < 1000; i++) {
            Assert.assertEquals(Integer.MIN_VALUE + i + 1, OpaqueGenerator.getNextOpaque());
        }
    }


    @After
    public void tearDown() {
        OpaqueGenerator.setOpaque(currentOpaque);
    }

}