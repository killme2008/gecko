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
import java.util.Random;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionSelector;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 连接选择器随机策略
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午05:01:45
 */

public class RandomConnectionSelector implements ConnectionSelector {

    /**
     * 最大重试次数
     */
    private static final int MAX_TIMES = 5;
    private final Random rand = new Random();


    /**
     * 这里的connectionList未做拷贝保护是基于性能考虑，如果select失败，也是抛出Runtime异常
     */
    public final Connection select(final String targetGroup, final RequestCommand request,
            final List<Connection> connectionList) throws NotifyRemotingException {
        try {
            if (connectionList == null) {
                return null;
            }
            final int size = connectionList.size();
            if (size == 0) {
                return null;
            }
            Connection result = connectionList.get(this.rand.nextInt(size));
            int tries = 0;
            while ((result == null || !result.isConnected()) && tries++ < MAX_TIMES) {
                result = connectionList.get(this.rand.nextInt(size));
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