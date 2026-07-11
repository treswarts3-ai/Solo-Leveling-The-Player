package com.tre.sololeveling.dungeon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GateVisuals {
    public static BlockState frame(DungeonTypes.GateRank rank) {
        return switch (rank) {
            case E -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case D -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case C -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            case B -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case A -> Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case S -> Blocks.OBSIDIAN.defaultBlockState();
        };
    }

    public static BlockState accent(DungeonTypes.GateRank rank) {
        return switch (rank) {
            case E -> Blocks.SCULK.defaultBlockState();
            case D -> Blocks.SEA_LANTERN.defaultBlockState();
            case C -> Blocks.LAPIS_BLOCK.defaultBlockState();
            case B -> Blocks.BUDDING_AMETHYST.defaultBlockState();
            case A -> Blocks.MAGMA_BLOCK.defaultBlockState();
            case S -> Blocks.GILDED_BLACKSTONE.defaultBlockState();
        };
    }

    public static SimpleParticleType primaryParticle(DungeonTypes.GateRank rank) {
        return switch (rank) {
            case E -> ParticleTypes.SOUL_FIRE_FLAME;
            case D -> ParticleTypes.HAPPY_VILLAGER;
            case C -> ParticleTypes.SOUL_FIRE_FLAME;
            case B -> ParticleTypes.WITCH;
            case A -> ParticleTypes.FLAME;
            case S -> ParticleTypes.END_ROD;
        };
    }

    public static SimpleParticleType secondaryParticle(DungeonTypes.GateRank rank) {
        return switch (rank) {
            case E -> ParticleTypes.END_ROD;
            case D -> ParticleTypes.GLOW;
            case C -> ParticleTypes.REVERSE_PORTAL;
            case B -> ParticleTypes.PORTAL;
            case A -> ParticleTypes.REVERSE_PORTAL;
            case S -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    public static int lightLevel(DungeonTypes.GateRank rank) {
        return 9 + rank.ordinal();
    }

    public static float ambientPitch(DungeonTypes.GateRank rank) {
        return 1.08F - rank.ordinal() * 0.07F;
    }

    public static boolean isManagedGateBlock(BlockState state) {
        return state.is(DungeonBlocks.GATE_PORTAL.get())
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.PRISMARINE_BRICKS)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.AMETHYST_BLOCK)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.SCULK)
                || state.is(Blocks.SEA_LANTERN)
                || state.is(Blocks.LAPIS_BLOCK)
                || state.is(Blocks.BUDDING_AMETHYST)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.GILDED_BLACKSTONE)
                || state.is(Blocks.PURPLE_STAINED_GLASS);
    }

    private GateVisuals() {}
}
