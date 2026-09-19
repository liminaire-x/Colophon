/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.impactdev.impactor.api.economy.EconomyService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Action: sends the acting player their current balance as a chat message.
 * A stopgap until data ports let a value flow into a Send Message node.
 */
public final class EconomyShowBalanceNode implements NodeType {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override public String id() { return "economy_show_balance"; }
    @Override public String label() { return "Economy: Show Balance"; }
    @Override public String category() { return "economy"; }
    @Override public List<FieldSpec> fields() { return List.of(); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        return ctx -> {
            ServerPlayer player = ctx.actor();
            MinecraftServer server = ctx.server();
            if (player != null && server != null) {
                try {
                    EconomyService.instance().account(player.getUUID()).thenAccept(account -> {
                        BigDecimal balance = account.balance();
                        // Send on the main thread (async completion may be off-thread).
                        server.execute(() -> player.sendSystemMessage(
                                Component.literal("Balance: " + balance.toPlainString())));
                    });
                } catch (Exception e) {
                    LOGGER.warn("[Colophon] economy_show_balance failed", e);
                }
            }
            return NodeResult.cont();
        };
    }
}
