/**
 *    Copyright 2015-2016 Austin Keener & Michael Ritter
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

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.audio.player.Player;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.DCAUtil;

import javax.security.auth.login.LoginException;
import java.io.File;

public class AudioExample extends ListenerAdapter
{

    private Player player = null;

    public static void main(String[] args)
    {
        try
        {
            JDA api = new JDABuilder()
                    .setEmail("EMAIL")
                    .setPassword("PASSWORD")
                    .addListener(new AudioExample())
                    .buildBlocking();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e)
        {
            System.out.println("The provided email / password combination was incorrect. Please provide valid details.");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        String message = event.getMessage().getContent();

        //Start an audio connection with a VoiceChannel
        if (message.startsWith("join "))
        {
            //Separates the name of the channel so that we can search for it
            String chanName = message.substring(5);

            //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
            VoiceChannel channel = event.getGuild().getVoiceChannels().stream().filter(
                    vChan -> vChan.getName().equalsIgnoreCase(chanName))
                    .findFirst().orElse(null);  //If there isn't a matching name, return null.
            if (channel == null)
            {
                event.getChannel().sendMessage("There isn't a VoiceChannel in this Guild with the name: '" + chanName + "'");
                return;
            }
            event.getJDA().getAudioManager().openAudioConnection(channel);
        }
        //Disconnect the audio connection with the VoiceChannel.
        if (message.equals("leave"))
            event.getJDA().getAudioManager().closeAudioConnection();

        //Start playing audio with our FilePlayer. If we haven't created and registered a FilePlayer yet, do that.
        if (message.equals("play"))
        {
            //If the player didn't exist, create it and start playback.
            if (player == null || !player.canProvide())
            {
                if (player == null || player.isStopped())
                {
                    File audioFile = new File("snowblind.dca");
                    player = new DCAUtil.DCAPlayer(audioFile);
                    event.getJDA().getAudioManager().setSendingHandler(player);
                }
                player.play();
            }
        }
        else if (player != null && !player.isStopped() && message.equals("pause"))
        {
            player.pause();
        }

        //You can't pause, stop or restart before a player has even been created!
        else if (player != null && message.equals("stop"))
        {
            player.stop();
            player = null;
        }
    }
}
