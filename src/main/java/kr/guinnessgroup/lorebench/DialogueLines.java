/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * What an NPC says, as a list of lines shown one page at a time, e.g.
 * {@code ["늑대 3마리만 잡아주게.", "요즘 가축이 자꾸 사라지거든."]}. Used by a quest's
 * lines and an NPC's greeting. A line is text for now; later a line may become an
 * object (animation, gaze, speaker, choices) and old text lines still read.
 * See docs/decisions/0009-quest-workbench.md.
 */
public final class DialogueLines {

    private DialogueLines() {}

    /**
     * Reads a list of lines (absent = none). Each line must be text that is not blank.
     *
     * @param where names the list in error messages, e.g. "quest 'quest_a' offer"
     */
    public static List<String> read(JsonElement e, String where, List<String> errors) {
        if (e == null) {
            return List.of();
        }
        if (!e.isJsonArray()) {
            errors.add(where + ": lines must be a list");
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (JsonElement line : e.getAsJsonArray()) {
            if (!line.isJsonPrimitive() || !line.getAsJsonPrimitive().isString()) {
                errors.add(where + ": a line must be text");
            } else if (line.getAsString().isBlank()) {
                errors.add(where + ": a line is empty");
            } else {
                lines.add(line.getAsString());
            }
        }
        return List.copyOf(lines);
    }

    public static JsonArray write(List<String> lines) {
        JsonArray arr = new JsonArray();
        lines.forEach(arr::add);
        return arr;
    }
}
