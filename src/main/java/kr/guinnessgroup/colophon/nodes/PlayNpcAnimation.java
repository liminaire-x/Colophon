/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.npc.Npcs;
import kr.guinnessgroup.colophon.runtime.Catalog;
import kr.guinnessgroup.colophon.runtime.Field;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

import java.util.List;
import java.util.UUID;

/**
 * Plays an NPC animation once, then the NPC returns to its idle animation. If this
 * run started from clicking that NPC, only the clicked placement plays; otherwise
 * every loaded placement of the NPC does.
 */
public final class PlayNpcAnimation implements NodeType {

    private static final Field NPC = Field.npc("npc", "NPC");
    private static final Field ANIMATION = Field.text("animation", "Animation");

    @Override public String id() { return "colophon:play_npc_animation"; }
    @Override public String label() { return "Play NPC Animation"; }
    @Override public String category() { return "action"; }
    @Override public List<Field> fields() { return List.of(NPC, ANIMATION); }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        String npc = NPC.read(config);
        String animation = ANIMATION.read(config).trim();
        if (npc.isBlank()) {
            throw new IllegalArgumentException("choose an NPC");
        }
        if (!catalog.hasNpc(npc)) {
            throw new IllegalArgumentException("no NPC with id '" + npc + "'");
        }
        if (animation.isEmpty()) {
            throw new IllegalArgumentException("animation name is empty");
        }
        return ctx -> {
            Npcs npcs = Npcs.current();
            if (npcs == null) {
                return NodeResult.fail("NPCs are not running");
            }
            String clicked = ctx.event(OnNpcInteract.EVENT_NPC_ENTITY);
            npcs.playAnimation(ctx.server(), npc, clicked == null ? null : UUID.fromString(clicked), animation);
            return NodeResult.next();
        };
    }
}
