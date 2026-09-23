/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.guinnessgroup.colophon.nodes.OnNpcInteract;
import kr.guinnessgroup.colophon.record.Owner;
import kr.guinnessgroup.colophon.record.RecordStore;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPCs as the game sees them: definitions (from the published NPC document) and
 * placements (server records). NPC entities and commands reach this through
 * {@link #current()} while the server runs.
 */
public final class Npcs {

    private static volatile Npcs current;

    private final ColophonRuntime runtime;
    private final RecordStore records;

    public Npcs(ColophonRuntime runtime, RecordStore records) {
        this.runtime = runtime;
        this.records = records;
    }

    /** The running server's NPCs, or {@code null} when no server is running. */
    public static Npcs current() {
        return current;
    }

    public void start() {
        current = this;
    }

    public void stop() {
        if (current == this) {
            current = null;
        }
    }

    private Owner owner() {
        return runtime.serverOwner();
    }

    public NpcDoc.NpcDef definition(String id) {
        return runtime.npc(id);
    }

    public List<NpcDoc.NpcDef> definitions() {
        return runtime.npcs();
    }

    /** The placement of this entity, or {@code null} if it has none. */
    public Placement placement(UUID entity) {
        String key = Placement.KEY_PREFIX + entity;
        return Placement.fromRecord(key, records.get(owner(), key));
    }

    public void place(Placement p) {
        records.set(owner(), p.key(), p.toValue());
    }

    public void unplace(UUID entity) {
        records.set(owner(), Placement.KEY_PREFIX + entity, null);
    }

    public List<Placement> placements() {
        List<Placement> out = new ArrayList<>();
        for (Map.Entry<String, String> e : records.all(owner()).entrySet()) {
            Placement p = Placement.fromRecord(e.getKey(), e.getValue());
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Remove every placement of an NPC. Loaded entities notice and disappear. */
    public int unplaceAll(String npcId) {
        int n = 0;
        for (Placement p : placements()) {
            if (p.npc().equals(npcId)) {
                unplace(p.entity());
                n++;
            }
        }
        return n;
    }

    /** A player right-clicked one of this NPC's placements. */
    public void interact(NpcEntity npc, ServerPlayer player) {
        runtime.fire(OnNpcInteract.ID, player.getServer(), player, Map.of(
                OnNpcInteract.EVENT_NPC, npc.npcId(),
                OnNpcInteract.EVENT_NPC_ENTITY, npc.getUUID().toString()));
    }

    /**
     * Play an animation once on an NPC. If {@code onlyEntity} is one of its loaded
     * placements, only that one plays (e.g. the NPC that was clicked); otherwise every
     * loaded placement of the NPC plays.
     *
     * @return how many placements played it
     */
    public int playAnimation(MinecraftServer server, String npcId, UUID onlyEntity, String animation) {
        if (onlyEntity != null) {
            NpcEntity npc = find(server, onlyEntity);
            if (npc != null && npc.npcId().equals(npcId)) {
                npc.playAnimation(animation);
                return 1;
            }
        }
        int n = 0;
        for (Placement p : placements()) {
            if (p.npc().equals(npcId)) {
                NpcEntity npc = find(server, p.entity());
                if (npc != null) {
                    npc.playAnimation(animation);
                    n++;
                }
            }
        }
        return n;
    }

    private static NpcEntity find(MinecraftServer server, UUID entity) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(entity) instanceof NpcEntity npc) {
                return npc;
            }
        }
        return null;
    }

    /** For the editor: {@code {"npc_7ha2m0qe": [{"dim": ..., "x": ..., "y": ..., "z": ...}]}}. */
    public String placementsJson() {
        JsonObject root = new JsonObject();
        for (Placement p : placements()) {
            JsonArray list = root.has(p.npc()) ? root.getAsJsonArray(p.npc()) : new JsonArray();
            JsonObject o = new JsonObject();
            o.addProperty("dim", p.dimension());
            o.addProperty("x", Math.round(p.x()));
            o.addProperty("y", Math.round(p.y()));
            o.addProperty("z", Math.round(p.z()));
            list.add(o);
            root.add(p.npc(), list);
        }
        return root.toString();
    }
}
