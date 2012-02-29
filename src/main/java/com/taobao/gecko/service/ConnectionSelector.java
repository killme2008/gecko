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
package com.taobao.gecko.service;

import java.util.List;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 选择连接策略
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午05:00:54
 */

public interface ConnectionSelector {
    /**
     * 从分组的连接列表中选择想要的连接
     * 
     * @param targetGroup
     *            分组名称
     * @param request
     *            请求命令
     * @param connectionList
     *            分组的连接列表
     * @return
     */
    public Connection select(String targetGroup, RequestCommand request, List<Connection> connectionList)
            throws NotifyRemotingException;
}