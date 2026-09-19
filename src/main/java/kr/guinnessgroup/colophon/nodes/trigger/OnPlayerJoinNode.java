/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.trigger;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

import java.util.List;

/** Trigger: fires when a player joins the server. Entry point (no flow-in). */
public final class OnPlayerJoinNode implements NodeType {

    @Override public String id() { return "on_player_join"; }
    @Override public String label() { return "On Player Join"; }
    @Override public String category() { return "trigger"; }
    @Override public List<FieldSpec> fields() { return List.of(); }
    @Override public boolean hasFlowIn() { return false; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        // Entry point: just pass execution downstream.
        return ctx -> NodeResult.cont();
    }
}
