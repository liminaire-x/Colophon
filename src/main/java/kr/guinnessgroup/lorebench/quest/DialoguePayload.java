/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import kr.guinnessgroup.lorebench.Lorebench;
import kr.guinnessgroup.lorebench.client.ClientDialogue;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server → one player: what an NPC has to talk about with them (0009). Only this
 * player's quests for this NPC are in it, so an offer reaches the client only when
 * the NPC can make it.
 *
 * @param resume sent after the player accepted or handed something in: carry on with
 *               the list instead of starting over
 */
public record DialoguePayload(String npcName, List<String> greeting, List<Entry> entries, boolean resume)
        implements CustomPacketPayload {

    /**
     * @param lines what the NPC says about it (offer, in progress or hand-in lines)
     * @param kills the player's kill counts, for progress
     */
    public record Entry(Dialogue.Kind kind, QuestDoc.Quest quest, List<String> lines, Map<String, Integer> kills) {}

    public static final Type<DialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Lorebench.MODID, "dialogue"));

    public static final StreamCodec<FriendlyByteBuf, DialoguePayload> CODEC =
            StreamCodec.ofMember(DialoguePayload::write, DialoguePayload::read);

    /** Registers both dialogue messages. Handled on the main thread (the registrar's default). */
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(TYPE, CODEC, (payload, context) -> ClientDialogue.accept(payload))
                .playToServer(DialogueChoicePayload.TYPE, DialogueChoicePayload.CODEC, (choice, context) -> {
                    Dialogues dialogues = Dialogues.current();
                    if (dialogues != null && context.player() instanceof ServerPlayer player) {
                        dialogues.choose(player, choice);
                    }
                });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(npcName);
        writeLines(buf, greeting);
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeEnum(e.kind());
            QuestSyncPayload.writeQuest(buf, e.quest());
            writeLines(buf, e.lines());
            QuestSyncPayload.writeKills(buf, e.kills());
        }
        buf.writeBoolean(resume);
    }

    private static DialoguePayload read(FriendlyByteBuf buf) {
        String npcName = buf.readUtf();
        List<String> greeting = readLines(buf);
        int n = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Dialogue.Kind kind = buf.readEnum(Dialogue.Kind.class);
            QuestDoc.Quest quest = QuestSyncPayload.readQuest(buf);
            List<String> lines = readLines(buf);
            entries.add(new Entry(kind, quest, lines, QuestSyncPayload.readKills(buf)));
        }
        return new DialoguePayload(npcName, greeting, List.copyOf(entries), buf.readBoolean());
    }

    private static void writeLines(FriendlyByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        lines.forEach(buf::writeUtf);
    }

    private static List<String> readLines(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lines.add(buf.readUtf());
        }
        return List.copyOf(lines);
    }
}
