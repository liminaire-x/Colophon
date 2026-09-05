package kr.guinnessgroup.colophon.runtime;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs graph executions on the main server thread, one server tick at a time.
 * <p>
 * Each tick, every active execution is advanced node by node. A node that needs
 * to wait returns {@link NodeResult.Suspend}; the execution is parked until its
 * {@link ResumeCondition} reports ready, then continues PAST that node along the
 * suspend's resume port (the node is not re-executed). A per-tick node budget
 * aborts any execution that runs away (e.g. a cycle with no wait).
 */
public final class TickScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX_NODES_PER_TICK = 10_000;

    private final List<Execution> active = new ArrayList<>();

    public synchronized void start(Graph graph, ExecContext ctx, String startNodeId) {
        if (graph.node(startNodeId) == null) {
            LOGGER.warn("[Colophon] start: no node '{}' in graph", startNodeId);
            return;
        }
        active.add(new Execution(graph, ctx, startNodeId));
    }

    public synchronized void tick() {
        if (active.isEmpty()) {
            return;
        }
        Iterator<Execution> it = active.iterator();
        while (it.hasNext()) {
            Execution ex = it.next();
            advance(ex);
            if (ex.state() == Execution.State.DONE || ex.state() == Execution.State.FAILED) {
                it.remove();
            }
        }
    }

    private void advance(Execution ex) {
        // Resume a suspended execution once its condition is ready, then step past
        // the suspending node along the stored resume port.
        if (ex.state() == Execution.State.SUSPENDED) {
            ResumeCondition cond = ex.resumeCondition();
            if (cond != null && !cond.isReady(ex.ctx())) {
                return;
            }
            GraphNode suspended = ex.graph().node(ex.currentNodeId());
            String port = ex.resumePort();
            ex.setResumeCondition(null);
            ex.setResumePort(null);
            ex.setState(Execution.State.RUNNING);
            if (suspended == null) {
                ex.setState(Execution.State.DONE);
                return;
            }
            step(ex, suspended, port);
        }

        int budget = MAX_NODES_PER_TICK;
        while (ex.state() == Execution.State.RUNNING) {
            if (budget-- <= 0) {
                LOGGER.error("[Colophon] Execution exceeded {} nodes in one tick "
                        + "(possible infinite loop); aborting", MAX_NODES_PER_TICK);
                ex.setState(Execution.State.FAILED);
                return;
            }
            GraphNode gn = ex.graph().node(ex.currentNodeId());
            if (gn == null) {
                ex.setState(Execution.State.DONE);
                return;
            }
            NodeResult result;
            try {
                result = gn.node().execute(ex.ctx());
            } catch (Exception e) {
                LOGGER.error("[Colophon] Node '{}' threw; failing execution", gn.id(), e);
                ex.setState(Execution.State.FAILED);
                return;
            }
            switch (result) {
                case NodeResult.Continue ignored -> step(ex, gn, GraphNode.DEFAULT_PORT);
                case NodeResult.Branch b -> step(ex, gn, b.port());
                case NodeResult.Suspend s -> {
                    ex.setResumeCondition(s.until());
                    ex.setResumePort(s.thenPort());
                    ex.setState(Execution.State.SUSPENDED);
                }
                case NodeResult.Done ignored -> ex.setState(Execution.State.DONE);
                case NodeResult.Fail f -> {
                    LOGGER.warn("[Colophon] Execution failed at '{}': {}", gn.id(), f.reason());
                    ex.setState(Execution.State.FAILED);
                }
            }
        }
    }

    private void step(Execution ex, GraphNode from, String port) {
        String next = from.next(port);
        if (next == null) {
            ex.setState(Execution.State.DONE);
        } else {
            ex.setCurrentNodeId(next);
        }
    }

    public synchronized int activeCount() {
        return active.size();
    }

    public synchronized void clear() {
        active.clear();
    }
}
