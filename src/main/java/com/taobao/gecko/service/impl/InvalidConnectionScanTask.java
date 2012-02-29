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
package com.taobao.gecko.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.service.Connection;


/**
 * 扫描无效的连接任务，仅用于服务器
 * 
 * @author boyan
 * @Date 2010-5-26
 * 
 */
public class InvalidConnectionScanTask implements ScanTask {
    // 对于服务器来说，如果5分钟没有任何操作，那么将断开连接，因为客户端总是会发起心跳检测，因此不会对正常的空闲连接误判。
    public static long TIMEOUT_THRESHOLD = Long.parseLong(System.getProperty(
        "notify.remoting.connection.timeout_threshold", "300000"));
    static final Log log = LogFactory.getLog(InvalidConnectionScanTask.class);


    public void visit(final long now, final Connection conn) {
        final long lastOpTimestamp = ((DefaultConnection) conn).getSession().getLastOperationTimeStamp();
        if (now - lastOpTimestamp > TIMEOUT_THRESHOLD) {
            log.info("无效的连接" + conn.getRemoteSocketAddress() + "被关闭，超过" + TIMEOUT_THRESHOLD + "毫秒没有任何IO操作");
            try {
                conn.close(false);
            }
            catch (final Throwable t) {
                log.error("关闭连接失败", t);
            }
        }
    }
}