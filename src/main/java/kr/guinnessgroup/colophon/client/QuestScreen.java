/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.client;

import kr.guinnessgroup.colophon.quest.QuestDoc;
import kr.guinnessgroup.colophon.quest.QuestSyncPayload;
import kr.guinnessgroup.colophon.quest.Quests;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The quest screen: revealed quests on the left, the chosen one's story, needs
 * (with progress from this player's inventory) and rewards on the right.
 */
public final class QuestScreen extends Screen {

    private static final int LIST_W = 120;
    private static final int ROW_H = 20;
    private static final int PAD = 6;

    private static final int PANEL = 0xE0101010;
    private static final int BORDER = 0xFF505050;
    private static final int SELECTED = 0x40FFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF909090;
    private static final int LIGHT = 0xFFD0D0D0;
    private static final int GOLD = 0xFFFFD84A;
    private static final int GREEN = 0xFF55FF55;

    private String selectedId;
    private int left;
    private int top;
    private int panelW;
    private int panelH;

    public QuestScreen() {
        super(Component.translatable("colophon.quests.title"));
    }

    @Override
    protected void init() {
        panelW = Math.min(380, width - 20);
        panelH = Math.min(230, height - 20);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ColophonClient.OPEN_QUESTS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<QuestSyncPayload.Entry> quests = ClientQuests.all();
        int x0 = left + PAD;
        int y0 = listTop();
        if (mouseX >= x0 && mouseX < x0 + LIST_W && mouseY >= y0) {
            int row = (int) ((mouseY - y0) / ROW_H);
            if (row < quests.size()) {
                selectedId = quests.get(row).quest().id();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int listTop() {
        return top + PAD + font.lineHeight + 8;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, BORDER);
        g.fill(left, top, left + panelW, top + panelH, PANEL);
        g.drawCenteredString(font, title, left + panelW / 2, top + PAD, WHITE);

        List<QuestSyncPayload.Entry> quests = ClientQuests.all();
        if (quests.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("colophon.quests.none"),
                    left + panelW / 2, top + panelH / 2 - font.lineHeight / 2, GRAY);
            return;
        }
        QuestSyncPayload.Entry selected = quests.get(0);
        for (QuestSyncPayload.Entry e : quests) {
            if (e.quest().id().equals(selectedId)) {
                selected = e;
            }
        }

        LocalPlayer player = minecraft.player;
        ItemStack hovered = ItemStack.EMPTY;

        // Left: the list.
        int x0 = left + PAD;
        int y = listTop();
        g.enableScissor(x0, y, x0 + LIST_W, top + panelH - PAD);
        for (QuestSyncPayload.Entry e : quests) {
            if (e == selected) {
                g.fill(x0, y, x0 + LIST_W, y + ROW_H, SELECTED);
            }
            g.renderItem(new ItemStack(Quests.item(icon(e.quest()))), x0 + 2, y + 2);
            String name = font.plainSubstrByWidth(e.quest().title(), LIST_W - 24);
            g.drawString(font, name, x0 + 22, y + (ROW_H - font.lineHeight) / 2 + 1, e.done() ? GRAY : WHITE);
            y += ROW_H;
        }
        g.disableScissor();
        int divider = x0 + LIST_W + PAD;
        g.fill(divider, listTop(), divider + 1, top + panelH - PAD, BORDER);

        // Right: the chosen quest.
        QuestDoc.Quest q = selected.quest();
        int dx = divider + PAD + 1;
        int dw = left + panelW - PAD - dx;
        int bottom = top + panelH - PAD;
        boolean ready = !selected.done() && player != null && Quests.hasGoals(player.getInventory(), q);
        y = listTop();
        g.enableScissor(dx, y, dx + dw, bottom);
        g.drawString(font, q.title(), dx, y, GOLD);
        y += font.lineHeight + 2;
        if (selected.done()) {
            g.drawString(font, Component.translatable("colophon.quests.done"), dx, y, GREEN);
            y += font.lineHeight + 2;
        } else if (ready) {
            g.drawString(font, Component.translatable("colophon.quests.ready"), dx, y, GREEN);
            y += font.lineHeight + 2;
        }
        if (!q.text().isEmpty()) {
            y += 2;
            for (FormattedCharSequence line : font.split(Component.literal(q.text()), dw)) {
                g.drawString(font, line, dx, y, LIGHT);
                y += font.lineHeight;
            }
        }
        if (!q.goals().isEmpty()) {
            y += 6;
            g.drawString(font, Component.translatable("colophon.quests.needs"), dx, y, GRAY);
            y += font.lineHeight + 2;
            for (QuestDoc.Stack goal : q.goals()) {
                ItemStack stack = new ItemStack(Quests.item(goal.item()));
                String amount;
                int color;
                if (selected.done() || player == null) {
                    amount = " × " + goal.count();
                    color = WHITE;
                } else {
                    int have = player.getInventory().countItem(stack.getItem());
                    amount = " " + Math.min(have, goal.count()) + "/" + goal.count();
                    color = have >= goal.count() ? GREEN : WHITE;
                }
                hovered = itemRow(g, stack, amount, color, dx, y, mouseX, mouseY, hovered);
                y += 18;
            }
        }
        if (!q.rewards().isEmpty()) {
            y += 4;
            g.drawString(font, Component.translatable("colophon.quests.rewards"), dx, y, GRAY);
            y += font.lineHeight + 2;
            for (QuestDoc.Stack reward : q.rewards()) {
                ItemStack stack = new ItemStack(Quests.item(reward.item()));
                hovered = itemRow(g, stack, " × " + reward.count(), WHITE, dx, y, mouseX, mouseY, hovered);
                y += 18;
            }
        }
        g.disableScissor();
        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    /** An item icon, its name and an amount. Returns the stack if the mouse is over the icon. */
    private ItemStack itemRow(GuiGraphics g, ItemStack stack, String amount, int color,
                              int x, int y, int mouseX, int mouseY, ItemStack hovered) {
        g.renderItem(stack, x, y);
        Component line = stack.getHoverName().copy().append(amount);
        g.drawString(font, line, x + 20, y + 5, color);
        boolean over = mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
        return over ? stack : hovered;
    }

    /** The list icon: the quest's icon, else its first goal's item, else a book. */
    private static String icon(QuestDoc.Quest q) {
        if (!q.icon().isEmpty()) {
            return q.icon();
        }
        return q.goals().isEmpty() ? "minecraft:book" : q.goals().get(0).item();
    }
}
