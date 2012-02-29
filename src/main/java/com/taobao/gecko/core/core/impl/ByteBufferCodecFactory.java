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
package com.taobao.gecko.core.core.impl;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;


/**
 * 编解码工厂的一个默认实现，直接发送IoBuffer
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:04:22
 */
public class ByteBufferCodecFactory implements CodecFactory {
    static final IoBuffer EMPTY_BUFFER = IoBuffer.allocate(0);

    private final boolean direct;


    public ByteBufferCodecFactory() {
        this(false);
    }


    public ByteBufferCodecFactory(final boolean direct) {
        super();
        this.direct = direct;
        this.encoder = new ByteBufferEncoder();
        this.decoder = new ByteBufferDecoder();
    }

    public class ByteBufferDecoder implements Decoder {

        public Object decode(final IoBuffer buff, final Session session) {
            if (buff == null) {
                return null;
            }
            if (buff.remaining() == 0) {
                return EMPTY_BUFFER;
            }
            final byte[] bytes = new byte[buff.remaining()];
            buff.get(bytes);
            final IoBuffer result = IoBuffer.allocate(bytes.length, ByteBufferCodecFactory.this.direct);
            result.put(bytes);
            result.flip();
            return result;
        }

    }

    private final Decoder decoder;


    public Decoder getDecoder() {
        return this.decoder;
    }

    public class ByteBufferEncoder implements Encoder {

        public IoBuffer encode(final Object message, final Session session) {
            final IoBuffer msgBuffer = (IoBuffer) message;
            if (msgBuffer == null) {
                return null;
            }
            if (msgBuffer.remaining() == 0) {
                return EMPTY_BUFFER;
            }
            final byte[] bytes = new byte[msgBuffer.remaining()];
            msgBuffer.get(bytes);
            final IoBuffer result = IoBuffer.allocate(bytes.length, ByteBufferCodecFactory.this.direct);
            result.put(bytes);
            result.flip();
            return result;
        }

    }

    private final Encoder encoder;


    public Encoder getEncoder() {
        return this.encoder;
    }

}