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

/**
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 上午11:22:49
 */

public class ClientConfig extends BaseConfig {

    public ClientConfig() {
        super();
        // 设置判断连接空闲时间为10秒
        this.setIdleTime(10);
        this.setMaxCallBackCount(100000);
        this.setSelectorPoolSize(Runtime.getRuntime().availableProcessors());
        this.setReadThreadCount(0);
        this.setMaxScheduleWrittenBytes(Runtime.getRuntime().maxMemory() / 10);
    }

    /**
     * Max retries to reconnect connections.If it is less or equal to zero,it
     * means unlimit.Unlimit by default.
     */
    private int maxReconnectTimes = -1;
    /**
     * 连接超时,单位毫秒
     */
    private long connectTimeout = 80000L;
    /**
     * 重连间隔，单位毫秒
     */
    private long healConnectionInterval = 2000L;

    /**
     * 重连管理器的连接池大小
     */
    private int healConnectionExecutorPoolSize = 1;


    public long getConnectTimeout() {
        return this.connectTimeout;
    }


    public void setConnectTimeout(final long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }


    public int getMaxReconnectTimes() {
        return this.maxReconnectTimes;
    }


    public void setMaxReconnectTimes(int maxReconnectTimes) {
        this.maxReconnectTimes = maxReconnectTimes;
    }


    public int getHealConnectionExecutorPoolSize() {
        return this.healConnectionExecutorPoolSize;
    }


    public void setHealConnectionExecutorPoolSize(final int healConnectionExecutorPoolSize) {
        this.healConnectionExecutorPoolSize = healConnectionExecutorPoolSize;
    }


    public long getHealConnectionInterval() {
        return this.healConnectionInterval;
    }


    public void setHealConnectionInterval(final long healConnectionInterval) {
        this.healConnectionInterval = healConnectionInterval;
    }

}