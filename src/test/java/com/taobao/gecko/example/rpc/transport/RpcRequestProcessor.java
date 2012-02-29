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
package com.taobao.gecko.example.rpc.transport;

import java.util.concurrent.ThreadPoolExecutor;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.example.rpc.command.RpcRequest;
import com.taobao.gecko.example.rpc.command.RpcResponse;
import com.taobao.gecko.example.rpc.server.BeanLocator;
import com.taobao.gecko.example.rpc.server.RpcSkeleton;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RequestProcessor;


public class RpcRequestProcessor implements RequestProcessor<RpcRequest> {
    private final ThreadPoolExecutor executor;
    private final BeanLocator beanLocator;


    public RpcRequestProcessor(ThreadPoolExecutor executor, BeanLocator beanLocator) {
        super();
        this.executor = executor;
        this.beanLocator = beanLocator;
    }


    public ThreadPoolExecutor getExecutor() {
        return this.executor;
    }


    public void handleRequest(RpcRequest request, Connection conn) {
        Object bean = this.beanLocator.getBean(request.getBeanName());
        if (bean == null) {
            throw new RuntimeException("Could not find bean named " + request.getBeanName());
        }
        RpcSkeleton skeleton = new RpcSkeleton(request.getBeanName(), bean);
        Object result = skeleton.invoke(request.getMethodName(), request.getArguments());
        try {
            conn.response(new RpcResponse(request.getOpaque(), ResponseStatus.NO_ERROR, result));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}