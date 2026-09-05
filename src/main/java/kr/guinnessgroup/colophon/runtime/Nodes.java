package kr.guinnessgroup.colophon.runtime;

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
     * A {@link Node} that starts an asynchronous operation and suspends until it
     * completes, then continues along the default output ("out"). If {@code op}
     * returns {@code null} (nothing to wait on), the flow continues immediately.
     * <p>
     * Only for single-output ("out") actions. Nodes that must branch on an async
     * value need a different pattern (see backlog: data ports / two-phase nodes).
     */
    public static Node awaitAction(Function<ExecContext, CompletableFuture<?>> op) {
        return ctx -> {
            CompletableFuture<?> future = op.apply(ctx);
            return (future == null) ? NodeResult.cont() : NodeResult.suspend(ResumeCondition.whenDone(future));
        };
    }
}
