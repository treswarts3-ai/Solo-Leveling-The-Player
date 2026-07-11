package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;

public final class GatePortalBlock extends Block {
    public static final EnumProperty<DungeonTypes.GateRank> RANK =
            EnumProperty.create("rank", DungeonTypes.GateRank.class);

    public GatePortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(-1.0F, 3_600_000.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> GateVisuals.lightLevel(state.getValue(RANK)))
                .pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(RANK, DungeonTypes.GateRank.E));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RANK);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        DungeonTypes.GateRank rank = state.getValue(RANK);
        int soundInterval = Math.max(38, 105 - rank.ordinal() * 11);
        if (random.nextInt(soundInterval) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.24F + rank.ordinal() * 0.025F,
                    GateVisuals.ambientPitch(rank) + random.nextFloat() * 0.08F, false);
        }

        int particleCount = rank.ordinal() >= DungeonTypes.GateRank.A.ordinal() ? 2 : 1;
        for (int i = 0; i < particleCount; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.42D + random.nextDouble() * 0.16D;
            double dx = (0.5D - (x - pos.getX())) * 0.025D + (random.nextDouble() - 0.5D) * 0.012D;
            double dy = 0.008D + random.nextDouble() * 0.025D;
            double dz = (random.nextDouble() - 0.5D) * 0.018D;
            level.addParticle(random.nextInt(3) == 0
                            ? GateVisuals.secondaryParticle(rank)
                            : GateVisuals.primaryParticle(rank),
                    x, y, z, dx, dy, dz);
        }
    }
}
