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
package com.taobao.gecko.core.core.impl;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.WriteMessage;


/**
 * 写消息毒丸，当写到此消息的时候，连接将关闭
 * 
 * @author boyan
 * @Date 2010-8-14
 * 
 */
public class PoisonWriteMessage implements WriteMessage {
    static IoBuffer EMPTY = IoBuffer.allocate(0);


    public Object getMessage() {

        return null;
    }


    public long remaining() {
        return 0;
    }


    public FutureImpl<Boolean> getWriteFuture() {

        return null;
    }


    public boolean hasRemaining() {
        return false;
    }


    public long write(final WritableByteChannel channel) throws IOException {
        return 0;
    }


    public boolean isWriting() {

        return false;
    }


    public void writing() {

    }

}