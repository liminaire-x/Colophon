/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import kr.guinnessgroup.lorebench.Lorebench;
import kr.guinnessgroup.lorebench.client.ClientQuests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server → one player: every quest revealed to that player, with its content.
 * Sent whole each time (on join, on reveal, after publish); a quest that was never
 * revealed to the player never reaches their client.
 */
public record QuestSyncPayload(List<Entry> quests) implements CustomPacketPayload {

    /** A revealed quest and whether the player has completed it. */
    /** A revealed quest, whether the player has completed it, and their kill counts so far. */
    public record Entry(QuestDoc.Quest quest, boolean done, Map<String, Integer> kills) {}

    public static final Type<QuestSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Lorebench.MODID, "quests"));

    public static final StreamCodec<FriendlyByteBuf, QuestSyncPayload> CODEC =
            StreamCodec.ofMember(QuestSyncPayload::write, QuestSyncPayload::read);

    public static void register(RegisterPayloadHandlersEvent event) {
        // Handled on the client's main thread (the registrar's default).
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> ClientQuests.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(quests.size());
        for (Entry e : quests) {
            QuestDoc.Quest q = e.quest();
            buf.writeUtf(q.id());
            buf.writeUtf(q.title());
            buf.writeUtf(q.icon());
            buf.writeUtf(q.text());
            buf.writeVarInt(q.goals().size());
            for (QuestDoc.Goal g : q.goals()) {
                buf.writeEnum(g.kind());
                buf.writeUtf(g.target());
                buf.writeVarInt(g.count());
            }
            writeStacks(buf, q.rewards());
            buf.writeBoolean(e.done());
            buf.writeVarInt(e.kills().size());
            e.kills().forEach((entity, n) -> {
                buf.writeUtf(entity);
                buf.writeVarInt(n);
            });
        }
    }

    private static QuestSyncPayload read(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Entry> quests = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            // Folders are for the editor only, so they aren't sent.
            QuestDoc.Quest q = new QuestDoc.Quest(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    readGoals(buf), readStacks(buf), "");
            boolean done = buf.readBoolean();
            int k = buf.readVarInt();
            Map<String, Integer> kills = new HashMap<>();
            for (int j = 0; j < k; j++) {
                kills.put(buf.readUtf(), buf.readVarInt());
            }
            quests.add(new Entry(q, done, Map.copyOf(kills)));
        }
        return new QuestSyncPayload(List.copyOf(quests));
    }

    private static List<QuestDoc.Goal> readGoals(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<QuestDoc.Goal> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new QuestDoc.Goal(buf.readEnum(QuestDoc.Goal.Kind.class), buf.readUtf(), buf.readVarInt()));
        }
        return List.copyOf(out);
    }

    private static void writeStacks(FriendlyByteBuf buf, List<QuestDoc.Stack> stacks) {
        buf.writeVarInt(stacks.size());
        for (QuestDoc.Stack s : stacks) {
            buf.writeUtf(s.item());
            buf.writeVarInt(s.count());
        }
    }

    private static List<QuestDoc.Stack> readStacks(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<QuestDoc.Stack> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new QuestDoc.Stack(buf.readUtf(), buf.readVarInt()));
        }
        return List.copyOf(out);
    }
}
