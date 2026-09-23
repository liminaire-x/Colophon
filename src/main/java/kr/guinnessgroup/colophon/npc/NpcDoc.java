/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import java.util.List;

/**
 * The saved NPC document ({@code npcs.json}): every NPC definition, authored in
 * the editor. A definition is placed in worlds any number of times; placements are
 * records, not part of this document. See docs/decisions/0002-npc.md.
 */
public record NpcDoc(List<NpcDef> npcs) {

    /**
     * @param id    stable; graphs refer to it
     * @param name  shown above the NPC
     * @param model GeckoLib model name, or "" for the default look. Files come from
     *              resource packs: {@code assets/colophon/geo/npc/<model>.geo.json},
     *              {@code animations/npc/<model>.animation.json}, {@code textures/npc/<model>.png}
     * @param idle  animation looped while nothing else plays, or ""
     */
    public record NpcDef(String id, String name, String model, String idle) {

        public NpcDef(String id, String name) {
            this(id, name, "", "");
        }
    }

    public NpcDef find(String id) {
        for (NpcDef n : npcs) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }
}
