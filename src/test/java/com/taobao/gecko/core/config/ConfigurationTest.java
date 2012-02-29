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
package com.taobao.gecko.core.config;

import junit.framework.TestCase;


public class ConfigurationTest extends TestCase {
    Configuration configuration;


    @Override
    protected void setUp() throws Exception {
        this.configuration = new Configuration();
    }


    public void testDefaultConfig() {
        assertTrue(this.configuration.isHandleReadWriteConcurrently());

        assertEquals(32 * 1024, this.configuration.getSessionReadBufferSize());
        assertEquals(0, this.configuration.getSoTimeout());

        assertEquals(0, this.configuration.getReadThreadCount());
        assertEquals(1000, this.configuration.getCheckSessionTimeoutInterval());
        assertEquals(5 * 60 * 1000, this.configuration.getStatisticsInterval());
        assertFalse(this.configuration.isStatisticsServer());
        assertEquals(5000L, this.configuration.getSessionIdleTimeout());

        this.configuration.setSessionReadBufferSize(8 * 1024);
        assertEquals(8 * 1024, this.configuration.getSessionReadBufferSize());
        try {
            this.configuration.setSessionReadBufferSize(0);
            fail();
        }
        catch (IllegalArgumentException e) {

        }
        this.configuration.setReadThreadCount(11);
        assertEquals(11, this.configuration.getReadThreadCount());
        try {
            this.configuration.setReadThreadCount(-10);
            fail();
        }
        catch (IllegalArgumentException e) {

        }

        this.configuration.setSoTimeout(1000);
        assertEquals(1000, this.configuration.getSoTimeout());
        this.configuration.setSoTimeout(0);
        assertEquals(0, this.configuration.getSoTimeout());
        try {
            this.configuration.setSoTimeout(-1000);
            fail();
        }
        catch (IllegalArgumentException e) {

        }

    }

}