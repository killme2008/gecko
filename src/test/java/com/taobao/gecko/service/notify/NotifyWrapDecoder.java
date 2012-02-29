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
import com.taobao.gecko.core.util.RemotingUtils;


/**
 * 
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:52:51
 */
public class NotifyWrapDecoder implements CodecFactory.Decoder {
    private final RequestCommandDecoder requestDecoder;
    private final ResponseCommandDecoder responseDecoder;


    public NotifyWrapDecoder() {
        this.requestDecoder = new RequestCommandDecoder();
        this.responseDecoder = new ResponseCommandDecoder();
    }


    public RequestCommandDecoder getRequestDecoder() {
        return this.requestDecoder;
    }


    public ResponseCommandDecoder getResponseDecoder() {
        return this.responseDecoder;
    }

    public static final String DECODER_STATE_KEY = NotifyWrapDecoder.class.getName() + ".STATE";
    public static final String CURRENT_DECODER = NotifyWrapDecoder.class.getName() + ".DECODER";


    public Object decode(final IoBuffer buff, final Session session) {
        if (!buff.hasRemaining()) {
            return null;
        }
        final DecoderState decoderState = this.getDecoderStateFromSession(session);
        if (decoderState.decodeCommand == null) {
            return this.decodeNewCommand(buff, session);
        }
        else {
            return this.decodeCurrentCommand(buff, session);
        }

    }


    private Object decodeCurrentCommand(final IoBuffer buff, final Session session) {
        return ((CodecFactory.Decoder) session.getAttribute(CURRENT_DECODER)).decode(buff, session);
    }


    private Object decodeNewCommand(final IoBuffer buff, final Session session) {
        final byte magic = buff.get(buff.position());
        if (magic == Constants.REQUEST_MAGIC) {
            return this.decodeRequestCommand(buff, session);
        }
        else if (magic == Constants.RESPONSE_MAGIC) {
            return this.decodeResponseCommand(buff, session);
        }
        else {
            throw new RuntimeException("Unknow command magic " + magic + " Buffer: "
                    + RemotingUtils.dumpBuffer(buff).toString());
        }
    }


    private Object decodeResponseCommand(final IoBuffer buff, final Session session) {
        session.setAttribute(CURRENT_DECODER, this.responseDecoder);
        return this.responseDecoder.decode(buff, session);
    }


    private Object decodeRequestCommand(final IoBuffer buff, final Session session) {
        session.setAttribute(CURRENT_DECODER, this.requestDecoder);
        return this.requestDecoder.decode(buff, session);
    }


    /**
     * 从连接属性中获取当前的decode状态，如果不存在就创建
     * 
     * @param session
     * @return
     */
    private DecoderState getDecoderStateFromSession(final Session session) {
        DecoderState decoderState = (DecoderState) session.getAttribute(DECODER_STATE_KEY);
        if (decoderState == null) {
            decoderState = new DecoderState();
            final DecoderState oldState =
                    (DecoderState) session.setAttributeIfAbsent(NotifyWrapDecoder.DECODER_STATE_KEY, decoderState);
            if (oldState != null) {
                decoderState = oldState;
            }
        }
        return decoderState;
    }
}