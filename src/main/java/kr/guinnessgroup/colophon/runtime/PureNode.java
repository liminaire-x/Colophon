/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.Map;

/**
 * A side-effect-free value producer (contract c): given its resolved data inputs
 * via a read-only {@link PureContext}, it returns its data outputs keyed by port
 * id. This is the counterpart to {@link ExecNode} — a pure node has no flow, no
 * side effects, and is pulled on demand, so it must be synchronous and safe to
 * evaluate any number of times (0..N), which forbids impure sources like random.
 */
public interface PureNode {

    /** Compute this node's data outputs (portId to value) from its inputs. */
    Map<String, Object> evaluate(PureContext ctx);
}
