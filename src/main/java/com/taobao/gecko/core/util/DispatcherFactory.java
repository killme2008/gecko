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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

import com.taobao.gecko.core.core.Dispatcher;
import com.taobao.gecko.core.core.impl.PoolDispatcher;


public class DispatcherFactory {
    public static Dispatcher newDispatcher(final int size, final RejectedExecutionHandler rejectedExecutionHandler) {
        if (size > 0) {
            return new PoolDispatcher(size, 60, TimeUnit.SECONDS, rejectedExecutionHandler);
        }
        else {
            return null;
        }
    }


    public static Dispatcher newDispatcher(final int size, final String prefix,
            final RejectedExecutionHandler rejectedExecutionHandler) {
        if (size > 0) {
            return new PoolDispatcher(size, 60, TimeUnit.SECONDS, prefix, rejectedExecutionHandler);
        }
        else {
            return null;
        }
    }

}