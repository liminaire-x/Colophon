/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.action;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;

/** Action: runs a server command as the console (permission level 4). */
public final class RunCommandNode implements NodeType {

    @Override public String id() { return "run_command"; }
    @Override public String label() { return "Run Command"; }
    @Override public String category() { return "action"; }
    @Override public List<InputSpec> inputs() { return List.of(InputSpec.knob("command", "string", "")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        final String command = (config != null && config.has("command") && !config.get("command").isJsonNull())
                ? config.get("command").getAsString() : "";
        return ctx -> {
            MinecraftServer server = ctx.server();
            if (server != null && !command.isBlank()) {
                CommandSourceStack source = server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source, command);
            }
            return NodeResult.cont();
        };
    }
}
