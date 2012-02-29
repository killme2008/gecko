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
package com.taobao.gecko.service.notify.request;

import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.util.OpaqueGenerator;
import com.taobao.gecko.service.notify.OpCode;


/**
 * 
 * ÐÄÌøÃüÁî
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-18 ÏÂÎç03:26:36
 */

public class NotifyHeartBeatCommand extends NotifyRequestCommand implements HeartBeatRequestCommand {
    static final long serialVersionUID = -98010017355L;


    public NotifyHeartBeatCommand() {
        this.opCode = OpCode.HEARTBEAT;
        this.opaque = OpaqueGenerator.getNextOpaque();
    }


    public NotifyHeartBeatCommand(final OpCode opCode) {
        super(opCode);
    }


    public void decodeContent() {
        // no header and no body
    }


    public void encodeContent() {
        // no header and no body

    }

}