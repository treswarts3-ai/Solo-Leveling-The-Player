package com.tre.sololeveling.dungeon;

import com.tre.sololeveling.SoloLevelingMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DungeonBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SoloLevelingMod.MODID);

    public static final RegistryObject<Block> GATE_PORTAL = BLOCKS.register("gate_portal", GatePortalBlock::new);

    private DungeonBlocks() {}
}
