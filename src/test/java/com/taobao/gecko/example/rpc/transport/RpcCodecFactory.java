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

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.example.rpc.command.RpcCommand;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.example.rpc.transport.RpcWireFormatType.RpcHeartBeatCommand;


/**
 * @author boyan
 * @Date 2011-2-17
 * 
 */
public class RpcCodecFactory implements CodecFactory {
    static final byte REQ_MAGIC = (byte) 0x70;
    static final byte RESP_MAGIC = (byte) 0x71;

    static final class RpcDecoder implements Decoder {

        private static final String CURRENT_COMMAND = "CurrentCommand";


        public Object decode(IoBuffer buff, Session session) {
            if (!buff.hasRemaining()) {
                return null;
            }
            RpcCommand command = (RpcCommand) session.getAttribute(CURRENT_COMMAND);
            if (command != null) {
                if (command.decode(buff)) {
                    session.removeAttribute(CURRENT_COMMAND);
                    return command;
                }
                else {
                    return null;
                }
            }
            else {
                byte magic = buff.get();
                if (magic == REQ_MAGIC) {
                    command = new RpcRequest();
                }
                else {
                    command = new RpcResponse();

                }
                if (command.decode(buff)) {
                    return command;
                }
                else {
                    session.setAttribute(CURRENT_COMMAND, command);
                    return null;
                }
            }
        }

    }

    static final class RpcEncoder implements Encoder {

        public IoBuffer encode(Object message, Session session) {
            if (message instanceof RpcHeartBeatCommand) {
                return ((RpcHeartBeatCommand) message).request.encode();
            }
            return ((RpcCommand) message).encode();
        }

    }


    public Decoder getDecoder() {
        return new RpcDecoder();
    }


    public Encoder getEncoder() {
        return new RpcEncoder();
    }

}