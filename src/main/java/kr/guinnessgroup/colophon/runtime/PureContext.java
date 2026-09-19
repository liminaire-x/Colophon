/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * Read-only context handed to a {@link PureNode} while it computes its outputs
 * (contract c). It exposes only the node's resolved data inputs — no storage
 * writes, no player, no flow — so the compiler keeps pure evaluation
 * side-effect-free. Inputs are resolved by pulling upstream producers; an input
 * with no producer (or an unresolved reference) reads as unset.
 */
public interface PureContext {

    /** The resolved value of a data input port, or {@code null} if it is unset. */
    Object input(String portId);

    /** Whether the given data input port resolved to a value. */
    boolean hasInput(String portId);
}
