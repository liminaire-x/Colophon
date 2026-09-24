/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.nodes;

import com.google.gson.JsonObject;
import kr.guinnessgroup.lorebench.record.Owner;
import kr.guinnessgroup.lorebench.runtime.Field;
import kr.guinnessgroup.lorebench.runtime.Catalog;
import kr.guinnessgroup.lorebench.runtime.Node;
import kr.guinnessgroup.lorebench.runtime.NodeResult;
import kr.guinnessgroup.lorebench.runtime.NodeType;

import java.util.List;

/** Leaves a flag on the player. Saved with the next world save. */
public final class SetFlag implements NodeType {

    private static final Field FLAG = Field.text("flag", "Flag");

    @Override public String id() { return "lorebench:set_flag"; }
    @Override public String label() { return "Set Flag"; }
    @Override public String category() { return "action"; }
    @Override public List<Field> fields() { return List.of(FLAG); }

    @Override
    public Node create(JsonObject config, Catalog catalog) {
        String key = Flags.key(Flags.requireName(FLAG.read(config)));
        return ctx -> {
            if (ctx.player() == null) {
                return NodeResult.fail("no player in this event");
            }
            ctx.records().set(Owner.player(ctx.player().getUUID()), key, Flags.VALUE);
            return NodeResult.next();
        };
    }
}
