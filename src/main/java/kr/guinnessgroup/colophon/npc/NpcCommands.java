/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.colophon.npc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * <pre>
 * /colophon npc spawn &lt;id&gt;    place the NPC where you stand (any number of times)
 * /colophon npc remove         remove the NPC nearest to you (within 4 blocks)
 * /colophon npc remove &lt;id&gt;   remove every placement of that NPC
 * /colophon npc list           defined NPCs and how many placements each has
 * </pre>
 */
public final class NpcCommands {

    private static final double REMOVE_RANGE = 4.0;

    private NpcCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("colophon")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("npc")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(NpcCommands::suggestIds)
                                        .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(Commands.literal("remove")
                                .executes(c -> removeNearest(c.getSource()))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(NpcCommands::suggestIds)
                                        .executes(c -> removeAll(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(Commands.literal("list")
                                .executes(c -> list(c.getSource())))));
    }

    /** Ids are random (npc_7ha2m0qe), so each suggestion shows the NPC's name as its tooltip. */
    private static CompletableFuture<Suggestions> suggestIds(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        Npcs npcs = Npcs.current();
        if (npcs != null) {
            String typed = b.getRemaining().toLowerCase(Locale.ROOT);
            for (NpcDoc.NpcDef def : npcs.definitions()) {
                if (def.id().startsWith(typed)) {
                    b.suggest(def.id(), Component.literal(def.name()));
                }
            }
        }
        return b.buildFuture();
    }

    private static int spawn(CommandSourceStack src, String id) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        Npcs npcs = Npcs.current();
        NpcDoc.NpcDef def = (npcs == null) ? null : npcs.definition(id);
        if (def == null) {
            src.sendFailure(Component.literal("No NPC '" + id + "'. Register it in the editor and publish first."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        NpcEntity npc = ColophonEntities.NPC.get().create(level);
        if (npc == null) {
            return 0;
        }
        float yaw = player.getYRot();
        npc.moveTo(player.getX(), player.getY(), player.getZ(), yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setNpcId(id);
        npc.setCustomName(Component.literal(def.name()));
        // Record first: the entity checks its record on its first tick.
        npcs.place(new Placement(npc.getUUID(), id, level.dimension().location().toString(),
                npc.getX(), npc.getY(), npc.getZ()));
        level.addFreshEntity(npc);
        src.sendSuccess(() -> Component.literal("Placed " + def.name() + " (" + id + ") here."), true);
        return 1;
    }

    private static int removeNearest(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        NpcEntity nearest = player.serverLevel()
                .getEntitiesOfClass(NpcEntity.class, player.getBoundingBox().inflate(REMOVE_RANGE))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (nearest == null) {
            src.sendFailure(Component.literal("No NPC within " + (int) REMOVE_RANGE + " blocks."));
            return 0;
        }
        String id = nearest.npcId();
        nearest.discard(); // also removes its placement record
        src.sendSuccess(() -> Component.literal("Removed one placement of " + id + "."), true);
        return 1;
    }

    private static int removeAll(CommandSourceStack src, String id) {
        Npcs npcs = Npcs.current();
        if (npcs == null) {
            return 0;
        }
        int n = npcs.unplaceAll(id);
        src.sendSuccess(() -> Component.literal("Removed " + n + " placement(s) of " + id + "."), true);
        return n;
    }

    private static int list(CommandSourceStack src) {
        Npcs npcs = Npcs.current();
        if (npcs == null || npcs.definitions().isEmpty()) {
            src.sendSuccess(() -> Component.literal("No NPCs defined. Register one in the editor."), false);
            return 0;
        }
        List<Placement> placements = npcs.placements();
        for (NpcDoc.NpcDef def : npcs.definitions()) {
            long count = placements.stream().filter(p -> p.npc().equals(def.id())).count();
            src.sendSuccess(() -> Component.literal(def.id() + " (" + def.name() + "): " + count + " placed"), false);
        }
        return npcs.definitions().size();
    }
}
