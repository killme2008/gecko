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
package com.taobao.gecko.example.rpc.example.client;

import com.taobao.gecko.example.rpc.client.RpcProxyFactory;
import com.taobao.gecko.example.rpc.example.Hello;
import com.taobao.gecko.example.rpc.example.server.HelloImpl;


public class HelloClient {
    public static void main(String[] args) throws Exception {
        RpcProxyFactory factory = new RpcProxyFactory();
        Hello hello = factory.proxyRemote("rpc://localhost:8080", "hello", Hello.class);
        System.out.println(hello.sayHello("×¯Ïþµ¤", 10000));
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            hello.add(1, 300);
        }
        System.out.println(System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        HelloImpl helloImpl=new HelloImpl();
        for (int i = 0; i < 10000; i++) {
            helloImpl.add(1, 300);
        }
        System.out.println(System.currentTimeMillis() - start);
        System.out.println(hello.getDate());
    }
}