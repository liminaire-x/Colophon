/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.Catalog;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;

/** Starts a graph when a player joins. The joining player is the run's player. */
public final class OnPlayerJoin implements NodeType {

    public static final String ID = "colophon:on_player_join";

    @Override public String id() { return ID; }
    @Override public String label() { return "On Player Join"; }
    @Override public String category() { return "trigger"; }
    @Override public boolean trigger() { return true; }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        return ctx -> NodeResult.next();
    }
}
