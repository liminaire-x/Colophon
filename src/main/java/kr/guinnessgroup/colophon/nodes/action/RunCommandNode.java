package kr.guinnessgroup.colophon.nodes.action;

import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
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
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("command", "string", "")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
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
