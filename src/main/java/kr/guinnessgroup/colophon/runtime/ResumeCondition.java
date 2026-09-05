/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import java.util.concurrent.CompletableFuture;

/**
 * A condition the {@link TickScheduler} polls once per server tick to decide
 * whether to resume a suspended execution.
 */
@FunctionalInterface
public interface ResumeCondition {

    boolean isReady(ExecContext ctx);

    /** Resume once {@code ticks} server ticks have elapsed (20 ticks = 1 second). */
    static ResumeCondition afterTicks(int ticks) {
        final long[] target = { Long.MIN_VALUE };
        return ctx -> {
            long now = (ctx.server() != null) ? ctx.server().getTickCount() : 0L;
            if (target[0] == Long.MIN_VALUE) {
                target[0] = now + Math.max(0, ticks);
            }
            return now >= target[0];
        };
    }

    /** Resume once the future has completed (normally, exceptionally, or cancelled). */
    static ResumeCondition whenDone(CompletableFuture<?> future) {
        return ctx -> future.isDone();
    }

    /** Resume once the future completes OR {@code maxTicks} elapse, whichever comes first. */
    static ResumeCondition whenDoneOrAfter(CompletableFuture<?> future, int maxTicks) {
        ResumeCondition timeout = afterTicks(maxTicks);
        return ctx -> future.isDone() || timeout.isReady(ctx);
    }
}
