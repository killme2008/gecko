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



/**
 * 常量值
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:54:39
 */
public class Constants {
    public static final byte REQUEST_MAGIC = (byte) 0x80;
    public static final byte RESPONSE_MAGIC = (byte) 0x81;
    public static final short RESERVED = (short) 0x0000;

    /**
     * 请求头长度
     */
    public static final int REQUEST_HEADER_LENGTH = 12;
    /**
     * 响应头长度
     */
    public static final int RESPONSE_HEADER_LENGTH = 16;
}