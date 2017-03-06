/*
 *     Copyright 2015-2017 Austin Keener & Michael Ritter
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
package net.dv8tion.jda.core.handle;

import net.dv8tion.jda.client.events.message.group.GroupMessageReceivedEvent;
import net.dv8tion.jda.core.entities.EntityBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageType;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.requests.GuildLock;
import net.dv8tion.jda.core.requests.WebSocketClient;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class MessageCreateHandler extends SocketHandler
{
    private static final Pattern invitePattern = Pattern.compile("\\bhttps://(?:www\\.)?discord(?:\\.gg|app\\.com/invite)/([a-zA-Z0-9-]+)\\b");

    public MessageCreateHandler(JDAImpl api)
    {
        super(api);
    }

    @Override
    protected String handleInternally(JSONObject content)
    {
        MessageType type = MessageType.fromId(content.getInt("type"));

        switch (type)
        {
            case DEFAULT:
                return handleDefaultMessage(content);
            default:
                WebSocketClient.LOG.debug(api.getShardMarker(), "JDA received a message of unknown type. Type: {}  JSON: {}", type, content);
        }
        return null;
    }

    private String handleDefaultMessage(JSONObject content)
    {
        Message message;
        try
        {
            message = EntityBuilder.get(api).createMessage(content, true);
        }
        catch (IllegalArgumentException e)
        {
            switch (e.getMessage())
            {
                case EntityBuilder.MISSING_CHANNEL:
                {
                    EventCache.get(api).cache(EventCache.Type.CHANNEL, content.getString("channel_id"), () ->
                    {
                        handle(responseNumber, allContent);
                    });
                    EventCache.LOG.debug("Received a message for a channel that JDA does not currently have cached");
                    return null;
                }
                case EntityBuilder.MISSING_USER:
                {
                    EventCache.get(api).cache(EventCache.Type.USER, content.getJSONObject("author").getString("id"), () ->
                    {
                        handle(responseNumber, allContent);
                    });
                    EventCache.LOG.debug("Received a message for a user that JDA does not currently have cached");
                    return null;
                }
                default:
                    throw e;
            }
        }

        switch (message.getChannelType())
        {
            case TEXT:
            {
                TextChannel channel = message.getTextChannel();
                if (GuildLock.get(api).isLocked(channel.getGuild().getId()))
                {
                    return channel.getGuild().getId();
                }
                api.getEventManager().handle(
                        new GuildMessageReceivedEvent(
                                api, responseNumber,
                                message));
                break;
            }
            case PRIVATE:
            {
                api.getEventManager().handle(
                        new PrivateMessageReceivedEvent(
                                api, responseNumber,
                                message));
                break;
            }
            case GROUP:
            {
                api.getEventManager().handle(
                        new GroupMessageReceivedEvent(
                                api, responseNumber,
                                message));
                break;
            }
            default:
                WebSocketClient.LOG.warn(api.getShardMarker(), "Received a MESSAGE_CREATE with a unknown MessageChannel ChannelType. JSON: {}", content);
                return null;
        }

        //Combo event
        api.getEventManager().handle(
                new MessageReceivedEvent(
                        api, responseNumber,
                        message));

//        //searching for invites
//        Matcher matcher = invitePattern.matcher(message.getContent());
//        while (matcher.find())
//        {
//            InviteUtil.Invite invite = InviteUtil.resolve(matcher.group(1));
//            if (invite != null)
//            {
//                api.getEventManager().handle(
//                        new InviteReceivedEvent(
//                                api, responseNumber,
//                                message,invite));
//            }
//        }
        return null;
    }
}
