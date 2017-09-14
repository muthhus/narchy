/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package spacegraph.net.vnc.rfb.codec.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class BellDecoder implements FrameDecoder {

    public BellDecoder() {
    }

    @Override
    public boolean decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable(1)) {
            return false;
        }
        in.skipBytes(1);
        out.add(new BellEvent());
        return true;
    }

}
