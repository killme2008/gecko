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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

import com.taobao.gecko.core.buffer.IoBuffer;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RemotingServer;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ServerConfig;
import com.taobao.gecko.service.notify.NotifyWireFormatType;
import com.taobao.gecko.service.notify.request.NotifyDummyRequestCommand;


/**
 * 文件传输单元测试
 * 
 * @author boyan
 * @Date 2011-4-20
 * 
 */
public class TransferFileUnitTest {
    int port = 8384;


    @Test
    public void testTransferFile() throws Exception {
        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setWireFormatType(new NotifyWireFormatType());
        serverConfig.setPort(this.port++);
        final RemotingServer server = RemotingFactory.bind(serverConfig);
        final String url = server.getConnectURI().toString();

        // 临时文件，写入7个字符
        final File file = File.createTempFile("remoting", "test");
        file.delete();
        final FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        final ByteBuffer buff = ByteBuffer.allocate(7);
        buff.put("hello".getBytes());
        buff.flip();
        channel.write(buff);
        channel.force(true);

        server.registerProcessor(NotifyDummyRequestCommand.class, new RequestProcessor<NotifyDummyRequestCommand>() {

            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void handleRequest(final NotifyDummyRequestCommand request, final Connection conn) {
                conn.transferFrom(IoBuffer.wrap("head ".getBytes()), IoBuffer.wrap(" tail\r\n".getBytes()), channel, 0,
                    7);
            }

        });

        final Socket socket = new Socket();
        socket.connect(server.getInetSocketAddress());
        final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeByte(0x80);
        out.writeByte(0x13);
        out.writeShort(0);
        out.writeInt(0);
        out.writeInt(0);
        out.flush();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final String line = reader.readLine();
        assertEquals("head hello tail", line);

        server.stop();
        socket.close();
        channel.close();
        file.delete();
    }
}