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
package com.taobao.gecko.core.core;

import java.net.SocketAddress;
import java.util.concurrent.Future;


/**
 * UDP连接抽象
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:02:22
 */
public interface UDPSession extends Session {
    /**
     * Async write message to another end
     * 
     * @param targetAddr
     * @param packet
     * @return future
     */
    public Future<Boolean> asyncWrite(SocketAddress targetAddr, Object packet);


    /**
     * Write message to another end,do not care when the message is written.
     * 
     * @param targetAddr
     * @param packet
     */
    public void write(SocketAddress targetAddr, Object packet);
}