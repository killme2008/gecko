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
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;


/**
 * 
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:53:03
 */

public class RequestCommandDecoder implements CodecFactory.Decoder {

    private static final Log log = LogFactory.getLog(RequestCommandDecoder.class);


    public Object decode(final IoBuffer in, final Session session) {
        final DecoderState decoderState = (DecoderState) session.getAttribute(NotifyWrapDecoder.DECODER_STATE_KEY);
        if (decoderState.decodeCommand == null) {
            if (in.remaining() < Constants.REQUEST_HEADER_LENGTH) {
                return null;
            }
            else {
                this.decodeHeader(in, session, decoderState);
            }
        }
        if (decoderState.decodeCommand != null) {
            final NotifyRequestCommand requestCommand = (NotifyRequestCommand) decoderState.decodeCommand;
            if (in.remaining() < requestCommand.getTotalBodyLength()) {
                return null;
            }
            else {
                return this.decodeContent(in, decoderState, requestCommand);
            }
        }
        return null;
    }


    private Object decodeContent(final IoBuffer in, final DecoderState decoderState,
            final NotifyRequestCommand requestCommand) {
        if (requestCommand.getTotalBodyLength() > 0) {
            if (requestCommand.getHeaderLength() > 0) {
                final byte[] header = new byte[requestCommand.getHeaderLength()];
                in.get(header);
                requestCommand.setHeader(header);
            }
            final int bodyLen = requestCommand.getTotalBodyLength() - requestCommand.getHeaderLength();
            if (bodyLen > 0) {
                final byte[] body = new byte[bodyLen];
                in.get(body);
                requestCommand.setBody(body);
            }
        }
        requestCommand.decodeContent();
        decoderState.decodeCommand = null;// reset status
        return requestCommand;
    }


    private void decodeHeader(final IoBuffer in, final Session session, final DecoderState decoderState) {
        final byte magic = in.get();
        if (magic != Constants.REQUEST_MAGIC) {
            log.error("请求命令的magic数值错误,expect " + Constants.REQUEST_MAGIC + ",real " + magic);
            session.close();
            return;
        }
        final OpCode opCode = OpCode.valueOf(in.get());
        final NotifyRequestCommand requestCommand =
                (NotifyRequestCommand) NotifyCommandFactory.newRequestCommand(opCode);
        requestCommand.setOpCode(opCode);
        requestCommand.setHeaderLength(in.getShort());
        requestCommand.setTotalBodyLength(in.getInt());
        requestCommand.setOpaque(in.getInt());
        decoderState.decodeCommand = requestCommand;
    }
}