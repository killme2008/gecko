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
package com.taobao.gecko.example.rpc.client;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.example.rpc.exception.RpcRuntimeException;
import com.taobao.gecko.example.rpc.transport.RpcWireFormatType;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;


public class RpcProxyFactory {
    private final RemotingClient remotingClient;


    public RpcProxyFactory() throws IOException {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new RpcWireFormatType());
        this.remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        try {
            this.remotingClient.start();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }


    @SuppressWarnings("unchecked")
    public <T> T proxyRemote(final String uri, final String beanName, Class<T> serviceClass) throws IOException,
            InterruptedException {
        try {
            this.remotingClient.connect(uri);
            this.remotingClient.awaitReadyInterrupt(uri);
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }

        return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class<?>[] { serviceClass }, new InvocationHandler() {

                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    RpcRequest request = new RpcRequest(beanName, method.getName(), args);
                    RpcResponse response = null;
                    try {
                        response = (RpcResponse) RpcProxyFactory.this.remotingClient.invokeToGroup(uri, request);
                    }
                    catch (Exception e) {
                        throw new RpcRuntimeException("Rpc failure", e);
                    }
                    if (response == null) {
                        throw new RpcRuntimeException("Rpc failure,no response from rpc server");
                    }
                    if (response.getResponseStatus() == ResponseStatus.NO_ERROR) {
                        return response.getResult();
                    }
                    else {
                        throw new RpcRuntimeException("Rpc failure:" + response.getErrorMsg());
                    }
                }
            });

    }
}