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
package com.taobao.gecko.service.notify;

/**
 * ÃüÁîÂë
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 ÏÂÎç05:54:52
 */

public enum OpCode {
    SEND_MESSAGE,
    DELIVER_MESSAGE,
    SEND_METADATA,
    CHECK_MESSAGE,
    COMMIT_ROLLBACK_MESSAGE,
    CLOSE_SUBSCRIPTION,
    OPEN_SUBSCRIPTION,
    SEND_MESSAGE_LIST,
    HEARTBEAT,
    SEND_SUBSCRIPTION,
    DUMMY;

    public byte getValue() {
        switch (this) {
        case SEND_MESSAGE:
            return 0x00;
        case DELIVER_MESSAGE:
            return 0x01;
        case SEND_METADATA:
            return 0x02;
        case CHECK_MESSAGE:
            return 0x03;
        case COMMIT_ROLLBACK_MESSAGE:
            return 0x04;
        case CLOSE_SUBSCRIPTION:
            return 0x05;
        case OPEN_SUBSCRIPTION:
            return 0x06;
        case SEND_MESSAGE_LIST:
            return 0x07;
        case HEARTBEAT:
            return 0x10;
        case SEND_SUBSCRIPTION:
            return 0x11;
        case DUMMY:
            return 0x13;

        }
        throw new IllegalArgumentException("Unknown OpCode " + this);
    }


    public static OpCode valueOf(byte value) {
        switch (value) {
        case 0x00:
            return SEND_MESSAGE;
        case 0x01:
            return DELIVER_MESSAGE;
        case 0x02:
            return SEND_METADATA;
        case 0x03:
            return CHECK_MESSAGE;
        case 0x04:
            return COMMIT_ROLLBACK_MESSAGE;
        case 0x05:
            return CLOSE_SUBSCRIPTION;
        case 0x06:
            return OPEN_SUBSCRIPTION;
        case 0x07:
            return SEND_MESSAGE_LIST;
        case 0x10:
            return HEARTBEAT;
        case 0x11:
            return SEND_SUBSCRIPTION;
        case 0x13:
            return DUMMY;
        }
        throw new IllegalArgumentException("Unknown OpCode " + value);
    }
}