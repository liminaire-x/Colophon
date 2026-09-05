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
 * {@link ResumeCondition} reports ready. A per-tick node budget aborts any
 * execution that runs away (e.g. a cycle with no wait) so it cannot freeze the
 * server thread.
 */
public final class TickScheduler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Max nodes a single execution may advance in one tick before it is aborted. */
    private static final int MAX_NODES_PER_TICK = 10_000;

    private final List<Execution> active = new ArrayList<>();

    /** Begin a new execution starting at the given node id. */
    public synchronized void start(Graph graph, ExecContext ctx, String startNodeId) {
        if (graph.node(startNodeId) == null) {
            LOGGER.warn("[Colophon] start: no node '{}' in graph", startNodeId);
            return;
        }
        active.add(new Execution(graph, ctx, startNodeId));
    }

    /** Advance all active executions by one tick. Call from ServerTickEvent.Post. */
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
        // A suspended execution only wakes when its condition is ready.
        if (ex.state() == Execution.State.SUSPENDED) {
            ResumeCondition cond = ex.resumeCondition();
            if (cond != null && !cond.isReady(ex.ctx())) {
                return;
            }
            ex.setState(Execution.State.RUNNING);
            ex.setResumeCondition(null);
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
            ex.setState(Execution.State.DONE); // no downstream node = flow ends
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
