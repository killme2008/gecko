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
package com.taobao.gecko.core.nio;

import java.nio.channels.SelectableChannel;
import java.util.Queue;

import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Dispatcher;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.SessionConfig;
import com.taobao.gecko.core.core.WriteMessage;
import com.taobao.gecko.core.nio.impl.SelectorManager;
import com.taobao.gecko.core.statistics.Statistics;


/**
 * Nio session配置�?
 * 
 * @author boyan
 * 
 */
public class NioSessionConfig extends SessionConfig {

    public final SelectableChannel selectableChannel;
    public final SelectorManager selectorManager;


    public NioSessionConfig(final SelectableChannel sc, final Handler handler, final SelectorManager reactor,
            final CodecFactory codecFactory, final Statistics statistics, final Queue<WriteMessage> queue,
            final Dispatcher dispatchMessageDispatcher, final boolean handleReadWriteConcurrently,
            final long sessionTimeout, final long sessionIdleTimeout) {
        super(handler, codecFactory, statistics, queue, dispatchMessageDispatcher, handleReadWriteConcurrently,
            sessionTimeout, sessionIdleTimeout);
        this.selectableChannel = sc;
        this.selectorManager = reactor;
    }

}