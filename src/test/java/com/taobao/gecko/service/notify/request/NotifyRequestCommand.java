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
package com.taobao.gecko.service.notify.request;

import java.util.Arrays;

import com.taobao.gecko.core.command.RequestCommand;
import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.service.notify.Constants;
import com.taobao.gecko.service.notify.NotifyCommand;
import com.taobao.gecko.service.notify.OpCode;


public abstract class NotifyRequestCommand implements RequestCommand, NotifyCommand {
    static final long serialVersionUID = -1L;

    private static final byte magic = Constants.REQUEST_MAGIC;
    protected OpCode opCode;
    protected short headerLength;
    protected byte[] header;
    protected int totalBodyLength;
    protected Integer opaque;


    public NotifyRequestCommand() {
        super();
    }


    public OpCode getOpCode() {
        return this.opCode;
    }


    public void setOpaque(final Integer opaque) {
        this.opaque = opaque;
    }


    public NotifyRequestCommand(final OpCode opCode) {
        this.opCode = opCode;
    }

    protected boolean controllCommand;

    protected byte[] body;


    public byte getMagic() {
        return this.magic;
    }


    public short getHeaderLength() {
        return this.headerLength;
    }


    public void setHeaderLength(final short headerLength) {
        this.totalBodyLength = this.totalBodyLength - this.headerLength + headerLength;
        this.headerLength = headerLength;
    }


    public byte[] getHeader() {
        return this.header;
    }


    public CommandHeader getRequestHeader() {
        return new NotifyRequestCommandHeader(this.opaque, this.opCode);
    }


    public void setHeader(final byte[] header) {
        this.header = header;
        if (this.header.length > Short.MAX_VALUE) {
            throw new IllegalStateException("Illegal header,too long");
        }
        this.setHeaderLength((short) this.header.length);
    }


    public int getTotalBodyLength() {
        return this.totalBodyLength;
    }


    public void setTotalBodyLength(final int totalBodyLength) {
        this.totalBodyLength = totalBodyLength;
    }


    public boolean isControllCommand() {
        return this.controllCommand;
    }


    public void setControllCommand(final boolean controllCommand) {
        this.controllCommand = controllCommand;
    }


    public Integer getOpaque() {
        return this.opaque;
    }


    public byte[] getBody() {
        return this.body;
    }


    public void setBody(final byte[] body) {
        if (body == null) {
            throw new NullPointerException("Null body");
        }
        this.body = body;
        this.totalBodyLength = this.headerLength + this.body.length;
    }


    public void setOpCode(final OpCode opCode) {
        this.opCode = opCode;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.body);
        result = prime * result + (this.controllCommand ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(this.header);
        result = prime * result + this.headerLength;
        result = prime * result + (this.opCode == null ? 0 : this.opCode.hashCode());
        result = prime * result + (this.opaque == null ? 0 : this.opaque.hashCode());
        result = prime * result + this.totalBodyLength;
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final NotifyRequestCommand other = (NotifyRequestCommand) obj;
        if (!Arrays.equals(this.body, other.body)) {
            return false;
        }
        if (this.controllCommand != other.controllCommand) {
            return false;
        }
        if (!Arrays.equals(this.header, other.header)) {
            return false;
        }
        if (this.headerLength != other.headerLength) {
            return false;
        }
        if (this.opCode == null) {
            if (other.opCode != null) {
                return false;
            }
        }
        else if (!this.opCode.equals(other.opCode)) {
            return false;
        }
        if (this.opaque == null) {
            if (other.opaque != null) {
                return false;
            }
        }
        else if (!this.opaque.equals(other.opaque)) {
            return false;
        }
        if (this.totalBodyLength != other.totalBodyLength) {
            return false;
        }
        return true;
    }

}