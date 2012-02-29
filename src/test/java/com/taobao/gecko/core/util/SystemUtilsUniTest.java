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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.channels.Selector;

import org.junit.Test;


public class SystemUtilsUniTest {
    @Test
    public void testOpenSelector() throws IOException {
        Selector selector = SystemUtils.openSelector();
        assertNotNull(selector);
        assertTrue(selector.isOpen());
        if (SystemUtils.isLinuxPlatform()) {
            assertEquals(selector.provider().getClass().getCanonicalName(), "sun.nio.ch.EPollSelectorProvider");
        }
        Selector selector2 = SystemUtils.openSelector();
        assertNotSame(selector, selector2);
        selector.close();
        selector2.close();
    }


    @Test
    public void testSystemThreadCount() {
        int cpus = Runtime.getRuntime().availableProcessors();
        int n = SystemUtils.getSystemThreadCount();

        if (cpus == 1) {
            assertEquals(n, 1);
        }
        else {
            assertEquals(n, cpus - 1);
        }
    }
}