package kr.guinnessgroup.colophon.runtime;

/**
 * A condition the {@link TickScheduler} polls once per server tick to decide
 * whether to resume a suspended execution.
 * <p>
 * v0/v1 uses polling. If polling proves too coarse, an event-driven wake can
 * replace this without changing the node API.
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
}
