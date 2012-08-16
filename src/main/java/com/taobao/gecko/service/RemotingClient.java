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
package com.taobao.gecko.service;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * Notify Remoting的客户端接口
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午02:04:38
 */

public interface RemotingClient extends RemotingController {
    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式
     * 
     * @param group
     *            服务端的URL，形如schema://host:port的字符串
     * @throws IOException
     */
    public void connect(String url) throws NotifyRemotingException;


    /**
     * 等待连接就绪，可中断，连接就绪的含义如下：是指指定分组的有效连接数达到设定值，并且可用。默认等待超时为连接数乘以连接超时
     * 
     * @param group
     * @throws NotifyRemotingException
     * @throws InterruptedException
     */
    public void awaitReadyInterrupt(String url) throws NotifyRemotingException, InterruptedException;


    /**
     * 等待连接就绪，可中断，连接就绪的含义如下：是指指定分组的有效连接数达到设定值，并且可用。默认等待超时为连接数乘以连接超时
     * 
     * @param group
     * @throws NotifyRemotingException
     * @throws InterruptedException
     */
    public void awaitReadyInterrupt(String url, long time) throws NotifyRemotingException, InterruptedException;


    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式
     * 
     * @param url
     *            服务端的URL，形如schema://host:port的字符串
     * @throws IOException
     */
    public void connect(String url, int connCount) throws NotifyRemotingException;


    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式，但是连接加入的分组将为target group。
     * 
     * @param url
     *            服务端的URL，形如schema://host:port的字符串
     * @param targetGroup
     *            连接成功后加入的分组
     * @param connCount
     *            连接数
     * @throws IOException
     */
    public void connect(String url, String targetGroup, int connCount) throws NotifyRemotingException;


    /**
     * 根据URL连接服务端，如果连接失败将转入重连模式，但是连接加入的分组将为target group,连接数默认为1
     * 
     * @param url
     * @param targetGroup
     * @throws NotifyRemotingException
     */
    public void connect(String url, String targetGroup) throws NotifyRemotingException;


    /**
     * 获取远端地址
     * 
     * @param url
     *            服务端的url，形如schema://host:port的字符串
     * @return
     */
    public InetSocketAddress getRemoteAddress(String url);


    /**
     * 获取远端地址
     * 
     * @param url
     *            服务端的group，形如schema://host:port的字符串
     * @return
     */
    public String getRemoteAddressString(String url);


    /**
     * 判断url对应的连接是否可用，注意，如果设置了连接池，那么如果连接池中任一连接可用，即认为可用
     * 
     * @param url
     *            服务端的url，形如schema://host:port的字符串
     * @return
     */
    public boolean isConnected(String url);


    /**
     * 关闭url对应的连接
     * 
     * @param url
     *            服务端的url，形如schema:://host:port的字符串
     * @param allowReconnect
     *            是否需要重连
     * @throws NotifyRemotingException
     * 
     */
    public void close(String url, boolean allowReconnect) throws NotifyRemotingException;


    /**
     * 设置客户端配置，只能在启动前设置，启动后设置无效
     * 
     * @param clientConfig
     */
    public void setClientConfig(ClientConfig clientConfig);

}