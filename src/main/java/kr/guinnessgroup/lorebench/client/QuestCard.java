/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.client;

import kr.guinnessgroup.lorebench.quest.QuestDoc;
import kr.guinnessgroup.lorebench.quest.Quests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A quest's needs and rewards as the quest screen and the dialogue show them, with
 * the items read once per screen. Progress comes from this player's inventory and
 * the kill counts the server sent.
 */
final class QuestCard {

    static final int ROW_H = 18;
    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFF909090;
    static final int GREEN = 0xFF55FF55;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Map<String, ItemStack> rewards = new HashMap<>();
    private final Map<String, ItemStack> displays = new HashMap<>();
    private final Map<String, Predicate<ItemStack>> conditions = new HashMap<>();

    /** How tall {@link #needsAndRewards} draws a quest. */
    int height(Font font, QuestDoc.Quest q) {
        int h = 0;
        if (!q.goals().isEmpty()) {
            h += 6 + font.lineHeight + 2 + q.goals().size() * ROW_H;
        }
        if (!q.rewards().isEmpty()) {
            h += 4 + font.lineHeight + 2 + q.rewards().size() * ROW_H;
        }
        return h;
    }

    /**
     * Needs and rewards from {@code y} down.
     *
     * @param progress show "3/10" from this player's inventory and kills; off shows "× 10"
     * @return the item under the mouse, for a tooltip, or empty
     */
    ItemStack needsAndRewards(GuiGraphics g, Font font, QuestDoc.Quest q, Map<String, Integer> kills, boolean progress,
                              int x, int y, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        LocalPlayer player = minecraft.player;
        if (!q.goals().isEmpty()) {
            y += 6;
            g.drawString(font, Component.translatable("lorebench.quests.needs"), x, y, GRAY);
            y += font.lineHeight + 2;
            for (QuestDoc.Goal goal : q.goals()) {
                boolean kill = goal.kind() == QuestDoc.Goal.Kind.KILL;
                ItemStack icon = goalIcon(goal);
                Component name = kill ? entityName(goal.target())
                        : goal.target().startsWith("#") ? Component.literal(goal.target()) // a tag: any item in it
                        : icon.getHoverName();
                String amount;
                int color;
                if (!progress || player == null) {
                    amount = " × " + goal.count();
                    color = WHITE;
                } else {
                    int have = kill
                            ? kills.getOrDefault(goal.target(), 0)
                            : Quests.count(player.getInventory(), condition(goal.target()));
                    amount = " " + Math.min(have, goal.count()) + "/" + goal.count();
                    color = have >= goal.count() ? GREEN : WHITE;
                }
                // A kill goal's icon is a spawn egg: its tooltip would say "Spawn Egg", so none.
                hovered = row(g, font, icon, name, amount, color, !kill, x, y, mouseX, mouseY, hovered);
                y += ROW_H;
            }
        }
        if (!q.rewards().isEmpty()) {
            y += 4;
            g.drawString(font, Component.translatable("lorebench.quests.rewards"), x, y, GRAY);
            y += font.lineHeight + 2;
            for (QuestDoc.Stack reward : q.rewards()) {
                ItemStack stack = rewardStack(reward.item());
                hovered = row(g, font, stack, stack.getHoverName(), " × " + reward.count(), WHITE, true, x, y, mouseX, mouseY, hovered);
                y += ROW_H;
            }
        }
        return hovered;
    }

    /**
     * An icon, a name and an amount. Returns the icon's stack if the mouse is over it
     * and {@code tooltip} is on (so the caller shows its tooltip), else {@code hovered}.
     */
    private static ItemStack row(GuiGraphics g, Font font, ItemStack icon, Component name, String amount, int color,
                                 boolean tooltip, int x, int y, int mouseX, int mouseY, ItemStack hovered) {
        g.renderItem(icon, x, y);
        g.drawString(font, name.copy().append(amount), x + 20, y + 5, color);
        boolean over = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        return (over && tooltip) ? icon : hovered;
    }

    /**
     * An item goal shows the item its condition names (with a name or enchantments if
     * it lists them); a kill goal shows the mob's spawn egg, if it has one.
     */
    private ItemStack goalIcon(QuestDoc.Goal goal) {
        if (goal.kind() == QuestDoc.Goal.Kind.ITEM) {
            return displays.computeIfAbsent(goal.target(), s -> minecraft.player == null
                    ? ItemStack.EMPTY
                    : Quests.display(s, minecraft.player.registryAccess()));
        }
        SpawnEggItem egg = SpawnEggItem.byId(Quests.entityType(goal.target()));
        return egg == null ? ItemStack.EMPTY : new ItemStack(egg);
    }

    /** A hand-in goal's condition, read once per screen. */
    Predicate<ItemStack> condition(String spec) {
        return conditions.computeIfAbsent(spec, s -> minecraft.player == null
                ? stack -> false
                : Quests.conditionOrNothing(s, minecraft.player));
    }

    private static Component entityName(String id) {
        EntityType<?> type = Quests.entityType(id);
        return type == null ? Component.literal(id) : type.getDescription();
    }

    /** A reward with its components (name, enchantments, ...), read once per screen. */
    private ItemStack rewardStack(String spec) {
        return rewards.computeIfAbsent(spec, s -> minecraft.player == null
                ? ItemStack.EMPTY
                : Quests.stack(s, minecraft.player.registryAccess()));
    }

    /** The list icon: the quest's icon, else its first goal's icon, else a book. */
    ItemStack icon(QuestDoc.Quest q) {
        if (!q.icon().isEmpty()) {
            return new ItemStack(Quests.item(q.icon()));
        }
        ItemStack first = q.goals().isEmpty() ? ItemStack.EMPTY : goalIcon(q.goals().get(0));
        return first.isEmpty() ? new ItemStack(Items.BOOK) : first;
    }
}
