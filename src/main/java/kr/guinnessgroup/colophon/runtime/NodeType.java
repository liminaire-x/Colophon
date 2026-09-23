/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.graph.GraphFormat;

import java.util.List;

/**
 * A kind of node the editor can place, e.g. "Send Message". One class per kind,
 * registered in {@link NodeRegistry}.
 */
public interface NodeType {

    /** Stored in saved graphs, e.g. {@code colophon:send_message}. Never rename. */
    String id();

    /** Shown in the editor. Free to change. */
    String label();

    /** Editor palette group: trigger / condition / action. */
    String category();

    /** Whether this node starts a graph (fired by an event, has no way in). */
    default boolean trigger() {
        return false;
    }

    /** The ways out. Straight: [next]; a check: [yes, no]; an end: []. */
    default List<String> outs() {
        return List.of(GraphFormat.DEFAULT_OUT);
    }

    /** Settings the editor shows as inputs; saved in the node's config. */
    default List<Field> fields() {
        return List.of();
    }

    /**
     * Build a runnable node from its saved config. Throw
     * {@link IllegalArgumentException} with a readable message if the config is
     * unusable (including a reference missing from {@code catalog}); publish then
     * rejects the graph with that message.
     */
    Node create(JsonObject config, Catalog catalog);
}
