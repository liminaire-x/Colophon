/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.state.Scope;
import kr.guinnessgroup.colophon.runtime.type.Type;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves data-port values during one flow execution (contract e). A node's data
 * input is read from the producer it is wired to, or from its inline default when
 * unconnected. A producer's output comes from the {@link ValueStore} when the
 * producer is an exec node (pushed after it ran; unset before), or from evaluating
 * the producer when it is a pure node (pull, memoized into the store).
 * <p>
 * Values in the store are runtime objects (the transient per-execution form);
 * {@link Type#resolve} casts/looks them up. A data cycle among pure nodes resolves
 * to unset rather than looping.
 */
public final class ValueResolver {

    private final Graph graph;
    private final ExecContext ctx;
    private final Set<String> evaluating = new HashSet<>(); // pure nodes mid-evaluation (cycle guard)

    public ValueResolver(Graph graph, ExecContext ctx) {
        this.graph = graph;
        this.ctx = ctx;
    }

    /** The typed value of a node's data input: wired producer output, else inline default. */
    public <T> T input(String nodeId, String portId, Type<T> type) {
        GraphNode gn = graph.node(nodeId);
        if (gn == null) {
            return null;
        }
        PortRef src = gn.dataSource(portId);
        Object stored = (src != null)
                ? output(src.nodeId(), src.portId())
                : inlineDefault(gn, portId, type);
        return type.resolve(stored, ctx);
    }

    /** The stored (runtime) value produced at a node's output port, or null if unset. */
    Object output(String nodeId, String portId) {
        if (ctx.values().has(nodeId, portId)) {
            return ctx.values().get(nodeId, portId);
        }
        GraphNode gn = graph.node(nodeId);
        if (gn == null || gn.type().kind() != NodeKind.PURE || gn.pure() == null) {
            return null; // exec output not yet pushed, or missing -> unset
        }
        if (!evaluating.add(nodeId)) {
            return null; // pure data cycle -> unset instead of looping
        }
        try {
            Map<String, Object> outs = gn.pure().evaluate(new PureView(nodeId));
            if (outs != null) {
                for (Map.Entry<String, Object> e : outs.entrySet()) {
                    ctx.values().put(nodeId, e.getKey(), e.getValue());
                }
            }
        } finally {
            evaluating.remove(nodeId);
        }
        return ctx.values().has(nodeId, portId) ? ctx.values().get(nodeId, portId) : null;
    }

    private Object inlineDefault(GraphNode gn, String portId, Type<?> type) {
        // Per-instance inline value (saved config) wins over the descriptor default;
        // both are string literals parsed by the type.
        String literal = null;
        JsonObject config = gn.config();
        if (config != null && config.has(portId) && config.get(portId).isJsonPrimitive()) {
            literal = config.get(portId).getAsString();
        }
        if (literal == null || literal.isEmpty()) {
            literal = gn.type().inputs().stream()
                    .filter(in -> in.id().equals(portId))
                    .findFirst()
                    .map(InputSpec::defaultValue)
                    .orElse(null);
        }
        return type.fromInline(literal);
    }

    /** Read-only pure context bound to one node: its inputs resolve through this resolver. */
    private final class PureView implements PureContext {
        private final String nodeId;

        PureView(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public <T> T get(String portId, Type<T> type) {
            return input(nodeId, portId, type);
        }

        @Override
        public String readVar(Scope scope, String key) {
            if (ctx.storage() == null) {
                return null;
            }
            ServerPlayer actor = ctx.actor();
            UUID uuid = (actor != null) ? actor.getUUID() : null;
            return ctx.storage().get(scope, key, uuid);
        }
    }
}
