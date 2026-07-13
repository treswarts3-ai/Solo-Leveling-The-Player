package com.tre.sololeveling.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public final class DungeonEnemies {
    private static final String TAG_COLLECTION_CARRIER = "sl_collection_carrier";
    private static final int[][] SPAWN_OFFSETS = {
            {-6, -3}, {6, -3}, {-6, 3}, {6, 3}, {0, -5}, {0, 5},
            {-4, 0}, {4, 0}, {-8, 0}, {8, 0}, {-3, 4}, {3, 4},
            {-3, -4}, {3, -4}, {-7, 4}, {7, 4}
    };

    public static int spawnWave(ServerLevel level, DungeonSession session, DungeonTypes.WaveDefinition wave, DungeonTypes.GateRank rank) {
        if (wave == null || session.liveEnemyCount() >= DungeonTypes.MAX_LIVE_ENEMIES
                || session.totalSpawns() >= DungeonTypes.MAX_WAVE_SPAWNS) return 0;
        int spawned = 0;
        int sequence = 0;
        for (DungeonTypes.SpawnEntry entry : wave.entries()) {
            for (int i = 0; i < entry.count(); i++) {
                if (session.liveEnemyCount() >= DungeonTypes.MAX_LIVE_ENEMIES
                        || session.totalSpawns() >= DungeonTypes.MAX_WAVE_SPAWNS) return spawned;
                BlockPos position = spawnPosition(level, session, sequence++);
                LivingEntity entity = spawn(level, session, entry.enemyId(), position, rank, wave.collectionDrops());
                if (entity != null) spawned++;
            }
        }
        return spawned;
    }

    public static LivingEntity spawn(ServerLevel level, DungeonSession session, String enemyId, BlockPos position,
                                      DungeonTypes.GateRank rank, boolean collectionCarrier) {
        DungeonTypes.EnemyDefinition definition = DungeonContent.enemy(enemyId);
        if (definition == null || session.liveEnemyCount() >= DungeonTypes.MAX_LIVE_ENEMIES
                || session.totalSpawns() >= DungeonTypes.MAX_WAVE_SPAWNS) return null;
        Mob mob = create(level, definition);
        if (mob == null) return null;
        BlockPos safe = findSafeSpawn(level, mob, position);
        if (safe == null) return null;
        mob.moveTo(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(safe);
        mob.finalizeSpawn(level, difficulty, MobSpawnType.COMMAND, null, null);
        configure(mob, definition, rank);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.getPersistentData().putBoolean(DungeonTypes.TAG_DUNGEON_ENEMY, true);
        mob.getPersistentData().putUUID(DungeonTypes.TAG_SESSION, session.sessionId());
        mob.getPersistentData().putString(DungeonTypes.TAG_ENEMY_ID, definition.id());
        mob.getPersistentData().putBoolean(DungeonTypes.TAG_SHADOW_EXTRACTABLE, definition.shadowExtractable());
        mob.getPersistentData().putBoolean(TAG_COLLECTION_CARRIER, collectionCarrier);
        if (!level.addFreshEntity(mob)) return null;
        session.enemySpawned(mob.getUUID());
        return mob;
    }

    public static boolean isDungeonEnemy(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(DungeonTypes.TAG_DUNGEON_ENEMY);
    }

    public static UUID sessionId(LivingEntity entity) {
        return entity.getPersistentData().hasUUID(DungeonTypes.TAG_SESSION)
                ? entity.getPersistentData().getUUID(DungeonTypes.TAG_SESSION) : null;
    }

    public static String enemyId(LivingEntity entity) {
        return entity.getPersistentData().getString(DungeonTypes.TAG_ENEMY_ID);
    }

    public static boolean shadowExtractable(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(DungeonTypes.TAG_SHADOW_EXTRACTABLE);
    }

    public static void dropCollectionToken(ServerLevel level, LivingEntity entity) {
        if (!entity.getPersistentData().getBoolean(TAG_COLLECTION_CARRIER)) return;
        UUID sessionId = sessionId(entity);
        if (sessionId == null) return;
        ItemEntity token = new ItemEntity(level, entity.getX(), entity.getY() + 0.25D, entity.getZ(),
                new ItemStack(Items.AMETHYST_SHARD));
        token.setCustomName(Component.literal("Mana Crystal"));
        token.setCustomNameVisible(true);
        token.setPickUpDelay(32767);
        token.getPersistentData().putBoolean(DungeonTypes.TAG_COLLECTION_ITEM, true);
        token.getPersistentData().putUUID(DungeonTypes.TAG_SESSION, sessionId);
        level.addFreshEntity(token);
    }

    private static Mob create(ServerLevel level, DungeonTypes.EnemyDefinition definition) {
        return switch (definition.kind()) {
            case MELEE -> EntityType.ZOMBIE.create(level);
            case FAST -> EntityType.CAVE_SPIDER.create(level);
            case TANK -> EntityType.RAVAGER.create(level);
            case RANGED -> EntityType.SKELETON.create(level);
            case ELITE -> EntityType.VINDICATOR.create(level);
        };
    }

    private static void configure(Mob mob, DungeonTypes.EnemyDefinition definition, DungeonTypes.GateRank rank) {
        double scale = rank.rewardMultiplier();
        set(mob, Attributes.MAX_HEALTH, definition.maxHealth() * scale);
        set(mob, Attributes.ATTACK_DAMAGE, definition.attackDamage() * Math.max(1.0D, scale * 0.72D));
        set(mob, Attributes.MOVEMENT_SPEED, definition.movementSpeed());
        set(mob, Attributes.ARMOR, definition.armor() * Math.max(1.0D, scale * 0.55D));
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.literal(displayName(definition.id())));
        mob.setCustomNameVisible(definition.elite());
        if (mob instanceof Skeleton) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        else if (mob instanceof Vindicator) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        else if (mob instanceof Zombie) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        } else if (mob instanceof CaveSpider) mob.setGlowingTag(false);
        else if (mob instanceof Ravager) mob.setCustomNameVisible(true);
    }

    private static void set(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static BlockPos spawnPosition(ServerLevel level, DungeonSession session, int index) {
        java.util.List<BlockPos> authored = DungeonArena.encounterSpawnPoints(level, session);
        if (!authored.isEmpty()) {
            BlockPos marker = authored.get(index % authored.size());
            int ring = index / authored.size();
            if (ring == 0) return marker;
            return marker.offset((ring % 2 == 0 ? ring : -ring), 0, (ring % 3) - 1);
        }
        BlockPos center = DungeonArena.encounterCenter(session);
        int[] offset = SPAWN_OFFSETS[index % SPAWN_OFFSETS.length];
        int ring = index / SPAWN_OFFSETS.length;
        int x = offset[0] + (ring % 2 == 0 ? ring : -ring);
        int z = offset[1] + (ring % 3) - 1;
        return center.offset(x, 0, z);
    }

    private static BlockPos findSafeSpawn(ServerLevel level, Mob mob, BlockPos preferred) {
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    for (int dy = 2; dy >= -3; dy--) {
                        BlockPos feet = preferred.offset(dx, dy, dz);
                        if (!level.getWorldBorder().isWithinBounds(feet)) continue;
                        BlockPos floor = feet.below();
                        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
                        mob.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, 0.0F, 0.0F);
                        if (level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox())) return feet.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static String displayName(String id) {
        String[] words = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private DungeonEnemies() {}
}
