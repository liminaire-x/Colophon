/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;

/**
 * A node type that is a side-effect-free value producer (contract a/c): no flow, it
 * builds a {@link PureNode} that is pulled on demand. Having no flow methods is what
 * makes purity compiler-enforced — a pure node cannot declare or drive control flow.
 */
public interface PureNodeType extends NodeType {

    /** Builds the runnable pure node from saved config. */
    PureNode createPure(JsonObject config);

    @Override
    default NodeKind kind() {
        return NodeKind.PURE;
    }
}
