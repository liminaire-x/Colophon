/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
    private String resumePort;
    private ResumeAction resumeAction;

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

    public String resumePort() { return resumePort; }

    public void setResumePort(String port) { this.resumePort = port; }

    public ResumeAction resumeAction() { return resumeAction; }

    public void setResumeAction(ResumeAction action) { this.resumeAction = action; }
}
