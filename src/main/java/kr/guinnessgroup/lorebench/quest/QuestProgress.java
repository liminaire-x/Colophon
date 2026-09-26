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
 * What a player has done toward one quest's counted goals, saved as their record
 * {@code progress_<quest id>} = {@code {"kill:minecraft:wolf": 2, "harvest:minecraft:wheat": 3}},
 * keyed {@code <kind>:<target>} ({@link QuestDoc.Goal#progressKey()}). Counted by kind and
 * target, not goal position, so reordering a quest's goals keeps everyone's progress.
 * Removed when the quest is completed (the state record says "done").
 * See docs/decisions/0010-farming-goals.md.
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

    public static String write(Map<String, Integer> progress) {
        JsonObject o = new JsonObject();
        new TreeMap<>(progress).forEach(o::addProperty);
        return o.toString();
    }
}
