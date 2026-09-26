/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kr.guinnessgroup.lorebench.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The crops a harvest goal can name: only those whose block shows when it is fully
 * grown. Pumpkins and melons (the block is the fruit), sugar cane, cactus and bamboo
 * (they only grow taller) and sweet berries (picked, not broken) are left for later.
 * See docs/decisions/0010-farming-goals.md.
 */
public final class Crops {

    private Crops() {}

    /** The block with this id, or {@code null} if there is none. */
    public static Block block(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }

    /**
     * Whether a harvest goal may name this block: the game's crop kind (wheat, carrots,
     * potatoes, beetroots, and mod crops built the same way) as long as its last stage
     * is still that crop (a torchflower crop turns into a flower instead), nether wart
     * and cocoa.
     */
    public static boolean harvestable(Block block) {
        if (block instanceof CropBlock crop) {
            return crop.getStateForAge(crop.getMaxAge()).is(crop);
        }
        return block instanceof NetherWartBlock || block instanceof CocoaBlock;
    }

    /** Whether this crop is fully grown. */
    public static boolean ripe(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        return false;
    }

    /** Every crop in this game a harvest goal may name, for the editor's list. */
    public static List<Block> all() {
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (harvestable(block)) {
                out.add(block);
            }
        }
        return out;
    }

    public static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
