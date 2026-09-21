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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure node (contract e): interpolates a template into a string — the one explicit
 * value&rarr;text node. Its data inputs are <em>dynamic</em>: each {@code {name}}
 * token in the template becomes a connectable string input port, derived per
 * instance from config via {@link #instanceInputs}. An unset token resolves to the
 * empty string. All token inputs are string-typed; a number producer must be
 * converted first (nominal type match, no implicit cast — see the deferred item in
 * ADR 0003).
 */
public final class FormatTextNode implements PureNodeType {

    /** A template placeholder: {@code {name}} where name is a valid port id. */
    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    @Override public String id() { return "format_text"; }
    @Override public String label() { return "Format Text"; }
    @Override public String category() { return "flow"; }

    /** Static schema: only the template knob. The token inputs are instance-derived. */
    @Override public List<InputSpec> inputs() {
        return List.of(InputSpec.knob("template", "string", ""));
    }

    /** Template knob + one connectable string input per distinct {@code {token}}. */
    @Override public List<InputSpec> instanceInputs(JsonObject config) {
        List<InputSpec> result = new ArrayList<>(inputs());
        for (String token : tokens(template(config))) {
            result.add(InputSpec.data(token, "string", token));
        }
        return result;
    }

    @Override public List<DataPort> dataOutPorts() {
        return List.of(new DataPort("text", "string", "Text"));
    }

    @Override
    public PureNode createPure(JsonObject config) {
        final String template = template(config);
        final Set<String> tokens = tokens(template);
        return ctx -> {
            String out = template;
            for (String token : tokens) {
                String value = ctx.get(token, Types.STRING);
                out = out.replace("{" + token + "}", value == null ? "" : value);
            }
            return Map.of("text", out);
        };
    }

    /** Distinct token names in template order. */
    private static Set<String> tokens(String template) {
        Set<String> names = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(template);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    private static String template(JsonObject config) {
        return (config != null && config.has("template") && !config.get("template").isJsonNull())
                ? config.get("template").getAsString() : "";
    }
}
