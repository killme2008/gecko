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
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.service.notify.response.NotifyResponseCommand;


/**
 * 
 * 
 * 
 * Ó¦´ðÃüÁî±àÂëÆ÷
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ÏÂÎç05:54:03
 */
public class ResponseCommandEncoder implements CodecFactory.Encoder {

    public IoBuffer encode(final Object message, final Session session) {
        if (message instanceof NotifyResponseCommand) {
            final NotifyResponseCommand responseCommand = (NotifyResponseCommand) message;
            responseCommand.encodeContent();
            final IoBuffer buffer =
                    IoBuffer.allocate(Constants.RESPONSE_HEADER_LENGTH + responseCommand.getTotalBodyLength());
            buffer.setAutoExpand(true);
            this.putHeader(responseCommand, buffer);
            this.putContent(message, responseCommand, buffer);
            buffer.flip();
            return buffer;

        }
        else {
            throw new RuntimeException("Illegal response message," + message);
        }
    }


    private void putContent(final Object message, final NotifyResponseCommand responseCommand, final IoBuffer buffer) {
        if (responseCommand.getHeaderLength() > 0) {
            if (responseCommand.getHeader() == null) {
                throw new IllegalArgumentException("Illegal response header," + message);
            }
            buffer.put(responseCommand.getHeader());
        }
        if (responseCommand.getTotalBodyLength() - responseCommand.getHeaderLength() > 0) {
            if (responseCommand.getBody() == null) {
                throw new IllegalArgumentException("Illegal response body," + message);
            }
            buffer.put(responseCommand.getBody());
        }
    }


    private void putHeader(final NotifyResponseCommand responseCommand, final IoBuffer buffer) {
        buffer.put(responseCommand.getMagic());
        buffer.put(responseCommand.getOpCode().getValue());
        buffer.putShort(ResponseStatusCode.getValue(responseCommand.getResponseStatus()));
        buffer.putShort(responseCommand.getHeaderLength());
        buffer.putShort(Constants.RESERVED);
        buffer.putInt(responseCommand.getTotalBodyLength());
        buffer.putInt(responseCommand.getOpaque());
    }

}