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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.service.notify.response.NotifyBooleanAckCommand;
import com.taobao.gecko.service.notify.response.NotifyResponseCommand;


/**
 * 
 * 应答命令解码器
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:53:55
 */

public class ResponseCommandDecoder implements CodecFactory.Decoder {

    private static final Log log = LogFactory.getLog(ResponseCommandDecoder.class);


    public Object decode(final IoBuffer in, final Session session) {
        final DecoderState decoderState = (DecoderState) session.getAttribute(NotifyWrapDecoder.DECODER_STATE_KEY);
        if (decoderState.decodeCommand == null) {
            if (in.remaining() < Constants.RESPONSE_HEADER_LENGTH) {
                return null;
            }
            else {
                this.decodeHeader(in, session, decoderState);
            }
        }
        if (decoderState.decodeCommand != null) {
            final NotifyResponseCommand responseCommand = (NotifyResponseCommand) decoderState.decodeCommand;
            if (in.remaining() < responseCommand.getTotalBodyLength()) {
                return null;
            }
            else {
                return this.decodeContent(in, decoderState, responseCommand);
            }
        }
        return null;

    }


    private Object decodeContent(final IoBuffer in, final DecoderState decoderState,
            final NotifyResponseCommand responseCommand) {
        if (responseCommand.getTotalBodyLength() > 0) {
            if (responseCommand.getHeaderLength() > 0) {
                final byte[] header = new byte[responseCommand.getHeaderLength()];
                in.get(header);
                responseCommand.setHeader(header);
            }
            final int bodyLen = responseCommand.getTotalBodyLength() - responseCommand.getHeaderLength();
            if (bodyLen > 0) {
                final byte[] body = new byte[bodyLen];
                in.get(body);
                responseCommand.setBody(body);
            }
            responseCommand.decodeContent();
        }
        decoderState.decodeCommand = null;// reset status
        return responseCommand;
    }


    private void decodeHeader(final IoBuffer in, final Session session, final DecoderState decoderState) {
        final byte magic = in.get();
        if (magic != Constants.RESPONSE_MAGIC) {
            log.error("应答命令的magic数值错误,expect " + Constants.RESPONSE_MAGIC + ",real " + magic);
            session.close();
            return;
        }

        final OpCode opCode = OpCode.valueOf(in.get());
        final ResponseStatus responseStatus = ResponseStatusCode.valueOf(in.getShort());
        NotifyResponseCommand responseCommand = null;
        if (responseStatus == ResponseStatus.NO_ERROR) {
            responseCommand = (NotifyResponseCommand) NotifyCommandFactory.newResponseCommand(opCode);
        }
        else {
            responseCommand = new NotifyBooleanAckCommand(opCode);
        }
        responseCommand.setResponseHost(session.getRemoteSocketAddress());
        responseCommand.setOpCode(opCode);
        responseCommand.setResponseStatus(responseStatus);
        responseCommand.setHeaderLength(in.getShort());
        // skip reserved field
        in.skip(2);
        responseCommand.setTotalBodyLength(in.getInt());
        responseCommand.setOpaque(in.getInt());
        decoderState.decodeCommand = responseCommand;
    }

}