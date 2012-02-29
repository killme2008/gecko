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

import com.taobao.gecko.core.command.CommandHeader;
import com.taobao.gecko.service.notify.OpCode;


/**
 * Notify的请求协议头
 * 
 * @author dennis
 * 
 */
public class NotifyRequestCommandHeader implements CommandHeader {
    private Integer opaque;
    private OpCode opCode;


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.opCode == null ? 0 : this.opCode.hashCode());
        result = prime * result + this.opaque;
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
        final NotifyRequestCommandHeader other = (NotifyRequestCommandHeader) obj;
        if (this.opCode == null) {
            if (other.opCode != null) {
                return false;
            }
        }
        else if (!this.opCode.equals(other.opCode)) {
            return false;
        }
        if (!this.opaque.equals(other.opaque)) {
            return false;
        }
        return true;
    }


    public NotifyRequestCommandHeader(final int opaque, final OpCode opCode) {
        super();
        this.opaque = opaque;
        this.opCode = opCode;
    }


    public Integer getOpaque() {
        return this.opaque;
    }


    public void setOpaque(final int opaque) {
        this.opaque = opaque;
    }


    public OpCode getOpCode() {
        return this.opCode;
    }


    public void setOpCode(final OpCode opCode) {
        this.opCode = opCode;
    }

}