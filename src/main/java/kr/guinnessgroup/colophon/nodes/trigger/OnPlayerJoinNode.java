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

import java.util.List;

/**
 * Trigger: fires when a player joins the server. Entry point (no flow-in).
 * <p>
 * Exposes the joining player as an explicit {@code player} data output (contract
 * d/e). The value is seeded by the runtime when the trigger fires (see
 * {@code ColophonRuntime.fireTrigger}), since only the trigger knows its event's
 * subject; downstream nodes read it as data instead of relying on the implicit
 * acting player.
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
        // Entry point: data output is seeded by the runtime; just pass execution on.
        return ctx -> NodeResult.cont();
    }
}
