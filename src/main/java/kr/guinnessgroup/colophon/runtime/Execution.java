package kr.guinnessgroup.colophon.runtime;

/**
 * One in-flight run of a graph: a pointer walking downstream from a start node,
 * plus the state the {@link TickScheduler} needs to advance or resume it.
 */
public final class Execution {

    public enum State { RUNNING, SUSPENDED, DONE, FAILED }

    private final Graph graph;
    private final ExecContext ctx;
    private String currentNodeId;
    private State state = State.RUNNING;
    private ResumeCondition resumeCondition;

    public Execution(Graph graph, ExecContext ctx, String startNodeId) {
        this.graph = graph;
        this.ctx = ctx;
        this.currentNodeId = startNodeId;
    }

    public Graph graph() { return graph; }

    public ExecContext ctx() { return ctx; }

    public String currentNodeId() { return currentNodeId; }

    public void setCurrentNodeId(String id) { this.currentNodeId = id; }

    public State state() { return state; }

    public void setState(State state) { this.state = state; }

    public ResumeCondition resumeCondition() { return resumeCondition; }

    public void setResumeCondition(ResumeCondition c) { this.resumeCondition = c; }
}
