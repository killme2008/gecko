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
package com.taobao.gecko.core.core;

/**
 * 业务处理器
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:00:24
 */
public interface Handler {

    void onSessionCreated(Session session);


    void onSessionStarted(Session session);


    void onSessionClosed(Session session);


    void onMessageReceived(Session session, Object msg);


    void onMessageSent(Session session, Object msg);


    void onExceptionCaught(Session session, Throwable throwable);


    void onSessionExpired(Session session);


    void onSessionIdle(Session session);


    void onSessionConnected(Session session, Object... args);

}