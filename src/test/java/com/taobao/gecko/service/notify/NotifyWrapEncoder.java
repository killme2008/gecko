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
package com.taobao.gecko.service.notify;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;


/**
 * 
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ÏÂÎç05:52:58
 */

public class NotifyWrapEncoder implements CodecFactory.Encoder {
    private final RequestCommandEncoder requestEncoder;
    private final ResponseCommandEncoder responseEncoder;


    public NotifyWrapEncoder() {
        this.requestEncoder = new RequestCommandEncoder();
        this.responseEncoder = new ResponseCommandEncoder();
    }


    public IoBuffer encode(final Object message, final Session session) {
        if (message instanceof RequestCommand) {
            return this.requestEncoder.encode(message, session);
        }
        else if (message instanceof ResponseCommand) {
            return this.responseEncoder.encode(message, session);
        }
        else {
            throw new RuntimeException("Unknow command type " + message.getClass().getName());
        }
    }
}