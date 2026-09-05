/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * A single executable node in a graph.
 * <p>
 * Implementations run on the main server thread and must return quickly: never
 * block or sleep. To wait for time or a condition, return
 * {@link NodeResult#suspend(ResumeCondition)} instead.
 */
public interface Node {

    NodeResult execute(ExecContext ctx);
}
