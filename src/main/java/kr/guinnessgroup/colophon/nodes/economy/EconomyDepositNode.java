package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.impactdev.impactor.api.economy.EconomyService;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Action: deposits an amount into the acting player's primary-currency account.
 * Account retrieval is async; this node suspends until the deposit has actually
 * completed, so downstream nodes (balance checks/displays) see the new value.
 */
public final class EconomyDepositNode implements NodeType {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override public String id() { return "economy_deposit"; }
    @Override public String label() { return "Economy: Deposit"; }
    @Override public String category() { return "economy"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("amount", "number", "0")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("out"); }

    @Override
    public Node create(JsonObject config) {
        final BigDecimal amount = EconomyNodes.amount(config);
        return ctx -> {
            ServerPlayer player = ctx.actor();
            if (player == null) {
                return NodeResult.cont();
            }
            try {
                CompletableFuture<Void> future = EconomyService.instance()
                        .account(player.getUUID())
                        .thenAccept(account -> account.deposit(amount));
                // Resume (continue along "out") once the deposit has completed.
                return NodeResult.suspend(c -> future.isDone());
            } catch (Exception e) {
                LOGGER.warn("[Colophon] economy_deposit failed", e);
                return NodeResult.cont();
            }
        };
    }
}
