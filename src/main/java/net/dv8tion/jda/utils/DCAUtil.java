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
import net.dv8tion.jda.audio.player.Player;
import org.json.JSONException;
import org.json.JSONObject;

import javax.sound.sampled.AudioInputStream;
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

    public static class DCAPlayer extends Player {
        private  DCAReader reader;
        private boolean started = false;
        private boolean paused = false;
        private boolean stopped = false;

        public DCAPlayer(DCAReader reader)
        {
            this.reader = reader;
        }

        public DCAPlayer(File dcaFile)
        {
            try
            {
                this.reader = new DCAReader(new FileInputStream(dcaFile));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public boolean canProvide()
        {
            if (reader.atEnd())
            {
                started = false;
                paused = false;
                reader.close();
                return false;
            }
            return started && !paused;
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

        @Override
        public void play()
        {
            if (stopped)
            {
                restart();
            }
            started = true;
            paused = false;
        }

        @Override
        public void pause()
        {
            paused = true;
        }

        @Override
        public void stop()
        {
            started = false;
            paused = false;
            stopped = true;
        }

        @Override
        public void restart()
        {
            reader.reset();
            stopped = false;
            started = false;
            paused = false;
        }

        @Override
        public boolean isStarted()
        {
            return started;
        }

        @Override
        public boolean isPlaying()
        {
            return canProvide();
        }

        @Override
        public boolean isPaused()
        {
            return paused;
        }

        @Override
        public boolean isStopped()
        {
            return stopped;
        }

        @Override
        public void setAudioSource(AudioInputStream inSource)
        {
            throw new UnsupportedOperationException("DCAPlayer does not use AudioInputStreams!");
        }

        @Override
        public void setVolume(float volume)
        {
            throw new UnsupportedOperationException("Volume manipulation on DCA-streams is not possible!");
        }
    }

    public static class DCAReader
    {
        private int version;
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
                version = inStr.read() - '0';
                int length = inStr.read() | (inStr.read() << 8) | (inStr.read() << 16) | (inStr.read() << 24);
                byte[] buff = new byte[length];
                if(inStr.read(buff) != length)
                    throw new IOException("Could not write meta-block");
                info = new JSONObject(new String(buff, StandardCharsets.UTF_8));
                if (inStr.markSupported())
                {
                    inStr.mark(Integer.MAX_VALUE);
                }
            }
            catch (IOException | JSONException e)
            {
                info = null;
                close();
                throw e;
            }
        }

        public void reset()
        {
            if (inStr.markSupported())
            {
                try
                {
                    inStr.reset();
                    return;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            throw new UnsupportedOperationException("Given InputStream does not allow resets!");
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

        public int getVersion()
        {
            return version;
        }

        public JSONObject getInfo()
        {
            return info;
        }
    }
}
