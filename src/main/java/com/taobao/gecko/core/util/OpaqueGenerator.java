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
package com.taobao.gecko.core.util;

/**
 * 命令的opaque产生器
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:21:34
 */
public class OpaqueGenerator {
    private static int opaque = Integer.MIN_VALUE;


    // 仅用于测试
    synchronized static void setOpaque(int target) {
        opaque = target;
    }


    public static final synchronized int getNextOpaque() {
        if (opaque >= Integer.MAX_VALUE - 10) {
            resetOpaque();
        }
        return opaque++;
    }


    // 仅用于测试
    public synchronized static void resetOpaque() {
        opaque = Integer.MIN_VALUE;
    }


    // 仅用于测试
    synchronized static int getCurrentOpaque() {
        return opaque;
    }
}