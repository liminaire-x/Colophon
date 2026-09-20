/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.trigger;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.DataPort;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.ExecNodeType;
import kr.guinnessgroup.colophon.runtime.type.Types;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Trigger: fires when a player joins the server. Entry point (no flow-in).
 * <p>
 * Exposes the joining player as an explicit data output (contract d/e): the exec
 * node pushes the acting player so downstream nodes can read the subject as a
 * value instead of relying on the implicit acting player. If there is no acting
 * player the output stays unset (absent), not a stored null.
 */
public final class OnPlayerJoinNode implements ExecNodeType {

    @Override public String id() { return "on_player_join"; }
    @Override public String label() { return "On Player Join"; }
    @Override public String category() { return "trigger"; }
    @Override public List<InputSpec> inputs() { return List.of(); }
    @Override public List<DataPort> dataOutPorts() {
        return List.of(new DataPort("player", "colophon:player", "Player"));
    }
    @Override public boolean hasFlowIn() { return false; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        // Entry point: push the joining player as data, then pass execution downstream.
        return ctx -> {
            ServerPlayer player = ctx.actor();
            if (player != null) {
                ctx.set("player", Types.PLAYER, player);
            }
            return NodeResult.cont();
        };
    }
}
