/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What an NPC says, as a list of lines shown one page at a time. Used by a quest's
 * lines and an NPC's greeting. A line is text, or text with an animation the NPC
 * plays when the page shows:
 * <pre>[ "고맙네!", { "text": "약속한 에메랄드일세.", "animation": "animation.chief.happy" } ]</pre>
 * A line without an animation is always written as plain text, so documents without
 * animations look as before. See docs/decisions/0009-quest-workbench.md.
 */
public final class DialogueLines {

    /**
     * One page of dialogue.
     *
     * @param animation played once by the NPC when the page shows, or ""
     */
    public record Line(String text, String animation) {

        public static Line of(String text) {
            return new Line(text, "");
        }
    }

    /** The keys of a line written as an object. Never rename: they are saved. */
    private static final Set<String> KEYS = Set.of("text", "animation");

    private DialogueLines() {}

    /** Plain text lines, e.g. for tests and defaults. */
    public static List<Line> text(String... lines) {
        List<Line> out = new ArrayList<>();
        for (String l : lines) {
            out.add(Line.of(l));
        }
        return List.copyOf(out);
    }

    /**
     * Reads a list of lines (absent = none). Each line's text must not be blank, and an
     * animation, if given, must not be blank either.
     *
     * @param where names the list in error messages, e.g. "quest 'quest_a' offer"
     */
    public static List<Line> read(JsonElement e, String where, List<String> errors) {
        if (e == null) {
            return List.of();
        }
        if (!e.isJsonArray()) {
            errors.add(where + ": lines must be a list");
            return List.of();
        }
        List<Line> lines = new ArrayList<>();
        for (JsonElement line : e.getAsJsonArray()) {
            Line read = line.isJsonObject() ? readObject(line.getAsJsonObject(), where, errors)
                    : isText(line) ? new Line(line.getAsString(), "")
                    : null;
            if (read == null) {
                if (!line.isJsonObject()) {
                    errors.add(where + ": a line must be text, or an object with text and animation");
                }
            } else if (read.text().isBlank()) {
                errors.add(where + ": a line is empty");
            } else {
                lines.add(read);
            }
        }
        return List.copyOf(lines);
    }

    private static Line readObject(JsonObject o, String where, List<String> errors) {
        for (String key : o.keySet()) {
            if (!KEYS.contains(key)) {
                errors.add(where + ": unknown line '" + key + "' (use text, animation)");
                return null;
            }
        }
        JsonElement text = o.get("text");
        JsonElement animation = o.get("animation");
        if (!isText(text)) {
            errors.add(where + ": a line needs its text");
            return null;
        }
        if (animation != null && (!isText(animation) || animation.getAsString().isBlank())) {
            errors.add(where + ": the animation of '" + text.getAsString() + "' must be a name like animation.chief.happy");
            return null;
        }
        return new Line(text.getAsString(), animation == null ? "" : animation.getAsString().trim());
    }

    private static boolean isText(JsonElement e) {
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString();
    }

    public static JsonArray write(List<Line> lines) {
        JsonArray arr = new JsonArray();
        for (Line l : lines) {
            if (l.animation().isEmpty()) {
                arr.add(l.text());
            } else {
                JsonObject o = new JsonObject();
                o.addProperty("text", l.text());
                o.addProperty("animation", l.animation());
                arr.add(o);
            }
        }
        return arr;
    }
}
