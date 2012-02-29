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
package com.taobao.gecko.core.core;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * 针对服务器的控制接口
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:00:59
 */
public interface ServerController extends Controller {

    public void bind(InetSocketAddress localAddress) throws IOException;


    public void bind(int port) throws IOException;


    public void unbind() throws IOException;

}