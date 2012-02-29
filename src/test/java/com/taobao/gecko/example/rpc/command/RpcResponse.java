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
package com.taobao.gecko.example.rpc.command;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.core.command.ResponseCommand;
import com.taobao.gecko.core.command.ResponseStatus;
import com.taobao.gecko.core.command.kernel.BooleanAckCommand;
import com.taobao.gecko.service.notify.ResponseStatusCode;


public class RpcResponse implements ResponseCommand, RpcCommand, BooleanAckCommand {
    static final long serialVersionUID = -1L;
    private Integer opaque;
    private InetSocketAddress responseHost;
    private ResponseStatus responseStatus;
    private long responseTime;
    private Object result;


    public Object getResult() {
        return this.result;
    }


    public RpcResponse() {
        super();
    }


    public RpcResponse(final Integer opaque, final ResponseStatus responseStatus, final Object result) {
        super();
        this.opaque = opaque;
        this.responseStatus = responseStatus;
        this.result = result;
    }

    private String errorMsg;


    public String getErrorMsg() {
        return this.errorMsg;
    }


    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;

    }


    public Integer getOpaque() {
        return this.opaque;
    }


    public InetSocketAddress getResponseHost() {
        return this.responseHost;
    }


    public ResponseStatus getResponseStatus() {
        return this.responseStatus;
    }


    public long getResponseTime() {
        return this.responseTime;
    }


    public boolean isBoolean() {
        return false;
    }


    public void setOpaque(final Integer opaque) {
        this.opaque = opaque;
    }


    public void setResponseHost(final InetSocketAddress address) {
        this.responseHost = address;

    }


    public boolean decode(final IoBuffer buffer) {
        buffer.mark();
        if (buffer.remaining() >= 4) {
            this.setOpaque(buffer.getInt());
            if (buffer.remaining() >= 2) {
                final short status = buffer.getShort();
                this.setResponseStatus(ResponseStatusCode.valueOf(status));
                if (buffer.remaining() >= 4) {
                    final int resultDataLen = buffer.getInt();
                    if (buffer.remaining() >= resultDataLen) {
                        final byte[] data = new byte[resultDataLen];
                        buffer.get(data);
                        final ByteArrayInputStream in = new ByteArrayInputStream(data);
                        try {
                            final ObjectInputStream objIn = new ObjectInputStream(in);
                            this.result = objIn.readObject();
                        }
                        catch (final Exception e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            try {
                                in.close();
                            }
                            catch (final IOException e) {
                                // ignore
                            }
                        }
                        return true;
                    }
                }
            }
        }

        buffer.reset();
        return false;
    }


    public IoBuffer encode() {
        byte[] resultData = new byte[0];
        if (resultData != null) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                final ObjectOutputStream objOut = new ObjectOutputStream(out);
                objOut.writeObject(this.result);
                out.close();
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
            resultData = out.toByteArray();
        }

        final IoBuffer buffer = IoBuffer.allocate(1 + 2 + 4 + 4 + resultData.length);
        buffer.put((byte) 0x71);
        buffer.putInt(this.opaque);
        buffer.putShort(ResponseStatusCode.getValue(this.responseStatus));
        buffer.putInt(resultData.length);
        buffer.put(resultData);
        buffer.flip();
        return buffer;
    }


    public void setResponseStatus(final ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;

    }


    public void setResponseTime(final long time) {
        this.responseTime = time;
    }

}