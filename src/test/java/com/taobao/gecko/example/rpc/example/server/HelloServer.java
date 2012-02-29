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
package com.taobao.gecko.example.rpc.example.server;

import java.net.InetSocketAddress;

import com.taobao.gecko.example.rpc.server.BeanLocator;
import com.taobao.gecko.example.rpc.server.RpcServer;


public class HelloServer {
    public static void main(String[] args) throws Exception{
        RpcServer rpcServer = new RpcServer();
        rpcServer.bind(new BeanLocator() {

            public Object getBean(String name) {
                if (name.equals("hello")) {
                    return new HelloImpl();
                }
                return null;
            }
        }, new InetSocketAddress(8080));
    }
}