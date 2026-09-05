package kr.guinnessgroup.colophon.nodes;

import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.nodes.action.BroadcastNode;
import kr.guinnessgroup.colophon.nodes.action.RunCommandNode;
import kr.guinnessgroup.colophon.nodes.action.SendMessageNode;
import kr.guinnessgroup.colophon.nodes.flow.ChanceNode;
import kr.guinnessgroup.colophon.nodes.flow.DelayNode;
import kr.guinnessgroup.colophon.nodes.trigger.OnPlayerDeathNode;
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
        // triggers
        NodeRegistry.register(new OnPlayerJoinNode());
        NodeRegistry.register(new OnPlayerDeathNode());
        // actions
        NodeRegistry.register(new SendMessageNode());
        NodeRegistry.register(new BroadcastNode());
        NodeRegistry.register(new RunCommandNode());
        // flow
        NodeRegistry.register(new DelayNode());
        NodeRegistry.register(new ChanceNode());

        LOGGER.info("[Colophon] Registered {} built-in node types", NodeRegistry.all().size());
    }
}
