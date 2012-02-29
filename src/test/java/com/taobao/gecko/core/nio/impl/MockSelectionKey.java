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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;


/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-24 ÏÂÎç02:01:20
 */

public class MockSelectionKey extends SelectionKey {
    MockSelectableChannel channel;
    int interestOps;
    boolean valid = true;
    Selector selector;


    @Override
    public void cancel() {
        this.valid = false;

    }


    @Override
    public SelectableChannel channel() {
        return this.channel;
    }


    @Override
    public int interestOps() {
        return interestOps;
    }


    @Override
    public SelectionKey interestOps(int ops) {
        this.interestOps = ops;
        return this;
    }


    @Override
    public boolean isValid() {
        return valid;
    }


    @Override
    public int readyOps() {
        return this.interestOps;
    }


    @Override
    public Selector selector() {
        return this.selector;
    }

}