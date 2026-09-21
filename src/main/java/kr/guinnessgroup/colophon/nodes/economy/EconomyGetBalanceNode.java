/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.DataPort;
import kr.guinnessgroup.colophon.runtime.ExecNode;
import kr.guinnessgroup.colophon.runtime.ExecNodeType;
import kr.guinnessgroup.colophon.runtime.InputSpec;
import kr.guinnessgroup.colophon.runtime.Nodes;
import kr.guinnessgroup.colophon.runtime.type.Types;
import net.impactdev.impactor.api.economy.EconomyService;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;

/**
 * Economy node (contract e): looks up a player's balance asynchronously and pushes
 * it as a {@code balance} number output — the demonstration of the exec async-value
 * pattern ({@link Nodes#awaitValue}). The subject is the wired {@code player} input,
 * falling back to the acting player when unwired. If there is no player or the query
 * fails, the output is left unset (defined result).
 */
public final class EconomyGetBalanceNode implements ExecNodeType {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override public String id() { return "get_balance"; }
    @Override public String label() { return "Economy: Get Balance"; }
    @Override public String category() { return "economy"; }
    @Override public List<InputSpec> inputs() {
        return List.of(InputSpec.data("player", "colophon:player", "Player"));
    }
    @Override public List<DataPort> dataOutPorts() {
        return List.of(new DataPort("balance", "number", "Balance"));
    }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public ExecNode create(JsonObject config) {
        return Nodes.awaitValue("balance", Types.NUMBER, ctx -> {
            ServerPlayer player = ctx.get("player", Types.PLAYER);
            if (player == null) {
                player = ctx.actor(); // unwired input -> acting player
            }
            if (player == null) {
                return null;
            }
            try {
                return EconomyService.instance().account(player.getUUID())
                        .thenApply(account -> account.balance().doubleValue());
            } catch (Exception e) {
                LOGGER.warn("[Colophon] get_balance failed", e);
                return null;
            }
        });
    }
}
