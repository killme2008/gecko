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
package com.taobao.gecko.core.command;

/**
 * 应答状态
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午05:55:23
 */
public enum ResponseStatus {
    NO_ERROR(null), // 正常成功
    ERROR("Error by user"), // 错误，响应端主动设置
    EXCEPTION("Exception occured"), // 异常
    UNKNOWN("Unknow error"), // 没有注册Listener，包括CheckMessageListener和MessageListener
    THREADPOOL_BUSY("Thread pool is busy"), // 响应段线程繁忙
    ERROR_COMM("Communication error"), // 通讯错误，如编码错误
    NO_PROCESSOR("There is no processor to handle this request"), // 没有该请求命令的处理器
    TIMEOUT("Operation timeout"); // 响应超时

    private String errorMessage;


    private ResponseStatus(final String errorMessage) {
        this.errorMessage = errorMessage;
    }


    public String getErrorMessage() {
        return this.errorMessage;
    }

}