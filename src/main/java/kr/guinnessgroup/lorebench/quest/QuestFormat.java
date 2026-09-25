/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import kr.guinnessgroup.lorebench.DocumentException;
import kr.guinnessgroup.lorebench.Folders;
import kr.guinnessgroup.lorebench.Ids;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads and writes the quest document (format 1):
 * <pre>{ "format": 1,
 *   "folders": [ { "id": "folder_2kq8d1xz", "name": "마을" }, { "id": "folder_9fm3a0pe", "name": "촌장", "parent": "folder_2kq8d1xz" } ],
 *   "quests": [ {
 *   "id": "quest_k3f9x2ma", "title": "밀 배달", "icon": "minecraft:wheat", "text": "...", "folder": "folder_9fm3a0pe",
 *   "goals":   [ { "item": "minecraft:wheat",   "count": 10 }, { "kill": "minecraft:wolf", "count": 3 } ],
 *   "rewards": [ { "item": "minecraft:emerald", "count": 5 } ] } ] }</pre>
 * {@code folders}, a folder's {@code parent}, and a quest's {@code icon}, {@code text} and
 * {@code folder} are optional (no parent or folder = the top). This checks only the shape; whether
 * the items and entities exist is checked on publish, where the game's lists are available.
 */
public final class QuestFormat {

    public static final int VERSION = 1;

    public static final int MAX_COUNT = 9999;

    /** An item id with its namespace, e.g. {@code minecraft:wheat}. */
    public static final Pattern ITEM = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    /**
     * An item as {@code /give} writes it: an id, optionally followed by components,
     * e.g. {@code minecraft:iron_sword[custom_name='"Blade"']}. Only the
     * shape is checked here; the game's item parser checks the rest on publish.
     */
    public static final Pattern ITEM_WITH_COMPONENTS = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+(\\[.*])?", Pattern.DOTALL);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private QuestFormat() {}

    public static QuestDoc read(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new DocumentException(List.of("quest document is not a JSON object"));
        }
        JsonElement format = root.get("format");
        if (format == null || !format.isJsonPrimitive() || !format.getAsJsonPrimitive().isNumber()) {
            throw new DocumentException(List.of("quest document: missing 'format' number"));
        }
        int version = format.getAsInt();
        if (version > VERSION) {
            throw new DocumentException(List.of("quest document format " + version
                    + " is newer than this Lorebench supports (" + VERSION + ")"));
        }
        if (version != VERSION) {
            throw new DocumentException(List.of("quest document: unknown format " + version));
        }
        JsonElement arr = root.get("quests");
        if (arr == null || !arr.isJsonArray()) {
            throw new DocumentException(List.of("quest document: missing 'quests' list"));
        }

