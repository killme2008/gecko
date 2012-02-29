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
package com.taobao.gecko.service.notify.response;

import com.google.protobuf.InvalidProtocolBufferException;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.service.notify.NotifyProtos;
import com.taobao.gecko.service.notify.OpCode;
import com.taobao.gecko.service.notify.request.NotifyRequestCommand;
import com.taobao.gecko.service.notify.request.NotifyRequestCommandHeader;


/**
 * 
 * 响应成功或者失败的应答，如果失败，可能body带有错误信息
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-17 下午07:38:13
 */

public class NotifyBooleanAckCommand extends NotifyResponseCommand implements BooleanAckCommand {
    /**
     * 
     */
    private static final long serialVersionUID = -2729908481782608962L;
    private String errorMsg;


    public NotifyBooleanAckCommand(final OpCode opCode) {
        super(opCode);
    }


    public NotifyBooleanAckCommand(final NotifyRequestCommand request, final ResponseStatus responseStatus,
            final String errorMsg) {
        if (request == null) {
            throw new NullPointerException("Null request");
        }
        if (responseStatus == null) {
            throw new NullPointerException("Null ResponseStatus");
        }
        this.opCode = request.getOpCode();
        this.opaque = request.getOpaque();
        this.responseStatus = responseStatus;
        this.errorMsg = errorMsg;
    }


    public NotifyBooleanAckCommand(final CommandHeader header, final ResponseStatus responseStatus,
            final String errorMsg) {
        if (header == null) {
            throw new NullPointerException("Null header");
        }
        if (responseStatus == null) {
            throw new NullPointerException("Null ResponseStatus");
        }
        if (header instanceof NotifyRequestCommandHeader) {
            this.opCode = ((NotifyRequestCommandHeader) header).getOpCode();
        }
        else {
            // remoting自身返回的header，可能没有设置opcode，那么默认设置为dummy
            this.opCode = OpCode.DUMMY;
        }
        this.opaque = header.getOpaque();
        this.responseStatus = responseStatus;
        this.errorMsg = errorMsg;
    }


    public void decodeContent() {
        if (this.header != null) {
            try {
                final NotifyProtos.ErrorMesssage errorMsg = NotifyProtos.ErrorMesssage.parseFrom(this.header);
                if (errorMsg.hasErrorMessage()) {
                    this.errorMsg = errorMsg.getErrorMessage();
                }
            }
            catch (final InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void encodeContent() {
        if (this.errorMsg != null) {
            final NotifyProtos.ErrorMesssage errorMsg =
                    NotifyProtos.ErrorMesssage.newBuilder().setErrorMessage(this.errorMsg).build();
            this.setHeader(errorMsg.toByteArray());
        }
    }


    public String getErrorMsg() {
        return this.errorMsg;
    }


    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }

}