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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Session;
import com.taobao.gecko.core.util.ByteBufferMatcher;
import com.taobao.gecko.core.util.ShiftAndByteBufferMatcher;


/**
 * 编解码工厂的一个实现，用于文本行协议
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:05:26
 */
public class TextLineCodecFactory implements CodecFactory {

    public static final IoBuffer SPLIT = IoBuffer.wrap("\r\n".getBytes());

    private static final ByteBufferMatcher SPLIT_PATTERN = new ShiftAndByteBufferMatcher(SPLIT);

    public static final String DEFAULT_CHARSET_NAME = "utf-8";

    private final Charset charset;


    public TextLineCodecFactory() {
        this.charset = Charset.forName(DEFAULT_CHARSET_NAME);
    }


    public TextLineCodecFactory(final String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

    class StringDecoder implements CodecFactory.Decoder {
        public Object decode(final IoBuffer buffer, final Session session) {
            String result = null;
            final int index = SPLIT_PATTERN.matchFirst(buffer);
            if (index >= 0) {
                final int limit = buffer.limit();
                buffer.limit(index);
                final CharBuffer charBuffer = TextLineCodecFactory.this.charset.decode(buffer.buf());
                result = charBuffer.toString();
                buffer.limit(limit);
                buffer.position(index + SPLIT.remaining());

            }
            return result;
        }
    }

    private final CodecFactory.Decoder decoder = new StringDecoder();


    public Decoder getDecoder() {
        return this.decoder;

    }

    class StringEncoder implements Encoder {
        public IoBuffer encode(final Object msg, final Session session) {
            if (msg == null) {
                return null;
            }
            final String message = (String) msg;
            final ByteBuffer buff = TextLineCodecFactory.this.charset.encode(message);
            final IoBuffer resultBuffer = IoBuffer.allocate(buff.remaining() + SPLIT.remaining());
            resultBuffer.put(buff);
            resultBuffer.put(SPLIT.slice());
            resultBuffer.flip();
            return resultBuffer;
        }
    }

    private final Encoder encoder = new StringEncoder();


    public Encoder getEncoder() {
        return this.encoder;
    }

}