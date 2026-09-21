/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.flow;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.DataPort;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.PureNode;
import kr.guinnessgroup.colophon.runtime.PureNodeType;
import kr.guinnessgroup.colophon.runtime.type.Types;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure node (contract e): breaks a player reference into selectable scalar outputs
 * — the reference&rarr;value decomposition (blueprint "Break Struct" pattern). Its
 * outputs are <em>dynamic</em>: {@link #dataOutPorts()} is the full catalog, and
 * {@link #instanceOutputs} narrows it to the fields chosen in {@code config.fields}
 * (comma-separated), the output counterpart to {@code format_text}'s dynamic inputs.
 * An unset (offline/unresolved) player produces no outputs.
 */
public final class PlayerInfoNode implements PureNodeType {

    /** The full catalog of decomposable player fields, in display order. */
    private static final List<DataPort> CATALOG = List.of(
            new DataPort("name", "string", "Name"),
            new DataPort("x", "number", "X"),
            new DataPort("y", "number", "Y"),
            new DataPort("z", "number", "Z"));

    @Override public String id() { return "player_info"; }
    @Override public String label() { return "Player Info"; }
    @Override public String category() { return "flow"; }

    @Override public List<InputSpec> inputs() {
        return List.of(
                InputSpec.data("player", "colophon:player", "Player"),
                InputSpec.knob("fields", "string", "name"));
    }

    /** The full catalog; the editor uses it as the pick list. */
    @Override public List<DataPort> dataOutPorts() {
        return CATALOG;
    }

    /** Only the selected fields become live output ports for this instance. */
    @Override public List<DataPort> instanceOutputs(JsonObject config) {
        Set<String> selected = fields(config);
        List<DataPort> out = new ArrayList<>();
        for (DataPort p : CATALOG) {
            if (selected.contains(p.id())) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public PureNode createPure(JsonObject config) {
        final Set<String> fields = fields(config);
        return ctx -> {
            ServerPlayer p = ctx.get("player", Types.PLAYER);
            if (p == null) {
                return Map.of(); // unresolved player -> no outputs
            }
            Map<String, Object> out = new java.util.HashMap<>();
            if (fields.contains("name")) out.put("name", p.getName().getString());
            if (fields.contains("x")) out.put("x", p.getX());
            if (fields.contains("y")) out.put("y", p.getY());
            if (fields.contains("z")) out.put("z", p.getZ());
            return out;
        };
    }

    /** Selected field ids from the comma-separated {@code fields} config. */
    private static Set<String> fields(JsonObject config) {
        Set<String> names = new LinkedHashSet<>();
        String raw = (config != null && config.has("fields") && !config.get("fields").isJsonNull())
                ? config.get("fields").getAsString() : "";
        for (String part : raw.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                names.add(t);
            }
        }
        return names;
    }
}
