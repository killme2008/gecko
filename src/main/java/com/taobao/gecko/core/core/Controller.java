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

import com.taobao.gecko.core.statistics.Statistics;


/**
 * 网络层主控接口
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:57:49
 */
public interface Controller {

    long getSessionTimeout();


    public long getSessionIdleTimeout();


    public void setSessionIdleTimeout(long sessionIdleTimeout);


    void setSessionTimeout(long sessionTimeout);


    int getSoTimeout();


    void setSoTimeout(int timeout);


    void addStateListener(ControllerStateListener listener);


    public void removeStateListener(ControllerStateListener listener);


    boolean isHandleReadWriteConcurrently();


    void setHandleReadWriteConcurrently(boolean handleReadWriteConcurrently);


    int getReadThreadCount();


    void setReadThreadCount(int readThreadCount);


    Handler getHandler();


    void setHandler(Handler handler);


    int getPort();


    void start() throws IOException;


    boolean isStarted();


    Statistics getStatistics();


    CodecFactory getCodecFactory();


    void setCodecFactory(CodecFactory codecFactory);


    void stop() throws IOException;


    void setReceiveThroughputLimit(double receivePacketRate);


    double getReceiveThroughputLimit();


    InetSocketAddress getLocalSocketAddress();


    void setLocalSocketAddress(InetSocketAddress inetAddress);


    int getDispatchMessageThreadCount();


    void setDispatchMessageThreadCount(int dispatchMessageThreadPoolSize);


    int getWriteThreadCount();


    void setWriteThreadCount(int writeThreadCount);


    <T> void setSocketOption(SocketOption<T> socketOption, T value);

}