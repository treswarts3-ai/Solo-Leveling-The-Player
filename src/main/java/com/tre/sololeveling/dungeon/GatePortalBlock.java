package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public final class GatePortalBlock extends Block {
    public GatePortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(-1.0F, 3_600_000.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 11)
                .pushReaction(PushReaction.BLOCK));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(80) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.35F,
                    0.85F + random.nextFloat() * 0.25F, false);
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + 0.44D + random.nextDouble() * 0.12D;
            double dx = (random.nextDouble() - 0.5D) * 0.04D;
            double dy = (random.nextDouble() - 0.25D) * 0.03D;
            double dz = (random.nextDouble() - 0.5D) * 0.02D;
            level.addParticle(random.nextBoolean() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL,
                    x, y, z, dx, dy, dz);
        }
    }
}
