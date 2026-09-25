/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.npc;

import kr.guinnessgroup.lorebench.Folders.Folder;

import java.util.List;

/**
 * The saved NPC document ({@code npcs.json}): every NPC definition, authored in
 * the editor. A definition is placed in worlds any number of times; placements are
 * records, not part of this document. See docs/decisions/0002-npc.md.
 */
public record NpcDoc(List<Folder> folders, List<NpcDef> npcs) {

    public static final NpcDoc EMPTY = new NpcDoc(List.of(), List.of());

    /**
     * @param id    stable; graphs refer to it
     * @param name  shown above the NPC
     * @param model GeckoLib model name, or "" for the default look. Files come from
     *              resource packs: {@code assets/lorebench/geo/npc/<model>.geo.json},
     *              {@code animations/npc/<model>.animation.json}, {@code textures/npc/<model>.png}
     * @param idle  animation looped while nothing else plays, or ""
     * @param folder the editor folder id it sits in, or "" for the top ({@link kr.guinnessgroup.lorebench.Folders})
     */
    public record NpcDef(String id, String name, String model, String idle, String folder) {

        public NpcDef(String id, String name) {
            this(id, name, "", "", "");
        }

        public NpcDef(String id, String name, String model, String idle) {
            this(id, name, model, idle, "");
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
