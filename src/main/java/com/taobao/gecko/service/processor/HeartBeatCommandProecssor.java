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
package com.taobao.gecko.service.processor;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.HeartBeatRequestCommand;
import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 
 * 心跳命令的处理器
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-18 下午03:50:17
 */

public class HeartBeatCommandProecssor implements RequestProcessor<HeartBeatRequestCommand> {
    static final Log logger = LogFactory.getLog(HeartBeatCommandProecssor.class);


    public HeartBeatCommandProecssor() {
        super();
    }


    public void handleRequest(final HeartBeatRequestCommand request, final Connection conn) {
        try {
            conn.response(conn.getRemotingContext().getCommandFactory()
                .createBooleanAckCommand(request.getRequestHeader(), ResponseStatus.NO_ERROR, null));
        }
        catch (final NotifyRemotingException e) {
            logger.error("发送心跳应答给连接[" + RemotingUtils.getAddrString(conn.getRemoteSocketAddress()) + "]失败", e);
        }
    }


    public ThreadPoolExecutor getExecutor() {
        return null;
    }

}