package kr.guinnessgroup.colophon.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds the known node types and exposes their schema to the editor. */
public final class NodeRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, NodeType> TYPES = new LinkedHashMap<>();

    private NodeRegistry() {}

    public static void register(NodeType type) {
        TYPES.put(type.id(), type);
    }

    public static NodeType get(String id) {
        return TYPES.get(id);
    }

    public static Collection<NodeType> all() {
        return TYPES.values();
    }

    /** Registers the built-in node types. Call once during mod setup. */
    public static void registerBuiltins() {
        register(new SimpleNodeType(
                "on_player_join", "On Player Join", "trigger",
                List.of(),
                cfg -> ctx -> NodeResult.cont()));

        register(new SimpleNodeType(
                "send_message", "Send Message", "action",
                List.of(new FieldSpec("message", "string", "")),
                cfg -> {
                    final String message = (cfg != null && cfg.has("message") && !cfg.get("message").isJsonNull())
                            ? cfg.get("message").getAsString() : "";
                    return ctx -> {
                        ServerPlayer player = ctx.actor();
                        if (player != null) {
                            player.sendSystemMessage(Component.literal(message));
                        }
                        return NodeResult.cont();
                    };
                }));

        LOGGER.info("[Colophon] Registered {} built-in node types", TYPES.size());
    }

    /** JSON schema of all node types, served to the editor as its node palette. */
    public static String schemaJson() {
        JsonArray arr = new JsonArray();
        for (NodeType t : TYPES.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", t.id());
            o.addProperty("label", t.label());
            o.addProperty("category", t.category());
            JsonArray fields = new JsonArray();
            for (FieldSpec f : t.fields()) {
                JsonObject fo = new JsonObject();
                fo.addProperty("name", f.name());
                fo.addProperty("type", f.type());
                fo.addProperty("default", f.defaultValue());
                fields.add(fo);
            }
            o.add("fields", fields);
            arr.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("nodes", arr);
        return root.toString();
    }
}
