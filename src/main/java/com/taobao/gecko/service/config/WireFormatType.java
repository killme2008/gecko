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
package com.taobao.gecko.service.config;

import java.util.HashMap;
import java.util.Map;

import com.taobao.gecko.core.command.CommandFactory;
import com.taobao.gecko.core.core.CodecFactory;


/**
 * 
 * 
 * wire协议类型，任何想要使用gecko的协议都需要继承此类并实现相应方法
 * 
 * @author boyan
 * 
 * @since 1.0, 2010-1-27 下午05:46:27
 */

public abstract class WireFormatType {
    private static Map<String, WireFormatType> registeredWireFormatType = new HashMap<String, WireFormatType>();


    /**
     * 注册协议类型
     * 
     * @param wireFormatType
     */
    public static void registerWireFormatType(final WireFormatType wireFormatType) {
        if (wireFormatType == null) {
            throw new IllegalArgumentException("Null wire format");
        }
        registeredWireFormatType.put(wireFormatType.name(), wireFormatType);
    }


    /**
     * 取消协议类型的注册
     * 
     * @param wireFormatType
     */
    public static void unregisterWireFormatType(final WireFormatType wireFormatType) {
        if (wireFormatType == null) {
            throw new IllegalArgumentException("Null wire format");
        }
        registeredWireFormatType.remove(wireFormatType.name());
    }


    @Override
    public String toString() {
        return this.name();
    }


    public static WireFormatType valueOf(final String name) {
        return registeredWireFormatType.get(name);

    }


    /**
     * 协议的scheme
     * 
     * @return
     */
    public abstract String getScheme();


    /**
     * 协议的编解码工厂
     * 
     * @return
     */
    public abstract CodecFactory newCodecFactory();


    /**
     * 协议的命令工厂
     * 
     * @return
     */
    public abstract CommandFactory newCommandFactory();


    /**
     * 协议名称
     * 
     * @return
     */
    public abstract String name();
}