/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import kr.guinnessgroup.colophon.runtime.type.Type;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Convenience helpers for authoring nodes. Part of the SDK surface that add-ons
 * build against, so common patterns (like awaiting an async operation) don't
 * have to be re-implemented per node.
 */
public final class Nodes {

    private Nodes() {}

    /**
     * A {@link ExecNode} that starts an asynchronous operation and suspends until it
     * completes, then continues along the default output ("out"). If {@code op}
     * returns {@code null} (nothing to wait on), the flow continues immediately.
     * <p>
     * Only for single-output ("out") actions. Nodes that must branch on an async
     * value need a different pattern (see backlog: data ports / two-phase nodes).
     */
    public static ExecNode awaitAction(Function<ExecContext, CompletableFuture<?>> op) {
        return ctx -> {
            CompletableFuture<?> future = op.apply(ctx);
            return (future == null) ? NodeResult.cont() : NodeResult.suspend(ResumeCondition.whenDone(future));
        };
    }

    /**
     * An {@link ExecNode} that starts an async operation, suspends until it completes,
     * then pushes its result into the given data output port (contract e) and continues
     * along "out". This is the exec async-value pattern: the node is not re-executed on
     * resume, so the value is pushed by a resume action. A {@code null} future continues
     * immediately with the output unset; a future that fails (or yields null) leaves the
     * output unset (defined result, never crashes the flow).
     */
    public static <T> ExecNode awaitValue(String port, Type<T> type,
                                          Function<ExecContext, CompletableFuture<T>> op) {
        return ctx -> {
            CompletableFuture<T> future = op.apply(ctx);
            if (future == null) {
                return NodeResult.cont();
            }
            return NodeResult.suspend(ResumeCondition.whenDone(future), GraphNode.DEFAULT_PORT, resumeCtx -> {
                T value;
                try {
                    value = future.getNow(null); // completed by now; null if it failed/absent
                } catch (RuntimeException e) {
                    value = null; // exceptional completion -> unset
                }
                if (value != null) {
                    resumeCtx.set(port, type, value);
                }
            });
        };
    }
}
