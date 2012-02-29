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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author dennis zhuang
 */
public class SimpleQueue<T> extends java.util.AbstractQueue<T> {

	protected final LinkedList<T> list;

	public SimpleQueue(int initializeCapacity) {
		this.list = new LinkedList<T>();
	}

	public SimpleQueue() {
		this(100);
	}

	public synchronized boolean offer(T e) {
		return this.list.add(e);
	}

	public synchronized T peek() {
		return this.list.peek();
	}

	public synchronized T poll() {
		return this.list.poll();
	}

	@Override
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	@Override
	public int size() {
		return this.list.size();
	}

}