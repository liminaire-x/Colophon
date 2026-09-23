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
     * @param goals   items to hand in (all of them)
     * @param rewards items given on completion
     */
    public record Quest(String id, String title, String icon, String text, List<Stack> goals, List<Stack> rewards) {}

    /**
     * Some number of one item, e.g. {@code minecraft:wheat} × 10. A reward item may be
     * written as {@code /give} writes it, with components (name, enchantments, data
     * from other mods): {@code minecraft:iron_sword[custom_name=...]}. Goals are plain ids.
     */
    public record Stack(String item, int count) {}

    public Quest find(String id) {
        for (Quest q : quests) {
            if (q.id().equals(id)) {
                return q;
            }
        }
        return null;
    }
}
