/**
 *      Copyright 2015-2016 Austin Keener & Michael Ritter
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
package net.dv8tion.jda.utils;

import net.dv8tion.jda.audio.AudioSendHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DCAUtil
{
    private DCAUtil(){}

    public static AudioSendHandler getSendHandlerForFile(File dcaFile)
    {
        try
        {
            return new DCAPlayer(new DCAReader(new FileInputStream(dcaFile)));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static class DCAPlayer implements AudioSendHandler {
        private final DCAReader reader;

        public DCAPlayer(DCAReader reader)
        {
            this.reader = reader;
        }

        @Override
        public boolean canProvide()
        {
            return !reader.atEnd();
        }

        @Override
        public boolean isRaw()
        {
            return true;
        }

        @Override
        public byte[] provide20MsAudio()
        {
            return reader.nextPacket();
        }
    }

    private static class DCAReader
    {
        private JSONObject info;
        private InputStream inStr;

        public DCAReader(InputStream inputStream) throws IOException
        {
            this.inStr = inputStream;
            try
            {
                if(inStr.read() != 'D' || inStr.read() != 'C' || inStr.read() != 'A')
                {
                    try {
                        inStr.close();
                    } catch(IOException ignored) {}
                    throw new IllegalArgumentException("Provided Stream is not a DCA stream!");
                }
                int version = inStr.read() - '0';
                System.out.println("Version: " + version);
                int length = inStr.read() | (inStr.read() << 8) | (inStr.read() << 16) | (inStr.read() << 24);
                System.out.println("Length: " + length);
                byte[] buff = new byte[length];
                if(inStr.read(buff) != length)
                    throw new IOException("Could not write meta-block");
                info = new JSONObject(new String(buff, StandardCharsets.UTF_8));
                System.out.println(info.toString(4));
            }
            catch (IOException | JSONException e)
            {
                info = null;
                close();
                throw e;
            }
        }

        public boolean atEnd() {
            return inStr == null;
        }

        public byte[] nextPacket()
        {
            if (inStr == null)
            {
                return new byte[0];
            }
            try
            {
                byte[] buff = new byte[inStr.read() | (inStr.read()<<8)];
                if (inStr.read(buff) != buff.length)
                {
                    close();
                    System.err.println("Error reading next audio-packet");
                } else {
                    return buff;
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return new byte[0];
        }

        public void close()
        {
            if (inStr != null)
            {
                try {
                    inStr.close();
                } catch(IOException ignored) {}
                inStr = null;
            }
        }
    }
}
