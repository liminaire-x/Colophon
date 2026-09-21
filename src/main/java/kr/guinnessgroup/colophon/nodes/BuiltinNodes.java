/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes;

import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.nodes.action.BroadcastNode;
import kr.guinnessgroup.colophon.nodes.action.RunCommandNode;
import kr.guinnessgroup.colophon.nodes.action.SendMessageNode;
import kr.guinnessgroup.colophon.nodes.flow.BranchIfNode;
import kr.guinnessgroup.colophon.nodes.flow.ChanceNode;
import kr.guinnessgroup.colophon.nodes.flow.CompareNode;
import kr.guinnessgroup.colophon.nodes.flow.DelayNode;
import kr.guinnessgroup.colophon.nodes.flow.FormatTextNode;
import kr.guinnessgroup.colophon.nodes.trigger.OnPlayerDeathNode;
import kr.guinnessgroup.colophon.nodes.trigger.OnPlayerJoinNode;
import kr.guinnessgroup.colophon.nodes.economy.EconomyNodes;
import kr.guinnessgroup.colophon.nodes.state.SetVariableNode;
import kr.guinnessgroup.colophon.nodes.state.HasVariableNode;
import kr.guinnessgroup.colophon.nodes.state.GetVariableNode;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import net.neoforged.fml.ModList;
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
        NodeRegistry.register(new CompareNode());
        NodeRegistry.register(new BranchIfNode());
        NodeRegistry.register(new FormatTextNode());
        // state
        NodeRegistry.register(new SetVariableNode());
        NodeRegistry.register(new HasVariableNode());
        NodeRegistry.register(new GetVariableNode());

        // Optional adapter: economy nodes only when Impactor is installed.
        if (ModList.get().isLoaded("impactor")) {
            EconomyNodes.registerAll();
        } else {
            LOGGER.info("[Colophon] Impactor not present; economy nodes skipped");
        }

        LOGGER.info("[Colophon] Registered {} node types total", NodeRegistry.all().size());
    }
}
