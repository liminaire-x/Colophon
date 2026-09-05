package kr.guinnessgroup.colophon.runtime;

/**
 * A condition the {@link TickScheduler} polls once per server tick to decide
 * whether to resume a suspended execution.
 * <p>
 * v0 uses polling for simplicity. If polling proves too coarse or wasteful, an
 * event-driven wake mechanism can replace this without changing the node API.
 */
@FunctionalInterface
public interface ResumeCondition {

    boolean isReady(ExecContext ctx);

    /** Resume after {@code ticks} server ticks have elapsed (20 ticks = 1 second). */
    static ResumeCondition afterTicks(int ticks) {
        final int[] remaining = { ticks };
        return ctx -> remaining[0]-- <= 0;
    }
}
