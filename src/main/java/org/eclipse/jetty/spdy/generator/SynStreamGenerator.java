/*
 * Copyright (c) 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.jetty.spdy.generator;

import java.nio.ByteBuffer;

import org.eclipse.jetty.spdy.StreamException;
import org.eclipse.jetty.spdy.api.SPDY;
import org.eclipse.jetty.spdy.api.StreamStatus;
import org.eclipse.jetty.spdy.frames.ControlFrame;
import org.eclipse.jetty.spdy.frames.SynStreamFrame;

public class SynStreamGenerator extends ControlFrameGenerator
{
    private final HeadersBlockGenerator headersBlockGenerator;

    public SynStreamGenerator(HeadersBlockGenerator headersBlockGenerator)
    {
        this.headersBlockGenerator = headersBlockGenerator;
    }

    @Override
    public ByteBuffer generate(ControlFrame frame) throws StreamException
    {
        SynStreamFrame synStream = (SynStreamFrame)frame;
        short version = synStream.getVersion();

        ByteBuffer headersBuffer = headersBlockGenerator.generate(version, synStream.getHeaders());

        int frameBodyLength = 10;

        int frameLength = frameBodyLength + headersBuffer.remaining();
        if (frameLength > 0xFF_FF_FF)
            throw new StreamException(StreamStatus.PROTOCOL_ERROR, "Too many headers");

        int totalLength = ControlFrame.HEADER_LENGTH + frameLength;

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        generateControlFrameHeader(synStream, frameLength, buffer);

        buffer.putInt(synStream.getStreamId() & 0x7F_FF_FF_FF);
        buffer.putInt(synStream.getAssociatedStreamId() & 0x7F_FF_FF_FF);
        writePriority(version, synStream.getPriority(), buffer);

        buffer.put(headersBuffer);

        buffer.flip();
        return buffer;
    }

    private void writePriority(short version, byte priority, ByteBuffer buffer) throws StreamException
    {
        switch (version)
        {
            case SPDY.V2:
                priority <<= 6;
                break;
            case SPDY.V3:
                priority <<= 5;
                break;
            default:
                throw new StreamException(StreamStatus.UNSUPPORTED_VERSION);
        }
        buffer.put(priority);
        buffer.put((byte)0);
    }
}
