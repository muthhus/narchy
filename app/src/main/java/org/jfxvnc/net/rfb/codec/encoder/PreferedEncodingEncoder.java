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
package org.jfxvnc.net.rfb.codec.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jfxvnc.net.rfb.codec.ClientEventType;

import java.util.Arrays;

public class PreferedEncodingEncoder extends MessageToByteEncoder<PreferedEncoding> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PreferedEncoding enc, ByteBuf out) throws Exception {
        out.writeByte(ClientEventType.SET_ENCODINGS);
        out.writeZero(1); // padding
        out.writeShort(enc.getEncodings().length);
        Arrays.stream(enc.getEncodings()).forEach(e -> out.writeInt(e.getType()));

        ctx.pipeline().remove(this);
    }

}
