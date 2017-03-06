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

import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class EventCache
{
    public static final Logger LOG = LoggerFactory.getLogger("EventCache");
    private static HashMap<JDA, EventCache> caches = new HashMap<>();
    private HashMap<Type, HashMap<String, List<Runnable>>> eventCache = new HashMap<>();

    public static EventCache get(JDA jda)
    {
        EventCache cache = caches.get(jda);
        if (cache == null)
        {
            cache = new EventCache();
            caches.put(jda, cache);
        }
        return cache;
    }

    public void cache(Type type, String triggerId, Runnable handler)
    {
        HashMap<String, List<Runnable>> triggerCache = eventCache.get(type);
        if (triggerCache == null)
        {
            triggerCache = new HashMap<>();
            eventCache.put(type, triggerCache);
        }

        List<Runnable> items = triggerCache.get(triggerId);
        if (items == null)
        {
            items = new LinkedList<>();
            triggerCache.put(triggerId, items);
        }

        items.add(handler);
    }

    public void playbackCache(Type type, String triggerId)
    {
        List<Runnable> items;
        try
        {
            items = eventCache.get(type).get(triggerId);
        }
        catch (NullPointerException e)
        {
            //If we encounter an NPE that means something didn't exist.
            return;
        }

        if (items != null && !items.isEmpty())
        {
            EventCache.LOG.debug("Replaying " + items.size() + " events from the EventCache for a " + type + " with id: " + triggerId);
            List<Runnable> itemsCopy = new LinkedList<>(items);
            items.clear();
            for (Runnable item : itemsCopy)
            {
                item.run();
            }
        }
    }

    public int size()
    {
        int count = 0;
        for (HashMap<String, List<Runnable>> typeMap : eventCache.values())
        {
            for (List<Runnable> eventList : typeMap.values())
            {
                count += eventList.size();
            }
        }
        return count;
    }

    public void clear()
    {
        eventCache.clear();
    }

    public enum Type
    {
        USER, GUILD, CHANNEL, ROLE, RELATIONSHIP, CALL
    }
}
