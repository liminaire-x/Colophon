/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package kr.guinnessgroup.colophon.runtime.type;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Registers Colophon's built-in data types into the {@link TypeRegistry}: the
 * four starting types of the v2 data-port contract. Add-ons register their own
 * (e.g. "impactor:account") the same way. (Contract b.)
 */
public final class BuiltinTypes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private BuiltinTypes() {}

    public static void registerAll() {
        // Value types: flow and are stored by value.
        TypeRegistry.register(new TypeDescriptor("colophon:string", "String", "#7048e8", TypeKind.VALUE, true));
        TypeRegistry.register(new TypeDescriptor("colophon:number", "Number", "#2f9e44", TypeKind.VALUE, true));
        TypeRegistry.register(new TypeDescriptor("colophon:boolean", "Boolean", "#e03131", TypeKind.VALUE, true));
        // Reference type: a handle (player UUID), resolved live at use; not stored by value.
        TypeRegistry.register(new TypeDescriptor("colophon:player", "Player", "#0d9488", TypeKind.REFERENCE, false));

        LOGGER.info("[Colophon] Registered {} data types total", TypeRegistry.all().size());
    }
}
