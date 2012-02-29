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
package com.taobao.gecko.core.util;

import java.lang.reflect.Method;


/**
 * 用于JMX注册的工具类
 * 
 * @author boyan(boyan@taobao.com)
 * @date 2011-11-1
 * 
 */
public class MBeanUtils {
    public static void registerMBeanWithIdPrefix(final Object o, final String idPrefix) {
        boolean registered = false;
        // 优先注册到notify的MBeanServer上
        try {
            final Class<?> clazz = Class.forName(" com.taobao.notify.utils.MyMBeanServer");
            final Method getInstance = clazz.getMethod("getInstance");
            if (getInstance != null) {
                final Object mbs = getInstance.invoke(null);
                final Method registerMethod = clazz.getMethod("registerMBeanWithIdPrefix", Object.class, String.class);
                if (mbs != null && registerMethod != null) {
                    registerMethod.invoke(mbs, o, idPrefix);
                    registered = true;
                }
            }

        }
        catch (final Throwable e) {
            // ignore

        }
        if (!registered) {
            MyMBeanServer.getInstance().registerMBeanWithIdPrefix(o, idPrefix);
        }
    }
}