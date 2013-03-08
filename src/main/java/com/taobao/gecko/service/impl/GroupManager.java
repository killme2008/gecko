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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.taobao.gecko.core.util.MBeanUtils;
import com.taobao.gecko.service.Connection;


/**
 * 
 * 分组管理器,管理分组到连接的映射关系
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-15 下午02:38:09
 */

public class GroupManager implements GroupManagerMBean {
    private final ConcurrentHashMap<String/* group */, List<Connection>> group2ConnectionMap =
            new ConcurrentHashMap<String, List<Connection>>();


    public GroupManager() {
        MBeanUtils.registerMBeanWithIdPrefix(this, null);
    }


    public boolean addConnection(final String group, final Connection connection) {
        synchronized (group.intern()) {
            List<Connection> connections = this.group2ConnectionMap.get(group);
            if (connections == null) {
                // 采用copyOnWrite主要是考虑遍历connection的操作会多一些，在发送消息的时候
                connections = new CopyOnWriteArrayList<Connection>();
                final List<Connection> oldList = this.group2ConnectionMap.putIfAbsent(group, connections);
                if (oldList != null) {
                    connections = oldList;
                }
            }
            // 已经包含，即认为添加成功
            if (connections.contains(connection)) {
                return true;
            }
            else {
                ((DefaultConnection) connection).addGroup(group);
                return connections.add(connection);
            }
        }
    }


    @Override
    public Map<String, Set<String>> getGroupConnectionInfo() {
        final Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (final Map.Entry<String, List<Connection>> entry : this.group2ConnectionMap.entrySet()) {
            final Set<String> set = new HashSet<String>();
            if (entry.getValue() != null) {
                for (final Connection conn : entry.getValue()) {
                    set.add(conn.toString());
                }
            }
            result.put(entry.getKey(), set);
        }
        return result;
    }


    public void clear() {
        this.group2ConnectionMap.clear();
    }


    public int getGroupConnectionCount(final String group) {
        synchronized (group.intern()) {
            final List<Connection> connections = this.group2ConnectionMap.get(group);
            if (connections == null) {
                return 0;
            }
            else {

                return connections.size();
            }
        }
    }


    public boolean removeConnection(final String group, final Connection connection) {
        synchronized (group.intern()) {
            final List<Connection> connections = this.group2ConnectionMap.get(group);
            if (connections == null) {
                return false;
            }
            else {
                final boolean result = connections.remove(connection);
                if (result) {
                    ((DefaultConnection) connection).removeGroup(group);
                }
                if (connections.isEmpty()) {
                    this.group2ConnectionMap.remove(group);
                }
                return result;
            }
        }

    }


    public Set<String> getGroupSet() {
        return this.group2ConnectionMap.keySet();
    }


    public List<Connection> getConnectionsByGroup(final String group) {
        return this.group2ConnectionMap.get(group);
    }

}