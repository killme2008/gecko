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
package com.taobao.gecko.example.rpc.transport;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.service.config.WireFormatType;


public class RpcWireFormatType extends WireFormatType {

    public static final class RpcHeartBeatCommand implements HeartBeatRequestCommand {
        public RpcRequest request = new RpcRequest("heartBeat" + System.currentTimeMillis(), "heartBeat"
                + System.currentTimeMillis(), null);


        public CommandHeader getRequestHeader() {
            return this.request;
        }


        public Integer getOpaque() {
            return this.request.getOpaque();
        }
    }


    @Override
    public String getScheme() {
        return "rpc";
    }


    @Override
    public String name() {
        return "Notify Remoting rpc";
    }


    @Override
    public CodecFactory newCodecFactory() {
        return new RpcCodecFactory();
    }


    @Override
    public CommandFactory newCommandFactory() {
        return new CommandFactory() {

            public HeartBeatRequestCommand createHeartBeatCommand() {
                return new RpcHeartBeatCommand();
            }


            public BooleanAckCommand createBooleanAckCommand(final CommandHeader request,
                    final ResponseStatus responseStatus, final String errorMsg) {
                final BooleanAckCommand ack = new RpcResponse(request.getOpaque(), responseStatus, null) {

                    @Override
                    public boolean isBoolean() {
                        return true;
                    }

                };
                ack.setErrorMsg(errorMsg);
                return ack;
            }
        };
    }

}