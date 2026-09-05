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
