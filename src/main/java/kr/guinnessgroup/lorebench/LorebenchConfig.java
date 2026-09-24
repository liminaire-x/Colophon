/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.regex.Pattern;

/** {@code config/lorebench-common.toml}. */
public final class LorebenchConfig {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
            .comment("This server's name (a-z, 0-9, _). Servers sharing one records database",
                    "need different names. Changing it later hides this server's saved records.")
            .define("serverName", "main");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private LorebenchConfig() {}

    public static String serverName() {
        String name = SERVER_NAME.get();
        if (!NAME.matcher(name).matches()) {
            throw new IllegalStateException("lorebench-common.toml: serverName '" + name + "' must use a-z, 0-9, _");
        }
        return name;
    }
}