        List<String> errors = new ArrayList<>();
        List<Folders.Folder> folders = Folders.read(root.get("folders"), "quest document", errors);
        List<QuestDoc.Quest> quests = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonElement el : (JsonArray) arr) {
            if (!el.isJsonObject()) {
                errors.add("a quest is not an object");
                continue;
            }
            JsonObject o = el.getAsJsonObject();
            String id = string(o, "id");
            if (!Ids.valid(Ids.QUEST, id)) {
                errors.add("quest id " + (id == null ? "is missing" : "'" + id + "' " + Ids.rule(Ids.QUEST)));
                continue;
            }
            String where = "quest '" + id + "'";
            int before = errors.size();
            if (!ids.add(id)) {
                errors.add("duplicate quest id '" + id + "'");
            }
            String title = string(o, "title");
            if (title == null || title.isBlank()) {
                errors.add(where + ": missing 'title'");
            }
            String icon = optional(o, "icon");
            if (!icon.isEmpty() && !ITEM.matcher(icon).matches()) {
                errors.add(where + ": icon '" + icon + "' is not an item id like minecraft:wheat");
            }
            String text = string(o, "text");
            String folder = Folders.placement(o, folders, where, errors);
            List<QuestDoc.Goal> goals = goals(o, where, errors);
            List<QuestDoc.Stack> rewards = stacks(o, "rewards", ITEM_WITH_COMPONENTS, where, errors);
            if (errors.size() == before) {
                quests.add(new QuestDoc.Quest(id, title.trim(), icon, text == null ? "" : text, goals, rewards, folder));
            }
        }
        if (!errors.isEmpty()) {
            throw new DocumentException(errors);
        }
        return new QuestDoc(folders, List.copyOf(quests));
    }

    /**
     * Goals: each names exactly one of {@code item} (hand in; an item condition as
     * {@code /clear} reads it) or {@code kill} (an entity type id). Kill goals must name
     * different entities, because progress is saved per entity.
     */
    private static List<QuestDoc.Goal> goals(JsonObject o, String where, List<String> errors) {
        JsonElement e = o.get("goals");
        if (e == null || !e.isJsonArray()) {
            errors.add(where + ": missing 'goals' list");
            return List.of();
        }
        List<QuestDoc.Goal> out = new ArrayList<>();
        Set<String> killed = new HashSet<>();
        for (JsonElement g : e.getAsJsonArray()) {
            if (!g.isJsonObject()) {
                errors.add(where + ": a goal is not an object");
                continue;
            }
            JsonObject go = g.getAsJsonObject();
            String item = optional(go, QuestDoc.Goal.Kind.ITEM.key);
            String kill = optional(go, QuestDoc.Goal.Kind.KILL.key);
            if (item.isEmpty() == kill.isEmpty()) {
                errors.add(where + ": a goal needs exactly one of 'item' or 'kill'");
                continue;
            }
            String target = item.isEmpty() ? kill : item;
            // An item goal is a condition as /clear reads it (minecraft:wheat,
            // minecraft:iron_sword[custom_data={...}], #minecraft:logs ...); the
            // game's parser checks it on publish.
            if (!kill.isEmpty() && !ITEM.matcher(kill).matches()) {
                errors.add(where + ": goal kill '" + kill + "' is not an entity id like minecraft:wolf");
                continue;
            }
            int count = count(go.get("count"));
            if (count == 0) {
                errors.add(where + ": goal count of '" + target + "' must be a whole number from 1 to " + MAX_COUNT);
                continue;
            }
            if (!kill.isEmpty() && !killed.add(kill)) {
                errors.add(where + ": two kill goals for '" + kill + "'; use one with the total count");
                continue;
            }
            out.add(item.isEmpty() ? QuestDoc.Goal.kill(kill, count) : QuestDoc.Goal.item(item, count));
        }
        return List.copyOf(out);
    }

    /** A whole number from 1 to {@link #MAX_COUNT}, or 0 if it is not one. */
    private static int count(JsonElement c) {
        if (c != null && c.isJsonPrimitive() && c.getAsJsonPrimitive().isNumber()) {
            double d = c.getAsDouble();
            return (d == Math.rint(d) && d >= 1 && d <= MAX_COUNT) ? (int) d : 0;
        }
        return 0;
    }

    private static List<QuestDoc.Stack> stacks(JsonObject o, String key, Pattern shape, String where, List<String> errors) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonArray()) {
            errors.add(where + ": missing '" + key + "' list");
            return List.of();
        }
        List<QuestDoc.Stack> out = new ArrayList<>();
        for (JsonElement s : e.getAsJsonArray()) {
            if (!s.isJsonObject()) {
                errors.add(where + ": an entry of '" + key + "' is not an object");
                continue;
            }
            String item = optional(s.getAsJsonObject(), "item");
            if (!shape.matcher(item).matches()) {
                errors.add(where + ": " + key + " item " + (item.isEmpty() ? "is missing"
                        : "'" + item + "' is not an item like minecraft:iron_sword or minecraft:iron_sword[...]"));
                continue;
            }
            int count = count(s.getAsJsonObject().get("count"));
            if (count == 0) {
                errors.add(where + ": " + key + " count of '" + item + "' must be a whole number from 1 to " + MAX_COUNT);
                continue;
            }
            out.add(new QuestDoc.Stack(item, count));
        }
        return List.copyOf(out);
    }

    public static String write(QuestDoc doc) {
        JsonArray arr = new JsonArray();
        for (QuestDoc.Quest q : doc.quests()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", q.id());
            o.addProperty("title", q.title());
            if (!q.icon().isEmpty()) {
                o.addProperty("icon", q.icon());
            }
            if (!q.text().isEmpty()) {
                o.addProperty("text", q.text());
            }
            Folders.writePlacement(o, q.folder());
            JsonArray goals = new JsonArray();
            for (QuestDoc.Goal g : q.goals()) {
                JsonObject go = new JsonObject();
                go.addProperty(g.kind().key, g.target());
                go.add("count", new JsonPrimitive(g.count()));
                goals.add(go);
            }
            o.add("goals", goals);
            o.add("rewards", writeStacks(q.rewards()));
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.addProperty("format", VERSION);
        Folders.write(root, doc.folders());
        root.add("quests", arr);
        return GSON.toJson(root);
    }

    private static JsonArray writeStacks(List<QuestDoc.Stack> stacks) {
        JsonArray arr = new JsonArray();
        for (QuestDoc.Stack s : stacks) {
            JsonObject o = new JsonObject();
            o.addProperty("item", s.item());
            o.add("count", new JsonPrimitive(s.count()));
            arr.add(o);
        }
        return arr;
    }

    /** An optional text field: trimmed, "" when absent. */
    private static String optional(JsonObject o, String key) {
        String s = string(o, key);
        return s == null ? "" : s.trim();
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) ? e.getAsString() : null;
    }
}
