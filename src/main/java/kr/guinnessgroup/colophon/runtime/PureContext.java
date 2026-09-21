/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.runtime.state.Scope;
import kr.guinnessgroup.colophon.runtime.type.Type;

/**
 * Read-only context handed to a {@link PureNode} while it computes its outputs
 * (contract c/e). It exposes the node's resolved data inputs and a read-only view
 * of stored state — no writes, no flow — so the compiler keeps pure evaluation
 * side-effect-free. Inputs are resolved by pulling upstream producers; an input
 * with no producer (or an unresolved reference) reads as unset ({@code null}).
 */
public interface PureContext {

    /** The typed value of a data input port, or {@code null} if it is unset. */
    <T> T get(String portId, Type<T> type);

    /**
     * Reads a stored variable (contract e, pure): a read is not a side effect, so a
     * pure node may consult state. {@code PLAYER} scope resolves against the acting
     * player. Returns {@code null} when absent or unavailable (defined-result, never
     * throws). Default returns {@code null} so older contexts stay compatible.
     */
    default String readVar(Scope scope, String key) {
        return null;
    }
}
