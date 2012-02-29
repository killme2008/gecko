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
package com.taobao.gecko.core.command;

import com.taobao.gecko.service.impl.DefaultConnection;
import com.taobao.gecko.service.impl.DefaultRemotingClient;
import com.taobao.gecko.service.impl.DefaultRemotingContext;


/**
 * 常量值
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:54:39
 */
public class Constants {

    /**
     * 默认分组名
     */
    public static final String DEFAULT_GROUP = DefaultRemotingContext.class.getSimpleName()
            + "_Notify_Default_Group_Name";
    // 连接数属性
    public static final String CONNECTION_COUNT_ATTR = DefaultRemotingClient.class.getName()
            + "_Notify_Remoting_ConnCount";

    public static final String GROUP_CONNECTION_READY_LOCK = DefaultRemotingClient.class.getName()
            + "_Notify_Remoting_Group_Ready_Lock";
    public static final byte TRUE = 0x01;
    public static final byte FALSE = 0x00;
    public static final String CONNECTION_ATTR = DefaultConnection.class.getName() + "_Notify_Remoting_Context";

}