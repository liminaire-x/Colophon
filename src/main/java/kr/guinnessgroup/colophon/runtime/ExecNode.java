/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

/**
 * A single flow (execution) node in a graph: it runs when control flow reaches
 * it, may have side effects, and returns a {@link NodeResult} telling the
 * {@link TickScheduler} where to go next. This is one of the two node kinds
 * (contract c); the other is {@code PureNode}, a side-effect-free value producer.
 * <p>
 * Implementations run on the main server thread and must return quickly: never
 * block or sleep. To wait for time or a condition, return
 * {@link NodeResult#suspend(ResumeCondition)} instead.
 */
public interface ExecNode {

    NodeResult execute(ExecContext ctx);
}
