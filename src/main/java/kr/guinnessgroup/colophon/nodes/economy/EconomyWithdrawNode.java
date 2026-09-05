package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeType;
import kr.guinnessgroup.colophon.runtime.Nodes;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Action: withdraws an amount from the acting player's account, waiting for the
 * async transaction to complete before continuing. Logs when the withdrawal was
 * not successful (e.g. not enough funds).
 */
public final class EconomyWithdrawNode implements NodeType {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override public String id() { return "economy_withdraw"; }
    @Override public String label() { return "Economy: Withdraw"; }
    @Override public String category() { return "economy"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("amount", "number", "0")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
        final BigDecimal amount = EconomyNodes.amount(config);
        return Nodes.awaitAction(ctx -> {
            ServerPlayer player = ctx.actor();
            if (player == null) {
                return null;
            }
            try {
                return EconomyService.instance().account(player.getUUID())
                        .thenAccept(account -> {
                            EconomyTransaction tx = account.withdraw(amount);
                            if (!tx.successful()) {
                                LOGGER.info("[Colophon] economy_withdraw for {} not successful: {}",
                                        player.getUUID(), tx.result());
                            }
                        });
            } catch (Exception e) {
                LOGGER.warn("[Colophon] economy_withdraw failed", e);
                return null;
            }
        });
    }
}
