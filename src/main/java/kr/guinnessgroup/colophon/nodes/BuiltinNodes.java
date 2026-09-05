package kr.guinnessgroup.colophon.nodes;

import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.nodes.action.SendMessageNode;
import kr.guinnessgroup.colophon.nodes.trigger.OnPlayerJoinNode;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import org.slf4j.Logger;

/**
 * Registers Colophon's built-in node library into the {@link NodeRegistry}.
 * This is the pattern add-ons will follow: contribute node types at setup.
 */
public final class BuiltinNodes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private BuiltinNodes() {}

    public static void registerAll() {
        NodeRegistry.register(new OnPlayerJoinNode());
        NodeRegistry.register(new SendMessageNode());
        LOGGER.info("[Colophon] Registered {} built-in node types", NodeRegistry.all().size());
    }
}
