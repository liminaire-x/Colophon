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

    /** {@code id} is stable (graphs refer to it); {@code name} is shown above the NPC. */
    public record NpcDef(String id, String name) {}

    public NpcDef find(String id) {
        for (NpcDef n : npcs) {
            if (n.id().equals(id)) {
                return n;
            }
        }
        return null;
    }
}
