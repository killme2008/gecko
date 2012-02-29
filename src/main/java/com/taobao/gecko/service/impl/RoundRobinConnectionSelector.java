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
package com.taobao.gecko.service.impl;

import java.util.List;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.util.PositiveAtomicCounter;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionSelector;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * Round robin的连接选择器
 * 
 * @author boyan
 * @Date 2010-9-10
 * 
 */
public class RoundRobinConnectionSelector implements ConnectionSelector {
    private final PositiveAtomicCounter sets = new PositiveAtomicCounter();

    private static int MAX_TIMES = 5;


    public Connection select(final String targetGroup, final RequestCommand request,
            final List<Connection> connectionList) throws NotifyRemotingException {
        try {
            if (connectionList == null) {
                return null;
            }
            final int size = connectionList.size();
            if (size == 0) {
                return null;
            }
            Connection result = connectionList.get(this.sets.incrementAndGet() % size);
            int tries = 0;
            while ((result == null || !result.isConnected()) && tries++ < MAX_TIMES) {
                result = connectionList.get(this.sets.incrementAndGet() % size);
            }
            if (result != null && !result.isConnected()) {
                return null;
            }
            return result;
        }
        catch (final Throwable e) {
            throw new NotifyRemotingException(e);
        }
    }

}