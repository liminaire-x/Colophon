/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

import java.util.List;

/**
 * The saved quest document ({@code quests.json}): every quest, authored in the
 * editor. Who has which quest is a record, not part of this document.
 * See docs/decisions/0005-quests.md.
 */
public record QuestDoc(List<Quest> quests) {

    /**
     * @param id      stable; graphs and players' records refer to it
     * @param title   shown in the quest screen
     * @param icon    item id shown in the quest list, or "" for the first goal's item
     * @param text    the quest's story text, may be ""
     * @param goals   all must be met, shown in this order
     * @param rewards items given on completion
     */
    public record Quest(String id, String title, String icon, String text, List<Goal> goals, List<Stack> rewards) {}

    /**
     * Some number of one item, e.g. {@code minecraft:emerald} × 5. A reward item may be
     * written as {@code /give} writes it, with components (name, enchantments, data
     * from other mods): {@code minecraft:iron_sword[custom_name=...]}.
     */
    public record Stack(String item, int count) {}

    /**
     * One thing a quest asks for. Saved as {@code {"item": "minecraft:wheat", "count": 10}}
     * (hand in: an item condition as {@code /clear} reads it; listed components must
     * match, others are ignored) or {@code {"kill": "minecraft:wolf", "count": 3}}
     * (kill while the quest is active; the count is the player's progress record).
     *
     * @param target an item id or an entity type id, depending on {@code kind}
     */
    public record Goal(Kind kind, String target, int count) {

        public enum Kind {
            ITEM("item"),
            KILL("kill");

            /** The key that names the target in the saved goal. Never rename. */
            public final String key;

            Kind(String key) {
                this.key = key;
            }
        }

        public static Goal item(String item, int count) {
            return new Goal(Kind.ITEM, item, count);
        }

        public static Goal kill(String entity, int count) {
            return new Goal(Kind.KILL, entity, count);
        }
    }

    public Quest find(String id) {
        for (Quest q : quests) {
            if (q.id().equals(id)) {
                return q;
            }
        }
        return null;
    }
}
