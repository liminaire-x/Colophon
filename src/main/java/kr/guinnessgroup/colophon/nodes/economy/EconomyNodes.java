/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.nodes.economy;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import kr.guinnessgroup.colophon.runtime.NodeRegistry;
import org.slf4j.Logger;

import java.math.BigDecimal;

/**
 * Registers the Impactor economy adapter nodes. Loaded and called only when the
 * Impactor mod is present (see BuiltinNodes), so Impactor's classes are never
 * touched when it is absent.
 */
public final class EconomyNodes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EconomyNodes() {}

    public static void registerAll() {
        NodeRegistry.register(new EconomyDepositNode());
        NodeRegistry.register(new EconomyWithdrawNode());
        NodeRegistry.register(new EconomyBalanceCheckNode());
        NodeRegistry.register(new EconomyShowBalanceNode());
        LOGGER.info("[Colophon] Registered economy nodes (Impactor adapter)");
    }

    /** Reads the "amount" config field as a BigDecimal (default 0). */
    static BigDecimal amount(JsonObject config) {
        double v = 0.0;
        if (config != null && config.has("amount") && config.get("amount").isJsonPrimitive()) {
            try {
                v = config.get("amount").getAsDouble();
            } catch (NumberFormatException ignored) {
                // keep default
            }
        }
        return BigDecimal.valueOf(v);
    }
}
