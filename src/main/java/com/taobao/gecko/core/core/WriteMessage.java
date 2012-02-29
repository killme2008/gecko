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

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import com.taobao.gecko.core.core.impl.FutureImpl;


/**
 * 发送消息包装类
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:02:37
 */
public interface WriteMessage {

    void writing();


    boolean isWriting();


    Object getMessage();


    public boolean hasRemaining();


    public long remaining();


    public long write(WritableByteChannel channel) throws IOException;


    FutureImpl<Boolean> getWriteFuture();

}