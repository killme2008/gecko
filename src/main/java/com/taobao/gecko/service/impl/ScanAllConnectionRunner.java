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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.taobao.gecko.core.command.Constants;
import com.taobao.gecko.service.Connection;


/**
 * 
 * 扫描所有连接的任务线程
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-18 下午04:18:10
 */

public class ScanAllConnectionRunner implements Runnable {
    private final BaseRemotingController controller;

    private final CopyOnWriteArrayList<ScanTask> taskList = new CopyOnWriteArrayList<ScanTask>();


    public void addScanTask(final ScanTask task) {
        this.taskList.add(task);
    }


    public void removeScanTask(final ScanTask task) {
        this.taskList.remove(task);
    }


    public ScanAllConnectionRunner(final BaseRemotingController controller, final ScanTask... tasks) {
        super();
        this.controller = controller;
        if (tasks != null) {
            for (final ScanTask task : tasks) {
                this.taskList.add(task);
            }
        }

    }


    public void run() {
        // 获取所有连接并遍历
        final long now = System.currentTimeMillis();
        final List<Connection> connections =
                this.controller.remotingContext.getConnectionsByGroup(Constants.DEFAULT_GROUP);
        if (connections != null) {
            for (final Connection conn : connections) {
                for (final ScanTask task : this.taskList) {
                    task.visit(now, conn);
                }
            }
        }

    }

}