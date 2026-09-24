/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.lorebench.runtime.Catalog;
import kr.guinnessgroup.lorebench.runtime.Node;
import kr.guinnessgroup.lorebench.runtime.NodeResult;
import kr.guinnessgroup.lorebench.runtime.NodeType;

/** Starts a graph when a player joins. The joining player is the run's player. */
public final class OnPlayerJoin implements NodeType {

    public static final String ID = "lorebench:on_player_join";

    @Override public String id() { return ID; }
    @Override public String label() { return "On Player Join"; }
    @Override public String category() { return "trigger"; }
    @Override public boolean trigger() { return true; }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        return ctx -> NodeResult.next();
    }
}
