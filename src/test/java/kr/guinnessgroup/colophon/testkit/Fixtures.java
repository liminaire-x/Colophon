/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.testkit;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.ExecContext;
import kr.guinnessgroup.colophon.runtime.Graph;
import kr.guinnessgroup.colophon.runtime.GraphNode;
import kr.guinnessgroup.colophon.runtime.PortRef;
import kr.guinnessgroup.colophon.runtime.PureContext;
import kr.guinnessgroup.colophon.runtime.PureNodeType;
import kr.guinnessgroup.colophon.runtime.ValueResolver;
import kr.guinnessgroup.colophon.runtime.type.Type;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central test fixtures (ADR 0004): assembling an {@link ExecContext}, a
 * {@link Graph} of node instances and a bound {@link ValueResolver} lives here in
 * ONE place. If a runtime constructor changes, this helper is the single edit —
 * individual tests keep working. Tier 1 needs no MC, so the context is built with
 * null server/actor/storage; tier 2 will add an in-memory StorageService here.
 */
public final class Fixtures {

    private Fixtures() {}

    /** A per-execution context with no MC server, actor or storage (tier 1). */
    public static ExecContext ctx() {
        return new ExecContext(null, null, null);
    }

    /** Builds a config object from alternating key/value string pairs. */
    public static JsonObject config(String... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            o.addProperty(kv[i], kv[i + 1]);
        }
        return o;
    }

    /** A pure node instance, optionally wired from upstream producers by data source. */
    public static GraphNode pure(String id, PureNodeType type, JsonObject config,
                                 Map<String, PortRef> dataSources) {
        return new GraphNode(id, type, null, type.createPure(config), config, Map.of(), dataSources);
    }

    public static GraphNode pure(String id, PureNodeType type, JsonObject config) {
        return pure(id, type, config, Map.of());
    }

    /** A graph from node instances, keyed by id in insertion order. */
    public static Graph graph(GraphNode... nodes) {
        Map<String, GraphNode> m = new LinkedHashMap<>();
        for (GraphNode n : nodes) {
            m.put(n.id(), n);
        }
        return new Graph(m);
    }

    /** A resolver bound to the context (as the scheduler would bind it at runtime). */
    public static ValueResolver resolver(Graph graph, ExecContext ctx) {
        ValueResolver r = new ValueResolver(graph, ctx);
        ctx.bindResolver(r);
        return r;
    }

    /** A read-only pure context whose inputs come from a fixed, already-typed map. */
    public static PureContext pureContext(Map<String, Object> values) {
        return new PureContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(String portId, Type<T> type) {
                return (T) values.get(portId);
            }
        };
    }
}
