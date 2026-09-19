/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * The two physically distinct kinds of node (contract c). An {@code EXEC} node
 * ({@link ExecNode}) runs in the control-flow graph and may have side effects; a
 * {@code PURE} node is a side-effect-free value producer, pulled on demand. The
 * kinds are separate types so the compiler keeps pure code side-effect-free.
 */
public enum NodeKind {
    EXEC,
    PURE
}
