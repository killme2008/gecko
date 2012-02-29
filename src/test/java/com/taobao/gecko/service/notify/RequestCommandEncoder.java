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
import com.taobao.gecko.service.impl.DefaultConnection;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;


/**
 * 
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:54:09
 */
public class RequestCommandEncoder implements CodecFactory.Encoder {
    static final IoBuffer EMPTY = IoBuffer.allocate(0);


    public IoBuffer encode(final Object message, final Session session) {
        if (message instanceof NotifyRequestCommand) {
            final NotifyRequestCommand requestCommand = (NotifyRequestCommand) message;
            try {
                requestCommand.encodeContent();
                final IoBuffer buffer =
                        IoBuffer.allocate(Constants.REQUEST_HEADER_LENGTH + requestCommand.getTotalBodyLength());
                buffer.setAutoExpand(true);
                this.putHeader(message, requestCommand, buffer);
                this.putContent(message, requestCommand, buffer);
                buffer.flip();
                return buffer;
            }
            catch (final Exception e) {
                session.getHandler().onExceptionCaught(session, e);
                // 捕捉mashall异常，返回给用户
                final DefaultConnection conn =
                        (DefaultConnection) session
                            .getAttribute(com.taobao.gecko.core.command.Constants.CONNECTION_ATTR);
                if (conn != null) {
                    conn.notifyClientException(requestCommand, e);
                }
                // 最后返回一个空buffer
                return EMPTY.slice();
            }

        }
        else {
            throw new RuntimeException("Illegal request message," + message);
        }
    }


    private void putContent(final Object message, final NotifyRequestCommand requestCommand, final IoBuffer buffer) {
        if (requestCommand.getHeaderLength() > 0) {
            if (requestCommand.getHeader() == null) {
                throw new IllegalArgumentException("Illegal request header," + message);
            }
            buffer.put(requestCommand.getHeader());
        }
        if (requestCommand.getTotalBodyLength() - requestCommand.getHeaderLength() > 0) {
            if (requestCommand.getBody() == null) {
                throw new IllegalArgumentException("Illegal request body," + message);
            }
            buffer.put(requestCommand.getBody());
        }
    }


    private void putHeader(final Object message, final NotifyRequestCommand requestCommand, final IoBuffer buffer) {
        buffer.put(requestCommand.getMagic());
        buffer.put(requestCommand.getOpCode().getValue());
        buffer.putShort(requestCommand.getHeaderLength());
        buffer.putInt(requestCommand.getTotalBodyLength());
        buffer.putInt(requestCommand.getOpaque());
    }
}