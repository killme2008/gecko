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

import java.net.InetSocketAddress;


/**
 * 应答命令公共接口
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:55:15
 */

public interface ResponseCommand extends CommandHeader {
    static final long serialVersionUID = 77788812547386438L;


    /**
     * 返回响应状态
     * 
     * @return
     */
    public ResponseStatus getResponseStatus();


    /**
     * 设置响应状态
     * 
     * @param responseStatus
     */
    public void setResponseStatus(ResponseStatus responseStatus);


    /**
     * 是否为BooleanAckCommand
     * 
     * @return
     */
    public boolean isBoolean();


    /**
     * 返回响应的远端地址
     * 
     * @return
     */
    public InetSocketAddress getResponseHost();


    /**
     * 设置响应的远端地址
     * 
     * @param address
     */
    public void setResponseHost(InetSocketAddress address);


    /**
     * 返回响应的时间戳
     * 
     * @return
     */
    public long getResponseTime();


    /**
     * 设置响应时间戳
     * 
     * @param time
     */
    public void setResponseTime(long time);


    /**
     * 设置响应的opaque
     * 
     * @param opaque
     */
    public void setOpaque(Integer opaque);
}