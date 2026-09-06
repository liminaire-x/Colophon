/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.state;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import kr.guinnessgroup.colophon.runtime.state.Scope;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Flow: branches true/false on a stored variable. With no {@code expected} value
 * it checks existence; with one it checks string equality. Read-only &mdash; it
 * decides the path but does not pass the value on (that is a data port, v2).
 */
public final class HasVariableNode implements NodeType {

    @Override public String id() { return "has_variable"; }
    @Override public String label() { return "Has Variable"; }
    @Override public String category() { return "state"; }
    @Override public List<FieldSpec> fields() {
        return List.of(
                new FieldSpec("scope", "enum", "PLAYER", List.of("PLAYER", "GLOBAL", "LOCAL")),
                new FieldSpec("key", "string", ""),
                new FieldSpec("expected", "string", ""));
    }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("true", "false"); }

    @Override
    public Node create(JsonObject config) {
        final Scope scope = Scope.parse(readString(config, "scope"), Scope.PLAYER);
        final String key = readString(config, "key");
        final String expected = readString(config, "expected");
        return ctx -> {
            ServerPlayer actor = ctx.actor();
            UUID uuid = (actor != null) ? actor.getUUID() : null;
            String actual = ctx.storage().get(scope, key, uuid);
            boolean result = expected.isEmpty() ? (actual != null) : expected.equals(actual);
            return NodeResult.branch(result ? "true" : "false");
        };
    }

    private static String readString(JsonObject config, String k) {
        return (config != null && config.has(k) && !config.get(k).isJsonNull())
                ? config.get(k).getAsString() : "";
    }
}
