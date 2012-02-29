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
package com.taobao.gecko.example.rpc.server;

import java.lang.reflect.Method;


public class RpcSkeleton {
    private final Object bean;
    private final String beanName;


    public RpcSkeleton(String beanName, Object bean) {
        super();
        this.beanName = beanName;
        this.bean = bean;
    }


    public Object invoke(String methodName, Object[] args) {
        Method method = this.getMethod(methodName, args);
        if (method == null) {
            throw new RuntimeException("Unknow method in " + this.beanName + ":" + methodName);
        }
        try {
            return method.invoke(this.bean, args);
        }
        catch (Exception e) {
            throw new RuntimeException("Invoke " + this.beanName + "." + methodName + " failure", e);
        }
    }


    private Method getMethod(String methodName, Object[] args) {
        Method method = null;
        Class<?> clazz = this.bean.getClass();
        try {
            if (args != null && args.length > 0) {
                Class<?>[] parameterTypes = new Class<?>[args.length];
                for (int i = 0; i < args.length; i++) {
                    parameterTypes[i] = args[i] != null ? args[i].getClass() : null;
                }
                method = clazz.getMethod(methodName, parameterTypes);
            }
            else {
                method = clazz.getMethod(methodName);
            }

        }
        catch (NoSuchMethodException e) {
        }
        if (method == null) {
            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (m.getName().equals(methodName) && m.getParameterTypes().length == args.length) {
                    method = m;
                    break;
                }
            }
        }
        return method;
    }
}