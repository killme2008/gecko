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
package com.taobao.gecko.core.core.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.Session;


/**
 * Handler  ≈‰∆˜
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 œ¬ŒÁ06:04:48
 */
public class HandlerAdapter implements Handler {
    private static final Log log = LogFactory.getLog(HandlerAdapter.class);


    public void onExceptionCaught(final Session session, final Throwable throwable) {

    }


    public void onMessageSent(final Session session, final Object message) {

    }


    public void onSessionConnected(final Session session, final Object... args) {

    }


    public void onSessionStarted(final Session session) {

    }


    public void onSessionCreated(final Session session) {

    }


    public void onSessionClosed(final Session session) {

    }


    public void onMessageReceived(final Session session, final Object message) {

    }


    public void onSessionIdle(final Session session) {

    }


    public void onSessionExpired(final Session session) {
        log.warn("Session(" + session.getRemoteSocketAddress() + ") is expired.");
    }

}