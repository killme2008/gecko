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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * 系统工具类，用于判断jvm版本等
 * 
 * 
 * 
 * @author boyan
 * 
 * @since 1.0, 2009-12-16 下午06:23:11
 */
public final class SystemUtils {

    private SystemUtils() {

    }

    public static final String OS_NAME = System.getProperty("os.name");

    private static boolean isLinuxPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().indexOf("linux") >= 0) {
            isLinuxPlatform = true;
        }
    }
    public static final String JAVA_VERSION = System.getProperty("java.version");
    private static boolean isAfterJava6u4Version = false;
    static {
        if (JAVA_VERSION != null) {
            // java4 or java5
            if (JAVA_VERSION.indexOf("1.4.") >= 0 || JAVA_VERSION.indexOf("1.5.") >= 0) {
                isAfterJava6u4Version = false;
            }
            else if (JAVA_VERSION.indexOf("1.6.") >= 0) {
                final int index = JAVA_VERSION.indexOf("_");
                if (index > 0) {
                    final String subVersionStr = JAVA_VERSION.substring(index + 1);
                    if (subVersionStr != null && subVersionStr.length() > 0) {
                        try {
                            final int subVersion = Integer.parseInt(subVersionStr);
                            if (subVersion >= 4) {
                                isAfterJava6u4Version = true;
                            }
                        }
                        catch (final Exception e) {

                        }
                    }
                }
                // after java6
            }
            else {
                isAfterJava6u4Version = true;
            }
        }
    }


    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }


    public static boolean isAfterJava6u4Version() {
        return isAfterJava6u4Version;
    }


    public static void main(final String[] args) {
        System.out.println(isAfterJava6u4Version());
    }


    /**
     * 默认为CPU个数-1，留一个CPU做网卡中断
     * 
     * @return
     */
    public static int getSystemThreadCount() {
        final int cpus = getCpuProcessorCount();
        final int result = cpus - 1;
        return result == 0 ? 1 : result;
    }


    public static int getCpuProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }


    public static Selector openSelector() throws IOException {
        Selector result = null;
        // 在linux平台，尽量启用epoll实现
        if (SystemUtils.isLinuxPlatform()) {
            try {
                final Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
                if (providerClazz != null) {
                    try {
                        final Method method = providerClazz.getMethod("provider");
                        if (method != null) {
                            final SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
                            if (selectorProvider != null) {
                                result = selectorProvider.openSelector();
                            }
                        }
                    }
                    catch (final Exception e) {
                        // ignore
                    }
                }
            }
            catch (final Exception e) {
                // ignore
            }
        }
        if (result == null) {
            result = Selector.open();
        }
        return result;

    }

    private static Map<InetSocketAddress, InetAddress> TEST_ADDRESSES =
            new LinkedHashMap<InetSocketAddress, InetAddress>();

    public static boolean SET_RECEIVE_BUFFER_SIZE_AVAILABLE = false;

    public static boolean SET_SEND_BUFFER_SIZE_AVAILABLE = false;

    public static boolean DEFAULT_REUSE_ADDRESS = false;

    public static int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;

    public static int DEFAULT_SEND_BUFFER_SIZE = 1024;

    public static boolean DEFAULT_KEEP_ALIVE = false;

    public static int DEFAULT_SO_LINGER = -1;

    public static boolean DEFAULT_TCP_NO_DELAY = false;

    // 静态初始化，确认默认的tcp选项参数
    static {
        initializeTestAddresses();

        boolean success = false;
        for (final Entry<InetSocketAddress, InetAddress> e : TEST_ADDRESSES.entrySet()) {
            success = initializeDefaultSocketParameters(e.getKey(), e.getValue());
            if (success) {
                break;
            }
        }

        if (!success) {
            initializeFallbackDefaultSocketParameters();
        }
    }


    private static void initializeFallbackDefaultSocketParameters() {
        final Socket unconnectedSocket = new Socket(); // Use a unconnected
                                                       // socket.
        try {
            initializeDefaultSocketParameters(unconnectedSocket);
        }
        catch (final SocketException se) {
            ExceptionMonitor.getInstance().exceptionCaught(se);

            try {
                unconnectedSocket.close();
            }
            catch (final IOException ioe) {
                ExceptionMonitor.getInstance().exceptionCaught(ioe);
            }
        }
    }


    private static void initializeTestAddresses() {
        try {
            // IPv6 localhost
            TEST_ADDRESSES.put(
                new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                                                                           0, 1 }), 0),
                InetAddress.getByAddress(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));

            // IPv4 localhost
            TEST_ADDRESSES.put(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 0),
                InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));

            // Bind to wildcard interface and connect to IPv6 localhost
            TEST_ADDRESSES.put(new InetSocketAddress(0),
                InetAddress.getByAddress(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));

            // Bind to wildcard interface and connect to IPv4 localhost
            TEST_ADDRESSES.put(new InetSocketAddress(0), InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));

        }
        catch (final UnknownHostException e) {
            ExceptionMonitor.getInstance().exceptionCaught(e);
        }
    }


    private static boolean initializeDefaultSocketParameters(final InetSocketAddress bindAddress,
            final InetAddress connectAddress) {
        ServerSocket ss = null;
        Socket socket = null;

        try {
            ss = new ServerSocket();
            ss.bind(bindAddress);
            socket = new Socket();

            // Timeout is set to 10 seconds in case of infinite blocking
            // on some platform.
            socket.connect(new InetSocketAddress(connectAddress, ss.getLocalPort()), 10000);

            initializeDefaultSocketParameters(socket);
            return true;
        }
        catch (final Exception e) {
            return false;
        }
        finally {
            if (socket != null) {
                try {
                    socket.close();
                }
                catch (final IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }

            if (ss != null) {
                try {
                    ss.close();
                }
                catch (final IOException e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }


    private static void initializeDefaultSocketParameters(final Socket socket) throws SocketException {
        DEFAULT_REUSE_ADDRESS = socket.getReuseAddress();
        DEFAULT_RECEIVE_BUFFER_SIZE = socket.getReceiveBufferSize();
        DEFAULT_SEND_BUFFER_SIZE = socket.getSendBufferSize();
        DEFAULT_KEEP_ALIVE = socket.getKeepAlive();
        DEFAULT_SO_LINGER = socket.getSoLinger();
        DEFAULT_TCP_NO_DELAY = socket.getTcpNoDelay();

        // Check if setReceiveBufferSize is supported.
        try {
            socket.setReceiveBufferSize(DEFAULT_RECEIVE_BUFFER_SIZE);
            SET_RECEIVE_BUFFER_SIZE_AVAILABLE = true;
        }
        catch (final SocketException e) {
            SET_RECEIVE_BUFFER_SIZE_AVAILABLE = false;
        }

        // Check if setSendBufferSize is supported.
        try {
            socket.setSendBufferSize(DEFAULT_SEND_BUFFER_SIZE);
            SET_SEND_BUFFER_SIZE_AVAILABLE = true;
        }
        catch (final SocketException e) {
            SET_SEND_BUFFER_SIZE_AVAILABLE = false;
        }

    }


    public static boolean isSetReceiveBufferSizeAvailable() {
        return SET_RECEIVE_BUFFER_SIZE_AVAILABLE;
    }


    public static boolean isSetSendBufferSizeAvailable() {
        return SET_SEND_BUFFER_SIZE_AVAILABLE;
    }

}