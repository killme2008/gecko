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
package com.taobao.gecko.service.callback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.GroupAllConnectionCallBackListener;


/**
 * 
 * 
 * 单个分组所有连接的请求回调
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午03:29:41
 */

public class GroupAllConnectionRequestCallBack extends AbstractRequestCallBack {
    private final ConcurrentHashMap<Connection, ResponseCommand> resultMap; // 应答结果集合
    private final GroupAllConnectionCallBackListener listener;
    private boolean responsed;


    public GroupAllConnectionRequestCallBack(final GroupAllConnectionCallBackListener listener,
            final CountDownLatch countDownLatch, final long timeout, final long timestamp,
            final ConcurrentHashMap<Connection, ResponseCommand> resultMap) {
        super(countDownLatch, timeout, timestamp);
        this.listener = listener;
        this.resultMap = resultMap;
        this.responsed = false;
    }


    public Map<Connection, ResponseCommand> getResultMap() {
        return this.resultMap;
    }


    @Override
    public void setException0(final Exception e, final Connection conn, final RequestCommand requestCommand) {
        if (this.resultMap.putIfAbsent(conn,
            createComunicationErrorResponseCommand(conn, e, requestCommand, conn.getRemoteSocketAddress())) == null) {
            this.countDownLatch();
        }
        this.tryNotifyListener();
    }


    @Override
    public void onResponse0(final String group, final ResponseCommand responseCommand, final Connection connection) {
        if (this.resultMap.putIfAbsent(connection, responseCommand) == null) {
            this.countDownLatch();
        }
        this.tryNotifyListener();
    }


    @Override
    public void complete() {
        this.responsed = true;
    }


    @Override
    public boolean isComplete() {
        return this.responsed;
    }


    private void tryNotifyListener() {
        if (this.tryComplete()) {
            if (this.listener != null) {
                if (this.listener.getExecutor() != null) {
                    this.listener.getExecutor().execute(new Runnable() {
                        public void run() {
                            GroupAllConnectionRequestCallBack.this.listener
                                .onResponse(GroupAllConnectionRequestCallBack.this.resultMap);
                        }
                    });
                }
                else {
                    this.listener.onResponse(this.resultMap);
                }
            }
        }
    }


    @Override
    public void dispose() {
        super.dispose();
        this.resultMap.clear();
    }

}