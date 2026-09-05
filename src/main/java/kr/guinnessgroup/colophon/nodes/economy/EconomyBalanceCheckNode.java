package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.FieldSpec;
import kr.guinnessgroup.colophon.runtime.Node;
import kr.guinnessgroup.colophon.runtime.NodeResult;
import kr.guinnessgroup.colophon.runtime.NodeType;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;

/**
 * Flow branch: "true" if the acting player's balance is at least the given
 * amount, else "false". The balance query needs a value now, so it reads the
 * account only if already loaded (getNow); if not ready it takes "false".
 * (A proper async-aware query is a v2 item — see spike retrospective.)
 */
public final class EconomyBalanceCheckNode implements NodeType {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override public String id() { return "economy_has_balance"; }
    @Override public String label() { return "Economy: Balance >= amount"; }
    @Override public String category() { return "economy"; }
    @Override public List<FieldSpec> fields() { return List.of(new FieldSpec("amount", "number", "0")); }
    @Override public boolean hasFlowIn() { return true; }
    @Override public List<String> flowOutPorts() { return List.of("true", "false"); }

    @Override
    public Node create(JsonObject config) {
        final BigDecimal amount = EconomyNodes.amount(config);
        return ctx -> {
            ServerPlayer player = ctx.actor();
            if (player == null) {
                return NodeResult.branch("false");
            }
            try {
                Account account = EconomyService.instance().account(player.getUUID()).getNow(null);
                if (account == null) {
                    LOGGER.info("[Colophon] economy_has_balance: account not loaded for {}, taking false",
                            player.getUUID());
                    return NodeResult.branch("false");
                }
                return NodeResult.branch(account.balance().compareTo(amount) >= 0 ? "true" : "false");
            } catch (Exception e) {
                LOGGER.warn("[Colophon] economy_has_balance error", e);
                return NodeResult.branch("false");
            }
        };
    }
}
