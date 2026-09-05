package kr.guinnessgroup.colophon.runtime;

/**
 * Outcome of executing a single {@link Node}, telling the {@link TickScheduler}
 * what to do next. Sealed so the scheduler can switch over it exhaustively.
 */
public sealed interface NodeResult {

    /** Continue along the default output port to the next node. */
    record Continue() implements NodeResult {}

    /** Continue along a specific named output port (conditional branch). */
    record Branch(String port) implements NodeResult {}

    /**
     * Pause until {@code until} reports ready (polled each tick); on resume the
     * scheduler advances along {@code thenPort} to the next node (it does not
     * re-execute the suspending node).
     */
    record Suspend(ResumeCondition until, String thenPort) implements NodeResult {}

    /** This flow finished successfully. */
    record Done() implements NodeResult {}

    /** This flow failed; {@code reason} is logged. */
    record Fail(String reason) implements NodeResult {}

    // Convenience factories for node authors.
    static NodeResult cont() { return new Continue(); }
    static NodeResult branch(String port) { return new Branch(port); }
    static NodeResult suspend(ResumeCondition until) { return new Suspend(until, GraphNode.DEFAULT_PORT); }
    static NodeResult suspend(ResumeCondition until, String thenPort) { return new Suspend(until, thenPort); }
    static NodeResult done() { return new Done(); }
    static NodeResult fail(String reason) { return new Fail(reason); }
}
