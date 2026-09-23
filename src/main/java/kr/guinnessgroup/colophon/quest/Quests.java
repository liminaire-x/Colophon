/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.quest;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import kr.guinnessgroup.colophon.record.Owner;
import kr.guinnessgroup.colophon.record.RecordStore;
import kr.guinnessgroup.colophon.runtime.ColophonRuntime;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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

    /**
     * Hand in a ready quest: take the goal items, give the rewards, and record it
     * done, all at once on the server thread. Rewards that do not fit drop at the
     * player's feet (like {@code /give}).
     *
     * @return false (and nothing changes) if the quest is not ready for this player
     */
    public boolean complete(ServerPlayer player, String questId) {
        QuestDoc.Quest quest = runtime.quest(questId);
        if (quest == null || state(player, questId) != QuestState.READY) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (QuestDoc.Stack goal : quest.goals()) {
            take(inventory, item(goal.item()), goal.count());
        }
        for (QuestDoc.Stack reward : quest.rewards()) {
            give(player, stack(reward.item(), player.registryAccess()), reward.count());
        }
        records.set(Owner.player(player.getUUID()), questId, QuestState.DONE_VALUE);
        sync(player);
        return true;
    }

    /** Remove {@code count} of an item from the same slots {@link Inventory#countItem} counts. */
    private static void take(Inventory inventory, Item item, int count) {
        int left = count;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        inventory.setChanged();
    }

    /** Give copies of {@code item} in stacks no larger than it allows; what does not fit drops. */
    private static void give(ServerPlayer player, ItemStack item, int count) {
        if (item.isEmpty()) {
            return;
        }
        int max = item.getMaxStackSize();
        for (int left = count; left > 0; ) {
            int n = Math.min(left, max);
            ItemHandlerHelper.giveItemToPlayer(player, item.copyWithCount(n));
            left -= n;
        }
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

    /**
     * One item as {@code /give} writes it ({@code minecraft:iron_sword[...]}), read by
     * the game's own parser, so components from other mods work too. Empty if it
     * cannot be read (publish rejects those).
     *
     * @param registries the running game's registries (enchantments live there)
     */
    public static ItemStack stack(String spec, HolderLookup.Provider registries) {
        try {
            ItemParser.ItemResult r = parse(spec, registries);
            return new ItemStack(r.item(), 1, r.components());
        } catch (CommandSyntaxException e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * For publish: why an item cannot be used, or {@code null} if it can. Reads it the
     * way {@link #stack} will, with the running server's registries.
     */
    public static String itemProblem(String spec) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return "the server is not running";
        }
        try {
            parse(spec, server.registryAccess());
            return null;
        } catch (CommandSyntaxException e) {
            return e.getMessage();
        }
    }

    private static ItemParser.ItemResult parse(String spec, HolderLookup.Provider registries) throws CommandSyntaxException {
        StringReader reader = new StringReader(spec);
        ItemParser.ItemResult r = new ItemParser(registries).parse(reader);
        if (reader.canRead()) {
            throw new SimpleCommandExceptionType(Component.literal(
                    "unexpected text after the item: '" + reader.getRemaining() + "'")).createWithContext(reader);
        }
        return r;
    }
}
