/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

import kr.guinnessgroup.colophon.record.Owner;
import kr.guinnessgroup.colophon.record.RecordStore;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Quests as the game sees them: definitions (from the published quest document)
 * and each player's state (their records). Nodes reach this through
 * {@link #current()} while the server runs. Call on the server thread.
 */
public final class Quests {

    private static volatile Quests current;

    private final ColophonRuntime runtime;
    private final RecordStore records;

    public Quests(ColophonRuntime runtime, RecordStore records) {
        this.runtime = runtime;
        this.records = records;
    }

    /** The running server's quests, or {@code null} when no server is running. */
    public static Quests current() {
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

    public QuestState state(ServerPlayer player, String questId) {
        QuestState stored = QuestState.fromRecord(records.get(Owner.player(player.getUUID()), questId));
        QuestDoc.Quest quest = runtime.quest(questId);
        if (stored == QuestState.ACTIVE && quest != null && hasGoals(player.getInventory(), quest)) {
            return QuestState.READY;
        }
        return stored;
    }

    /** Show a hidden quest to the player (it becomes active). Does nothing if already revealed. */
    public void reveal(ServerPlayer player, String questId) {
        Owner owner = Owner.player(player.getUUID());
        if (QuestState.fromRecord(records.get(owner, questId)) != QuestState.HIDDEN) {
            return;
        }
        records.set(owner, questId, QuestState.ACTIVE_VALUE);
        sync(player);
    }

    /** Send the player every quest revealed to them, and nothing else. */
    public void sync(ServerPlayer player) {
        Owner owner = Owner.player(player.getUUID());
        List<QuestSyncPayload.Entry> revealed = new ArrayList<>();
        for (QuestDoc.Quest q : runtime.quests()) {
            QuestState s = QuestState.fromRecord(records.get(owner, q.id()));
            if (s != QuestState.HIDDEN) {
                revealed.add(new QuestSyncPayload.Entry(q, s == QuestState.DONE));
            }
        }
        PacketDistributor.sendToPlayer(player, new QuestSyncPayload(List.copyOf(revealed)));
    }

    /** After a publish: quest content may have changed for everyone online. */
    public void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    /** Whether the inventory holds every goal (both sides use this, so the screen agrees with the server). */
    public static boolean hasGoals(Inventory inventory, QuestDoc.Quest quest) {
        for (QuestDoc.Stack goal : quest.goals()) {
            if (inventory.countItem(item(goal.item())) < goal.count()) {
                return false;
            }
        }
        return true;
    }

    /** The item with this id; air if there is none (publish rejects unknown items). */
    public static Item item(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
    }

    /** For publish: whether an item id names a real item. */
    public static boolean itemExists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl);
    }
}
