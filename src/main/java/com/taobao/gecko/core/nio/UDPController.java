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
package com.taobao.gecko.core.nio;

/**
 *Copyright [2008-2009] [dennis zhuang]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License. 
 *You may obtain a copy of the License at 
 *             http://www.apache.org/licenses/LICENSE-2.0 
 *Unless required by applicable law or agreed to in writing, 
 *software distributed under the License is distributed on an "AS IS" BASIS, 
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
import java.io.IOException;

import com.taobao.gecko.core.config.Configuration;
import com.taobao.gecko.core.core.CodecFactory;
import com.taobao.gecko.core.core.Handler;
import com.taobao.gecko.core.core.ServerController;
import com.taobao.gecko.core.nio.impl.DatagramChannelController;


/**
 * Controller for udp server
 * 
 * @author dennis
 * 
 */
public class UDPController extends DatagramChannelController implements ServerController {

    public UDPController(final Configuration configuration) {
        super(configuration, null, null);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public UDPController() {
        super();
    }


    public UDPController(final Configuration configuration, final CodecFactory codecFactory) {
        super(configuration, null, codecFactory);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }


    public void unbind() throws IOException {
        this.stop();
    }


    public UDPController(final Configuration configuration, final Handler handler, final CodecFactory codecFactory) {
        super(configuration, handler, codecFactory);
        this.setMaxDatagramPacketLength(configuration.getSessionReadBufferSize() > 9216 ? 4096 : configuration
            .getSessionReadBufferSize());
    }
}