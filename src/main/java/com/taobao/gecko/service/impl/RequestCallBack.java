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

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.service.Connection;


/**
 * 
 * 
 * 请求回调的公共接口
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午01:13:41
 */

public interface RequestCallBack {

    /**
     * 判断回调是否过期
     * 
     * @param now
     *            当前时间
     * @return
     */
    public boolean isInvalid(long now);


    /**
     * 当响应到达的时，触发此方法
     * 
     * @param group
     *            应答的分组名
     * @param responseCommand
     *            应答命令
     * @param connection
     *            应答的连接
     */
    public void onResponse(String group, ResponseCommand responseCommand, Connection connection);


    /**
     * 设置异常
     * 
     * @param e
     * @param conn
     * @param requestCommand
     */
    public void setException(Exception e, Connection conn, RequestCommand requestCommand);


    public void dispose();
}