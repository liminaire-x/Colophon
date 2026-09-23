/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.Catalog;
import kr.guinnessgroup.colophon.runtime.Field;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

import java.util.List;

/**
 * Starts a graph when a player right-clicks the chosen NPC (any of its
 * placements). The clicking player is the run's player.
 */
public final class OnNpcInteract implements NodeType {

    public static final String ID = "colophon:on_npc_interact";

    /** Event fact: which NPC was clicked. */
    public static final String EVENT_NPC = "npc";

    /** Event fact: which placement (entity UUID) was clicked. */
    public static final String EVENT_NPC_ENTITY = "npc_entity";

    private static final Field NPC = Field.npc("npc", "NPC");

    @Override public String id() { return ID; }
    @Override public String label() { return "On NPC Interact"; }
    @Override public String category() { return "trigger"; }
    @Override public boolean trigger() { return true; }
    @Override public List<Field> fields() { return List.of(NPC); }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        String npc = NPC.read(config);
        if (npc.isBlank()) {
            throw new IllegalArgumentException("choose an NPC");
        }
        if (!catalog.hasNpc(npc)) {
            throw new IllegalArgumentException("no NPC with id '" + npc + "'");
        }
        return ctx -> npc.equals(ctx.event(EVENT_NPC)) ? NodeResult.next() : NodeResult.stop();
    }
}
