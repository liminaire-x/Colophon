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
import kr.guinnessgroup.colophon.runtime.ContentChecks;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Quests as the game sees them: definitions (from the published quest document)
 * and each player's state (their records). Nodes reach this through
 * {@link #current()} while the server runs. Call on the server thread.
 * <p>
 * Items are read with the game's own command parsers: rewards as {@code /give}
 * writes them, hand-in goals as {@code /clear} reads them (listed components must
 * match, others are ignored). See docs/decisions/0006-item-syntax.md.
 */
public final class Quests {

    private static volatile Quests current;

    private final ColophonRuntime runtime;
    private final RecordStore records;

    /** Goal conditions already read, by their text. Registries change only between server runs. */
    private final Map<String, Predicate<ItemStack>> conditions = new HashMap<>();

    public Quests(ColophonRuntime runtime, RecordStore records) {
        this.runtime = runtime;
        this.records = records;
    }

    /** The running server's quests, or {@code null} when no server is running. */
    public static Quests current() {
        return current;
    }

    public void start() {
        conditions.clear();
        current = this;
    }

    public void stop() {
        if (current == this) {
            current = null;
        }
    }

    public QuestState state(ServerPlayer player, String questId) {
        Owner owner = Owner.player(player.getUUID());
        QuestState stored = QuestState.fromRecord(records.get(owner, questId));
        QuestDoc.Quest quest = runtime.quest(questId);
        if (stored == QuestState.ACTIVE && quest != null
                && goalsMet(player.getInventory(), kills(owner, questId), quest, s -> condition(player, s))) {
            return QuestState.READY;
        }
        return stored;
    }

    private Map<String, Integer> kills(Owner owner, String questId) {
        return QuestProgress.read(records.get(owner, QuestProgress.key(questId)));
    }

    private Predicate<ItemStack> condition(ServerPlayer player, String spec) {
        return conditions.computeIfAbsent(spec, s -> conditionOrNothing(s, player));
    }

    /**
     * A player killed something: count it toward every active quest of theirs with a
     * kill goal for that entity, up to the goal's count. Tamed animals (someone's pet
     * wolf, cat, parrot, horse ...) never count.
     */
    public void onKill(ServerPlayer player, LivingEntity victim) {
        if ((victim instanceof TamableAnimal pet && pet.isTame())
                || (victim instanceof AbstractHorse horse && horse.isTamed())) {
            return;
        }
        String entity = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        Owner owner = Owner.player(player.getUUID());
        boolean changed = false;
        for (QuestDoc.Quest q : runtime.quests()) {
            if (QuestState.fromRecord(records.get(owner, q.id())) != QuestState.ACTIVE) {
                continue;
            }
            for (QuestDoc.Goal goal : q.goals()) {
                if (goal.kind() != QuestDoc.Goal.Kind.KILL || !goal.target().equals(entity)) {
                    continue;
                }
                Map<String, Integer> kills = kills(owner, q.id());
                int have = kills.getOrDefault(entity, 0);
                if (have < goal.count()) {
                    kills.put(entity, have + 1);
                    records.set(owner, QuestProgress.key(q.id()), QuestProgress.write(kills));
                    changed = true;
                }
            }
        }
        if (changed) {
            sync(player);
        }
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
     * done (dropping its kill progress), all at once on the server thread. Rewards
     * that do not fit drop at the player's feet (like {@code /give}).
     *
     * @return false (and nothing changes) if the quest is not ready for this player
     */
    public boolean complete(ServerPlayer player, String questId) {
        QuestDoc.Quest quest = runtime.quest(questId);
        if (quest == null || state(player, questId) != QuestState.READY) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (QuestDoc.Goal goal : quest.goals()) {
            if (goal.kind() == QuestDoc.Goal.Kind.ITEM) {
                take(inventory, condition(player, goal.target()), goal.count());
            }
        }
        for (QuestDoc.Stack reward : quest.rewards()) {
            give(player, stack(reward.item(), player.registryAccess()), reward.count());
        }
        Owner owner = Owner.player(player.getUUID());
        records.set(owner, questId, QuestState.DONE_VALUE);
        records.set(owner, QuestProgress.key(questId), null);
        sync(player);
        return true;
    }

    /** Remove {@code count} matching items from the same slots {@link #count} counts. */
    private static void take(Inventory inventory, Predicate<ItemStack> matches, int count) {
        int left = count;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && matches.test(stack)) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        inventory.setChanged();
    }

    /** How many items in the inventory (main, armor, offhand) match. */
    public static int count(Inventory inventory, Predicate<ItemStack> matches) {
        int n = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && matches.test(stack)) {
                n += stack.getCount();
            }
        }
        return n;
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
                revealed.add(new QuestSyncPayload.Entry(q, s == QuestState.DONE, kills(owner, q.id())));
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

    /**
     * Whether every goal is met: items in the inventory, kills in {@code kills}.
     * Both sides use this, so the screen agrees with the server.
     *
     * @param condition a hand-in goal's item condition, from its text
     */
    public static boolean goalsMet(Inventory inventory, Map<String, Integer> kills, QuestDoc.Quest quest,
                                   Function<String, Predicate<ItemStack>> condition) {
        for (QuestDoc.Goal goal : quest.goals()) {
            int have = (goal.kind() == QuestDoc.Goal.Kind.ITEM)
                    ? count(inventory, condition.apply(goal.target()))
                    : kills.getOrDefault(goal.target(), 0);
            if (have < goal.count()) {
                return false;
            }
        }
        return true;
    }

    // --- items and entities, read the way the game's commands read them ---

    /** The item with this plain id; air if there is none (publish rejects unknown items). */
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
            ItemParser.ItemResult r = parseItem(spec, registries);
            return new ItemStack(r.item(), 1, r.components());
        } catch (CommandSyntaxException e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * An item condition as {@code /clear} reads it: {@code minecraft:wheat},
     * {@code minecraft:iron_sword[custom_data={...}]}, {@code #minecraft:logs} ...
     * Listed components must match; unlisted ones are ignored.
     */
    public static Predicate<ItemStack> condition(String spec, HolderLookup.Provider registries, FeatureFlagSet features)
            throws CommandSyntaxException {
        StringReader reader = new StringReader(spec);
        Predicate<ItemStack> p = new ItemPredicateArgument(CommandBuildContext.simple(registries, features)).parse(reader);
        requireEnd(reader);
        return p;
    }

    /** A condition for a player's side of the game; matches nothing if it cannot be read. */
    public static Predicate<ItemStack> conditionOrNothing(String spec, Player player) {
        try {
            return condition(spec, player.registryAccess(), player.level().enabledFeatures());
        } catch (CommandSyntaxException e) {
            return stack -> false;
        }
    }

    /**
     * What to show for a hand-in goal: the item it names, with any components that
     * read as {@code /give} would; for a tag ({@code #minecraft:logs}), its first item.
     */
    public static ItemStack display(String spec, HolderLookup.Provider registries) {
        if (spec.startsWith("#")) {
            ResourceLocation rl = ResourceLocation.tryParse(spec.substring(1));
            return rl == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl))
                    .filter(tag -> tag.size() > 0)
                    .map(tag -> new ItemStack(tag.get(0)))
                    .orElse(ItemStack.EMPTY);
        }
        ItemStack exact = stack(spec, registries);
        if (!exact.isEmpty()) {
            return exact;
        }
        int bracket = spec.indexOf('[');
        return new ItemStack(item(bracket < 0 ? spec : spec.substring(0, bracket)));
    }

    /**
     * What a player holds in their main hand, as {@code /give} writes it, with every
     * component (name, enchantments, damage, other mods' data). Empty if nothing.
     */
    public static String held(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return "";
        }
        return new ItemInput(stack.getItemHolder(), stack.getComponentsPatch()).serialize(player.registryAccess());
    }

    /** The entity type with this id, or {@code null} if there is none. */
    public static EntityType<?> entityType(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }

    /** For publish: reads content with the running server's registries. */
    public static final ContentChecks CHECKS = new ContentChecks() {
        @Override
        public String item(String spec) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return "the server is not running";
            }
            try {
                parseItem(spec, server.registryAccess());
                return null;
            } catch (CommandSyntaxException e) {
                return e.getMessage();
            }
        }

        @Override
        public String itemCondition(String spec) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return "the server is not running";
            }
            try {
                condition(spec, server.registryAccess(), server.getWorldData().enabledFeatures());
                return null;
            } catch (CommandSyntaxException e) {
                return e.getMessage();
            }
        }

        @Override
        public String entity(String id) {
            return entityType(id) == null ? "no entity '" + id + "' in this game" : null;
        }
    };

    private static ItemParser.ItemResult parseItem(String spec, HolderLookup.Provider registries) throws CommandSyntaxException {
        StringReader reader = new StringReader(spec);
        ItemParser.ItemResult r = new ItemParser(registries).parse(reader);
        requireEnd(reader);
        return r;
    }

    private static void requireEnd(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead()) {
            throw new SimpleCommandExceptionType(Component.literal(
                    "unexpected text after the item: '" + reader.getRemaining() + "'")).createWithContext(reader);
        }
    }
}
