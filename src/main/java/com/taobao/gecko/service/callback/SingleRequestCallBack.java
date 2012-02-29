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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.SingleRequestCallBackListener;
import com.taobao.gecko.service.exception.NotifyRemotingException;


/**
 * 
 * 
 * 针对单个连接或者单个分组的请求回调
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午04:04:2
 */

public final class SingleRequestCallBack extends AbstractRequestCallBack {
    private final CommandHeader requestCommandHeader;
    private ResponseCommand responseCommand;
    private SingleRequestCallBackListener requestCallBackListener;
    private Exception exception;
    private boolean responsed = false;


    public SingleRequestCallBack(final CommandHeader requestCommandHeader, final long timeout,
            final SingleRequestCallBackListener requestCallBackListener) {
        super(new CountDownLatch(1), timeout, System.currentTimeMillis());
        this.requestCommandHeader = requestCommandHeader;
        this.requestCallBackListener = requestCallBackListener;
    }


    public SingleRequestCallBack(final CommandHeader requestCommandHeader, final long timeout) {
        super(new CountDownLatch(1), timeout, System.currentTimeMillis());
        this.requestCommandHeader = requestCommandHeader;
    }


    public Exception getException() {
        return this.exception;
    }


    @Override
    public void setException0(final Exception exception, final Connection conn, final RequestCommand requestCommand) {
        this.exception = exception;
        this.countDownLatch();
        if (this.tryComplete()) {
            if (this.requestCallBackListener != null) {
                this.requestCallBackListener.onException(exception);
            }
        }
    }


    @Override
    public void complete() {
        this.responsed = true;
    }


    @Override
    public boolean isComplete() {
        return this.responsed;
    }


    public ResponseCommand getResult() throws InterruptedException, TimeoutException, NotifyRemotingException {
        if (!this.await(1000, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Operation timeout(1 second)");
        }
        if (this.exception != null) {
            throw new NotifyRemotingException("同步调用失败", this.exception);
        }
        synchronized (this) {
            return this.responseCommand;
        }
    }


    public CommandHeader getRequestCommandHeader() {
        return this.requestCommandHeader;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * com.taobao.gecko.service.impl.RequestCallBack#setResponseCommand
     * (com.taobao.gecko.core.command.ResponseCommand,
     * java.lang.String)
     */
    @Override
    public void onResponse0(final String group, final ResponseCommand responseCommand, final Connection connection) {
        synchronized (this) {
            if (this.responseCommand == null) {
                this.responseCommand = responseCommand;
            }
            else {
                return;// 已经有应答了
            }
        }

        this.countDownLatch();
        if (this.tryComplete()) {
            if (this.requestCallBackListener != null) {
                if (this.requestCallBackListener.getExecutor() != null) {
                    this.requestCallBackListener.getExecutor().execute(new Runnable() {
                        public void run() {
                            SingleRequestCallBack.this.requestCallBackListener.onResponse(responseCommand, connection);
                        }
                    });
                }
                else {
                    this.requestCallBackListener.onResponse(responseCommand, connection);
                }
            }
        }
    }


    public ResponseCommand getResult(final long time, final TimeUnit timeUnit, final Connection conn)
            throws InterruptedException, TimeoutException, NotifyRemotingException {
        if (!this.await(time, timeUnit)) {
            this.cancelWrite(conn);
            // 切记移除回调
            this.removeCallBackFromConnection(conn, this.requestCommandHeader.getOpaque());
            throw new TimeoutException("Operation timeout");
        }
        if (this.exception != null) {
            this.cancelWrite(conn);
            // 切记移除回调
            this.removeCallBackFromConnection(conn, this.requestCommandHeader.getOpaque());
            throw new NotifyRemotingException("同步调用失败", this.exception);
        }
        synchronized (this) {
            return this.responseCommand;
        }
    }

}