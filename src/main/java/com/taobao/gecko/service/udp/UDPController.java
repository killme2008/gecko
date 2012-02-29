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
package com.taobao.gecko.service.udp;

import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * UDP服务控制器
 * 
 * @author boyan
 * @Date 2010-8-26
 * 
 */
public interface UDPController {
    /**
     * 启动服务
     * 
     * @throws NotifyRemotingException
     */
    public void start() throws NotifyRemotingException;


    /**
     * 关闭服务
     * 
     * @throws NotifyRemotingException
     */
    public void stop() throws NotifyRemotingException;


    /**
     * 返回处理器
     * 
     * @return
     */
    public UDPServiceHandler getUDPServiceHandler();


    public boolean isStarted();
}