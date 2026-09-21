/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.state;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.DataPort;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.PureNode;
import kr.guinnessgroup.colophon.runtime.PureNodeType;
import kr.guinnessgroup.colophon.runtime.state.Scope;

import java.util.List;
import java.util.Map;

/**
 * Pure node (contract e): reads a stored variable and produces its string value.
 * A read is side-effect-free, so state may be consulted from a pure pull via
 * {@link kr.guinnessgroup.colophon.runtime.PureContext#readVar}. {@code PLAYER}
 * scope resolves against the acting player. An absent variable yields unset (no
 * value produced), never an error.
 */
public final class GetVariableNode implements PureNodeType {

    @Override public String id() { return "get_variable"; }
    @Override public String label() { return "Get Variable"; }
    @Override public String category() { return "state"; }
    @Override public List<InputSpec> inputs() {
        return List.of(
                InputSpec.enumKnob("scope", "PLAYER", List.of("PLAYER", "GLOBAL", "LOCAL")),
                InputSpec.knob("key", "string", ""));
    }
    @Override public List<DataPort> dataOutPorts() {
        return List.of(new DataPort("value", "string", "Value"));
    }

    @Override
    public PureNode createPure(JsonObject config) {
        final Scope scope = Scope.parse(readString(config, "scope"), Scope.PLAYER);
        final String key = readString(config, "key");
        return ctx -> {
            String value = ctx.readVar(scope, key);
            return (value == null) ? Map.of() : Map.of("value", value);
        };
    }

    private static String readString(JsonObject config, String k) {
        return (config != null && config.has(k) && !config.get(k).isJsonNull())
                ? config.get(k).getAsString() : "";
    }
}
