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

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;
import com.taobao.gecko.service.notify.request.NotifyHeartBeatCommand;
import com.taobao.gecko.service.notify.response.NotifyBooleanAckCommand;
import com.taobao.gecko.service.notify.response.NotifyDummyAckCommand;


/**
 * 
 * 
 * 协议命令工厂类，任何实现的协议都需要在此工厂注册，提供给编解码器使用
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-18 上午11:13:33
 */

public final class NotifyCommandFactory implements CommandFactory {

    public BooleanAckCommand createBooleanAckCommand(final CommandHeader request,
            final ResponseStatus responseStatus, final String errorMsg) {
        return new NotifyBooleanAckCommand(request, responseStatus, errorMsg);
    }


    public HeartBeatRequestCommand createHeartBeatCommand() {
        return new NotifyHeartBeatCommand();
    }


    public static final ResponseCommand newResponseCommand(final OpCode opCode) {
        ResponseCommand responseCommand = null;
        switch (opCode) {

        case HEARTBEAT:
            responseCommand = new NotifyBooleanAckCommand(opCode);
            break;
        case DUMMY:
            responseCommand = new NotifyDummyAckCommand(opCode);
            break;
        default:
            throw new RuntimeException("Unknow response command for " + opCode.name());
        }
        return responseCommand;
    }


    public static final RequestCommand newRequestCommand(final OpCode opCode) {
        RequestCommand requestCommand = null;
        switch (opCode) {

        case HEARTBEAT:
            requestCommand = new NotifyHeartBeatCommand(opCode);
            break;
        case DUMMY:
            requestCommand = new NotifyDummyRequestCommand(opCode);
            break;

        default:
            throw new RuntimeException("Could not new request command by opCode,opCode=" + opCode);
        }
        return requestCommand;
    }

}