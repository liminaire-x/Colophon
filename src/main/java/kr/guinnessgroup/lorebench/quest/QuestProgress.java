/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.TreeMap;

/**
 * A player's kill counts for one quest, saved as their record
 * {@code progress_<quest id>} = {@code {"minecraft:wolf": 2}}. Counted by entity id,
 * not goal position, so reordering a quest's goals keeps everyone's progress.
 * Removed when the quest is completed (the state record says "done").
 */
public final class QuestProgress {

    private QuestProgress() {}

    public static String key(String questId) {
        return "progress_" + questId;
    }

    /** The counts in a stored value; empty if there is none or it is broken. */
    public static Map<String, Integer> read(String value) {
        Map<String, Integer> out = new TreeMap<>();
        if (value == null) {
            return out;
        }
        try {
            JsonObject o = JsonParser.parseString(value).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                int n = e.getValue().getAsInt();
                if (n > 0) {
                    out.put(e.getKey(), n);
                }
            }
        } catch (RuntimeException e) {
            out.clear();
        }
        return out;
    }

    public static String write(Map<String, Integer> kills) {
        JsonObject o = new JsonObject();
        new TreeMap<>(kills).forEach(o::addProperty);
        return o.toString();
    }
}
