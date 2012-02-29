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

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;


/**
 * @author boyan
 * 
 */
public final class MyMBeanServer {

    private MBeanServer mbs = null;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>> idMap =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>>();
    private final ReentrantLock lock = new ReentrantLock();

    private static class Holder {
        private static final MyMBeanServer instance = new MyMBeanServer();
    }


    // private static MyMBeanServer me = new MyMBeanServer();

    private MyMBeanServer() {
        this.mbs = ManagementFactory.getPlatformMBeanServer();
    }


    public static MyMBeanServer getInstance() {
        return Holder.instance;
    }


    public void registMBean(final Object o, final String name) {
        // ×¢²áMBean
        if (null != this.mbs) {
            try {
                this.mbs.registerMBean(o, new ObjectName(o.getClass().getPackage().getName() + ":type="
                        + o.getClass().getSimpleName()
                        + (null == name ? ",id=" + o.hashCode() : ",name=" + name + "-" + o.hashCode())));
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void registerMBeanWithId(final Object o, final String id) {
        // ×¢²áMBean
        if (null == id || id.length() == 0) {
            throw new IllegalArgumentException("must set id");
        }
        if (null != this.mbs) {
            try {
                this.mbs.registerMBean(o, new ObjectName(o.getClass().getPackage().getName() + ":type="
                        + o.getClass().getSimpleName() + ",id=" + id));
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    private String getId(final String name, final String idPrefix) {
        ConcurrentHashMap<String, AtomicLong> subMap = this.idMap.get(name);
        if (null == subMap) {
            this.lock.lock();
            try {
                subMap = this.idMap.get(name);
                if (null == subMap) {
                    subMap = new ConcurrentHashMap<String, AtomicLong>();
                    this.idMap.put(name, subMap);
                }
            }
            finally {
                this.lock.unlock();
            }
        }

        AtomicLong indexValue = subMap.get(idPrefix);
        if (null == indexValue) {
            this.lock.lock();
            try {
                indexValue = subMap.get(idPrefix);
                if (null == indexValue) {
                    indexValue = new AtomicLong(0);
                    subMap.put(idPrefix, indexValue);
                }
            }
            finally {
                this.lock.unlock();
            }
        }
        final long value = indexValue.incrementAndGet();
        final String result = idPrefix + "-" + value;
        return result;
    }


    public void registerMBeanWithIdPrefix(final Object o, String idPrefix) {
        // ×¢²áMBean
        if (null != this.mbs) {
            if (null == idPrefix || idPrefix.length() == 0) {
                idPrefix = "default";
            }
            idPrefix = idPrefix.replace(":", "-");

            try {
                final String id = this.getId(o.getClass().getName(), idPrefix);

                this.mbs.registerMBean(o, new ObjectName(o.getClass().getPackage().getName() + ":type="
                        + o.getClass().getSimpleName() + ",id=" + id));
            }
            catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}