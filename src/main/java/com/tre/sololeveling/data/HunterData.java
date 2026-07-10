package com.tre.sololeveling.data;

import com.tre.sololeveling.network.ModNetwork;
import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.network.packet.SyncHunterDataPacket;
import com.tre.sololeveling.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

public final class HunterData {
    public static final String KEY = "SoloLevelingHunterData";
    public static final int DATA_VERSION = 2;
    private static final UUID STRENGTH_DAMAGE = UUID.fromString("0b95a0a4-cf1a-41c0-b0f4-9a8dd83d5831");
    private static final UUID AGILITY_SPEED = UUID.fromString("f0cff684-33cc-4a42-bf40-18d2551d0786");
    private static final UUID STAMINA_HEALTH = UUID.fromString("bbfc2c5f-c616-46e7-934e-1a3f8ef45e90");
    private static final UUID ACCESSORY_SPEED = UUID.fromString("72d476da-b861-48f5-9abc-d0a8392da00f");
    private static final UUID ACCESSORY_DAMAGE = UUID.fromString("0e94b29a-c119-4a86-9343-166371dca330");
    private static final UUID HIGH_KNIGHT_ARMOR = UUID.fromString("b3018dcf-2a75-4669-a086-654f19de844d");
    private static final UUID HIGH_KNIGHT_KNOCKBACK = UUID.fromString("565e315a-71d0-48e3-a61d-ef98ed8d08cc");
    private static final UUID ASSASSIN_SPEED = UUID.fromString("6f42fd6e-99d8-44ad-b78a-725c8dc33d27");
    private static final UUID ASSASSIN_ATTACK_SPEED = UUID.fromString("f522a111-eeb9-4d5f-83a0-755644f98a44");
    private static final UUID TRUTH_SEEKER_ARMOR = UUID.fromString("a577eb07-70d0-4913-8d60-fee1f3eb56b4");

    private static CompoundTag raw(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(KEY, Tag.TAG_COMPOUND)) {
            CompoundTag data = defaults();
            root.put(KEY, data);
            return data;
        }
        CompoundTag data = root.getCompound(KEY);
        migrate(data);
        return data;
    }

    private static void migrate(CompoundTag data) {
        CompoundTag defaults = defaults();
        for (String key : defaults.getAllKeys()) {
            if (!data.contains(key)) {
                Tag value = defaults.get(key);
                if (value != null) data.put(key, value.copy());
            }
        }
        data.putInt("data_version", DATA_VERSION);
    }

    private static CompoundTag defaults() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("data_version", DATA_VERSION);
        tag.putBoolean("awakened", false);
        tag.putInt("level", 1);
        tag.putInt("xp", 0);
        tag.putInt("stat_points", 0);
        tag.putInt("strength", 1);
        tag.putInt("agility", 1);
        tag.putInt("stamina", 1);
        tag.putInt("intelligence", 1);
        tag.putInt("sense", 1);
        tag.putInt("mana", 110);
        tag.putInt("gold", 0);
        tag.putString("job", "None");
        tag.putString("title", "The Player");
        tag.putString("rank_override", "");
        tag.putBoolean("hud", true);
        tag.putBoolean("black_heart", false);
        tag.putInt("shadow_capacity_bonus", 0);
        tag.putLong("last_combat", Long.MIN_VALUE / 4L);
        tag.putBoolean("stealth_active", false);
        tag.putLong("stealth_until", 0L);
        tag.putString("authority_held", "");
        tag.putLong("authority_hold_until", 0L);
        tag.putLong("authority_flight_until", 0L);
        tag.putInt("shadow_mode", 0);
        tag.putInt("progression_stage", 0);
        tag.putString("active_main_quest", "awakening_tutorial");
        tag.putBoolean("tutorial_system_opened", false);
        tag.putBoolean("tutorial_stat_allocated", false);
        tag.putInt("total_kills", 0);
        tag.putInt("shadow_extractions", 0);
        tag.putBoolean("shadow_summoned_once", false);
        tag.putBoolean("emergency_active", false);
        tag.putInt("emergency_kills", 0);
        tag.put("unlocked_titles", new ListTag());
        tag.put("completed_quests", new ListTag());
        tag.put("notifications", new ListTag());
        tag.put("shadows", new ListTag());
        tag.put("active_shadows", new ListTag());
        tag.put("imprints", new ListTag());
        tag.put("system_inventory", new ListTag());
        resetDaily(tag);
        return tag;
    }

    public static CompoundTag snapshot(Player player) { return raw(player).copy(); }
    public static CompoundTag mutable(ServerPlayer player) { return raw(player); }
    public static void copy(Player from, Player to) { to.getPersistentData().put(KEY, raw(from).copy()); }
    public static void sync(ServerPlayer player) { ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncHunterDataPacket(snapshot(player))); }

    public static void awaken(ServerPlayer player) {
        CompoundTag tag = raw(player);
        if (!tag.getBoolean("awakened")) {
            tag.putBoolean("awakened", true);
            tag.putInt("stat_points", Math.max(5, tag.getInt("stat_points")));
            player.sendSystemMessage(Component.literal("[SYSTEM] You have awakened as the Player.").withStyle(ChatFormatting.AQUA));
            player.level().playSound(null, player.blockPosition(), ModSounds.SYSTEM.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        unlockByLevel(player);
        sync(player);
    }

    public static boolean isAwakened(Player player) { return raw(player).getBoolean("awakened"); }
    public static int getLevel(Player player) { return Math.max(1, raw(player).getInt("level")); }
    public static int getXp(Player player) { return Math.max(0, raw(player).getInt("xp")); }
    public static int xpNeeded(int level) { return Math.max(100, (int)Math.floor(100.0D * Math.pow(Math.max(1, level), 1.55D))); }
    public static int getMana(Player player) { return Math.max(0, raw(player).getInt("mana")); }
    public static int getGold(Player player) { return Math.max(0, raw(player).getInt("gold")); }
    public static int getStat(Player player, String stat) { return Math.max(1, raw(player).getInt(normalizeStat(stat))); }
    public static int getStatPoints(Player player) { return Math.max(0, raw(player).getInt("stat_points")); }

    public static int getMaxMana(Player player) {
        CompoundTag tag = raw(player);
        int max = 100 + getLevel(player) * 2 + getStat(player, "intelligence") * 8;
        if (tag.getBoolean("black_heart")) max += 1000;
        if (equipped(player, "ring").contains("high_magicians_ring")) max += 150;
        if (fullShadowSet(player)) max += 300;
        if (fullTruthSeekerSet(player)) max += 200;
        if (fullDemonMonarchSet(player)) max += 250;
        return max;
    }

    public static int getShadowCapacity(Player player) {
        int cap = 3 + getStat(player, "intelligence") / 5 + getLevel(player) / 10 + raw(player).getInt("shadow_capacity_bonus");
        if (raw(player).getBoolean("black_heart")) cap += 20;
        if (fullShadowSet(player)) cap += 15;
        if (fullDemonMonarchSet(player)) cap += 8;
        return Math.min(100, cap);
    }

    public static String getRank(Player player) {
        String override = raw(player).getString("rank_override");
        if (!override.isBlank()) return override;
        int level = getLevel(player);
        if (level >= 100 && raw(player).getBoolean("black_heart")) return "Shadow Monarch";
        if (level >= 80) return "National-Level";
        if (level >= 60) return "S-Rank";
        if (level >= 40) return "A-Rank";
        if (level >= 30) return "B-Rank";
        if (level >= 20) return "C-Rank";
        if (level >= 10) return "D-Rank";
        return "E-Rank";
    }

    public static void addXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        CompoundTag tag = raw(player);
        int xp = tag.getInt("xp") + amount;
        int level = getLevel(player);
        int oldLevel = level;
        int maximumLevel = ModConfigs.MAX_HUNTER_LEVEL.get();
        while (level < maximumLevel && xp >= xpNeeded(level)) {
            xp -= xpNeeded(level);
            level++;
            tag.putInt("strength", getStat(player, "strength") + 1);
            tag.putInt("agility", getStat(player, "agility") + 1);
            tag.putInt("stamina", getStat(player, "stamina") + 1);
            tag.putInt("intelligence", getStat(player, "intelligence") + 1);
            tag.putInt("sense", getStat(player, "sense") + 1);
            tag.putInt("stat_points", tag.getInt("stat_points") + 5);
        }
        tag.putInt("level", level);
        tag.putInt("xp", Math.max(0, xp));
        if (level > oldLevel) {
            tag.putInt("mana", getMaxMana(player));
            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();
            unlockByLevel(player);
            player.sendSystemMessage(Component.literal("[LEVEL UP] " + oldLevel + " -> " + level).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 35, 0.7, 1.0, 0.7, 0.08);
            player.serverLevel().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 50, 0.8, 1.0, 0.8, 0.3);
            player.level().playSound(null, player.blockPosition(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        sync(player);
    }

    public static void setXp(ServerPlayer player, int xp) { raw(player).putInt("xp", 0); addXp(player, Math.max(0, xp)); }

    public static void setLevel(ServerPlayer player, int newLevel) {
        newLevel = Math.max(1, Math.min(ModConfigs.MAX_HUNTER_LEVEL.get(), newLevel));
        CompoundTag tag = raw(player);
        int old = getLevel(player);
        int delta = newLevel - old;
        tag.putInt("level", newLevel);
        tag.putInt("xp", 0);
        if (delta > 0) {
            for (String stat : new String[]{"strength", "agility", "stamina", "intelligence", "sense"}) tag.putInt(stat, getStat(player, stat) + delta);
            tag.putInt("stat_points", tag.getInt("stat_points") + delta * 5);
        }
        tag.putInt("mana", getMaxMana(player));
        unlockByLevel(player);
        sync(player);
    }

    public static void addMana(ServerPlayer player, int amount) { CompoundTag tag = raw(player); tag.putInt("mana", Math.min(getMaxMana(player), Math.max(0, tag.getInt("mana") + amount))); }
    public static void setMana(ServerPlayer player, int amount) { raw(player).putInt("mana", Math.max(0, Math.min(getMaxMana(player), amount))); sync(player); }

    public static boolean spendMana(ServerPlayer player, int amount) {
        CompoundTag tag = raw(player);
        int current = tag.getInt("mana");
        if (current < amount) {
            player.level().playSound(null, player.blockPosition(), ModSounds.MANA_FAIL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            player.sendSystemMessage(Component.literal("[SYSTEM] Insufficient mana.").withStyle(ChatFormatting.RED));
            return false;
        }
        tag.putInt("mana", current - amount);
        return true;
    }

    public static void addGold(ServerPlayer player, int amount) { CompoundTag tag = raw(player); long value = (long)tag.getInt("gold") + amount; tag.putInt("gold", (int)Math.max(0, Math.min(Integer.MAX_VALUE, value))); }
    public static void setGold(ServerPlayer player, int amount) { raw(player).putInt("gold", Math.max(0, amount)); sync(player); }
    public static boolean spendGold(ServerPlayer player, int amount) { CompoundTag tag = raw(player); if (amount < 0 || tag.getInt("gold") < amount) return false; tag.putInt("gold", tag.getInt("gold") - amount); return true; }

    public static boolean allocate(ServerPlayer player, String stat, int amount) {
        String key = normalizeStat(stat);
        if (amount <= 0 || !isPrimaryStat(key)) return false;
        CompoundTag tag = raw(player);
        int used = Math.min(tag.getInt("stat_points"), amount);
        if (used <= 0) return false;
        tag.putInt(key, Math.max(1, tag.getInt(key)) + used);
        tag.putBoolean("tutorial_stat_allocated", true);
        tag.putInt("stat_points", tag.getInt("stat_points") - used);
        tag.putInt("mana", Math.min(getMaxMana(player), tag.getInt("mana") + (key.equals("intelligence") ? used * 8 : 0)));
        sync(player);
        return true;
    }

    public static void setStat(ServerPlayer player, String stat, int value) { String key = normalizeStat(stat); if (isPrimaryStat(key)) { raw(player).putInt(key, Math.max(1, Math.min(10000, value))); sync(player); } }
    public static void addStatPoints(ServerPlayer player, int amount) { CompoundTag tag = raw(player); tag.putInt("stat_points", Math.max(0, tag.getInt("stat_points") + amount)); sync(player); }
    public static void setStatPoints(ServerPlayer player, int amount) { raw(player).putInt("stat_points", Math.max(0, amount)); sync(player); }

    public static void resetStats(ServerPlayer player) {
        CompoundTag tag = raw(player); int total = 0;
        for (String stat : new String[]{"strength", "agility", "stamina", "intelligence", "sense"}) { total += Math.max(0, tag.getInt(stat) - 1); tag.putInt(stat, 1); }
        tag.putInt("stat_points", tag.getInt("stat_points") + total); tag.putInt("mana", getMaxMana(player)); sync(player);
    }

    public static boolean unlockSkill(ServerPlayer player, String skill) { String key = "skill_" + skill.toLowerCase(Locale.ROOT); CompoundTag tag = raw(player); boolean fresh = !tag.getBoolean(key); tag.putBoolean(key, true); sync(player); return fresh; }
    public static void lockSkill(ServerPlayer player, String skill) { raw(player).putBoolean("skill_" + skill.toLowerCase(Locale.ROOT), false); sync(player); }
    public static boolean hasSkill(Player player, String skill) { return raw(player).getBoolean("skill_" + skill.toLowerCase(Locale.ROOT)); }
    public static boolean cooldownReady(ServerPlayer player, String skill) { return player.level().getGameTime() >= raw(player).getLong("cooldown_" + skill); }
    public static long cooldownRemaining(ServerPlayer player, String skill) { return Math.max(0L, raw(player).getLong("cooldown_" + skill) - player.level().getGameTime()); }
    public static void setCooldown(ServerPlayer player, String skill, int ticks) { raw(player).putLong("cooldown_" + skill, player.level().getGameTime() + Math.max(0, ticks)); }
    public static void clearCooldowns(ServerPlayer player) { CompoundTag tag = raw(player); for (String skill : SKILLS) tag.remove("cooldown_" + skill); sync(player); }

    public static void recordCombat(ServerPlayer player) { raw(player).putLong("last_combat", player.level().getGameTime()); }
    public static long ticksSinceCombat(ServerPlayer player) { return Math.max(0L, player.level().getGameTime() - raw(player).getLong("last_combat")); }
    public static void beginStealth(ServerPlayer player, int durationTicks) {
        CompoundTag tag = raw(player);
        tag.putBoolean("stealth_active", true);
        tag.putLong("stealth_until", player.level().getGameTime() + Math.max(1, durationTicks));
    }
    public static boolean isStealthed(Player player) { return raw(player).getBoolean("stealth_active"); }
    public static void endStealth(ServerPlayer player) {
        CompoundTag tag = raw(player);
        tag.putBoolean("stealth_active", false);
        tag.putLong("stealth_until", 0L);
        player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        sync(player);
    }
    public static long stealthUntil(Player player) { return raw(player).getLong("stealth_until"); }

    public static void tick(ServerPlayer player) {
        CompoundTag tag = raw(player); checkDailyReset(player, tag); if (!isAwakened(player)) return;
        if (player.tickCount % 20 == 0) {
            int regen = 2 + getStat(player, "intelligence") / 20 + (tag.getBoolean("black_heart") ? 8 : 0);
            if (equipped(player, "ring").contains("high_magicians_ring")) regen += 3;
            tag.putInt("mana", Math.min(getMaxMana(player), tag.getInt("mana") + regen));
            applyAttributes(player); unlockByLevel(player);
            if (tag.getBoolean("monarch_domain") && !spendMana(player, 4)) tag.putBoolean("monarch_domain", false);
            sync(player);
        }
    }

    private static void applyAttributes(ServerPlayer player) {
        apply(player.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH_DAMAGE, "Hunter Strength", Math.max(0, getStat(player, "strength") - 1) * 0.15D);
        apply(player.getAttribute(Attributes.MOVEMENT_SPEED), AGILITY_SPEED, "Hunter Agility", Math.min(0.20D, Math.max(0, getStat(player, "agility") - 1) * 0.0015D));
        apply(player.getAttribute(Attributes.MAX_HEALTH), STAMINA_HEALTH, "Hunter Stamina", Math.min(80.0D, Math.max(0, getStat(player, "stamina") - 1) * 0.4D));
        apply(player.getAttribute(Attributes.MOVEMENT_SPEED), ACCESSORY_SPEED, "Accessory Agility", equipped(player, "necklace").contains("gatekeepers_necklace") ? 0.025D : 0.0D);
        apply(player.getAttribute(Attributes.ATTACK_DAMAGE), ACCESSORY_DAMAGE, "Accessory Strength", equipped(player, "hands").contains("high_knight_gauntlets") ? 2.0D : 0.0D);
        apply(player.getAttribute(Attributes.ARMOR), HIGH_KNIGHT_ARMOR, "High Knight Set", fullHighKnightSet(player) ? 4.0D : 0.0D);
        apply(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), HIGH_KNIGHT_KNOCKBACK, "High Knight Stability", fullHighKnightSet(player) ? 0.15D : 0.0D);
        apply(player.getAttribute(Attributes.MOVEMENT_SPEED), ASSASSIN_SPEED, "Assassin Set", fullAssassinSet(player) ? 0.04D : 0.0D);
        apply(player.getAttribute(Attributes.ATTACK_SPEED), ASSASSIN_ATTACK_SPEED, "Assassin Handling", fullAssassinSet(player) ? 0.60D : 0.0D);
        apply(player.getAttribute(Attributes.ARMOR), TRUTH_SEEKER_ARMOR, "Truth Seeker Set", fullTruthSeekerSet(player) ? 3.0D : 0.0D);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void apply(AttributeInstance attribute, UUID id, String name, double amount) { if (attribute == null) return; AttributeModifier old = attribute.getModifier(id); if (old != null) attribute.removeModifier(id); if (amount != 0.0D) attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION)); }

    public static void setBlackHeart(ServerPlayer player, boolean value) { raw(player).putBoolean("black_heart", value); raw(player).putInt("mana", getMaxMana(player)); sync(player); }
    public static boolean hasBlackHeart(Player player) { return raw(player).getBoolean("black_heart"); }
    public static void setJob(ServerPlayer player, String value) { raw(player).putString("job", sanitizeLabel(value, 48)); sync(player); }
    public static void setTitle(ServerPlayer player, String value) { raw(player).putString("title", sanitizeLabel(value, 64)); sync(player); }
    public static void setRankOverride(ServerPlayer player, String value) { raw(player).putString("rank_override", sanitizeLabel(value, 32)); sync(player); }
    public static void clearRankOverride(ServerPlayer player) { raw(player).putString("rank_override", ""); sync(player); }
    public static void setProgressionStage(ServerPlayer player, int stage) {
        CompoundTag tag = raw(player);
        int value = Math.max(0, Math.min(5, stage));
        tag.putInt("progression_stage", value);
        tag.putString("active_main_quest", switch (value) {
            case 0 -> "awakening_tutorial";
            case 1 -> "dagger_training";
            case 2 -> "job_change";
            case 3 -> "shadow_mastery";
            case 4 -> "black_heart_trial";
            default -> "completed";
        });
        sync(player);
    }
    public static void resetProgression(ServerPlayer player) {
        CompoundTag fresh = defaults();
        CompoundTag tag = raw(player);
        boolean awakened = tag.getBoolean("awakened");
        int level = tag.getInt("level");
        int xp = tag.getInt("xp");
        int gold = tag.getInt("gold");
        for (String key : fresh.getAllKeys()) {
            Tag value = fresh.get(key);
            if (value != null) tag.put(key, value.copy());
        }
        tag.putBoolean("awakened", awakened);
        tag.putInt("level", Math.max(1, level));
        tag.putInt("xp", Math.max(0, xp));
        tag.putInt("gold", Math.max(0, gold));
        unlockByLevel(player);
        sync(player);
    }
    public static void toggleHud(ServerPlayer player) { raw(player).putBoolean("hud", !raw(player).getBoolean("hud")); sync(player); }

    public static void toggleAccessory(ServerPlayer player, String slot, Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item); if (id == null) return; CompoundTag tag = raw(player); String key = "accessory_" + slot;
        if (tag.getString(key).equals(id.toString())) { tag.remove(key); player.sendSystemMessage(Component.literal("[SYSTEM] Accessory unequipped.").withStyle(ChatFormatting.GRAY)); }
        else { tag.putString(key, id.toString()); player.sendSystemMessage(Component.literal("[SYSTEM] Equipped " + item.getDescription().getString()).withStyle(ChatFormatting.AQUA)); }
        sync(player);
    }
    public static String equipped(Player player, String slot) { return raw(player).getString("accessory_" + slot); }
    private static boolean armor(Player player, int slot, String id) { return player.getInventory().armor.get(slot).getItem().toString().contains(id); }
    private static boolean fullHighKnightSet(Player player) { return armor(player,3,"high_knight_helmet") && armor(player,2,"high_knight_chestplate") && armor(player,1,"high_knight_leggings") && armor(player,0,"high_knight_boots") && equipped(player,"hands").contains("high_knight_gauntlets"); }
    private static boolean fullAssassinSet(Player player) { return armor(player,3,"assassins_hood") && armor(player,2,"assassins_jacket") && armor(player,1,"assassins_trousers") && armor(player,0,"assassins_shoes"); }
    private static boolean fullTruthSeekerSet(Player player) { return armor(player,2,"truth_seekers_top") && armor(player,1,"truth_seekers_pants") && armor(player,0,"truth_seekers_shoes") && equipped(player,"hands").contains("truth_seekers_gloves"); }
    private static boolean fullDemonMonarchSet(Player player) { return equipped(player,"earring").contains("demon_monarch_earring") && equipped(player,"necklace").contains("demon_monarch_necklace") && equipped(player,"ring").contains("demon_monarch_ring"); }
    private static boolean fullShadowSet(Player player) { return armor(player,2,"shadow_monarch_coat") && armor(player,1,"shadow_monarch_trousers") && armor(player,0,"shadow_monarch_boots") && equipped(player,"hands").contains("shadow_monarch_gloves"); }

    public static boolean storeSystemItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        ListTag list = raw(player).getList("system_inventory", Tag.TAG_COMPOUND);
        if (list.size() >= 108) {
            player.sendSystemMessage(Component.literal("[SYSTEM] System inventory is full.").withStyle(ChatFormatting.RED));
            return false;
        }
        list.add(stack.save(new CompoundTag()));
        raw(player).put("system_inventory", list);
        sync(player);
        return true;
    }
    public static int systemInventorySize(Player player) { return raw(player).getList("system_inventory", Tag.TAG_COMPOUND).size(); }
    public static boolean retrieveFirst(ServerPlayer player) { return retrieveSystemItem(player, 0); }
    public static boolean retrieveSystemItem(ServerPlayer player, int index) {
        ListTag list = raw(player).getList("system_inventory", Tag.TAG_COMPOUND);
        if (index < 0 || index >= list.size()) return false;
        ItemStack stack = ItemStack.of(list.getCompound(index));
        if (stack.isEmpty()) {
            list.remove(index);
            raw(player).put("system_inventory", list);
            sync(player);
            return false;
        }
        int before = stack.getCount();
        player.getInventory().add(stack);
        int moved = before - stack.getCount();
        if (moved <= 0) {
            player.sendSystemMessage(Component.literal("[SYSTEM] Your inventory is full.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (stack.isEmpty()) list.remove(index);
        else list.set(index, stack.save(new CompoundTag()));
        raw(player).put("system_inventory", list);
        sync(player);
        return true;
    }
    public static boolean storeHeld(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || held.getItem().toString().contains("black_heart")) return false;
        if (!storeSystemItem(player, held.copy())) return false;
        held.setCount(0);
        return true;
    }

    public static ListTag shadows(Player player) { return raw(player).getList("shadows", Tag.TAG_COMPOUND); }
    public static void setShadows(Player player, ListTag list) { raw(player).put("shadows", list); }
    public static ListTag imprints(Player player) { return raw(player).getList("imprints", Tag.TAG_COMPOUND); }
    public static void setImprints(Player player, ListTag list) { raw(player).put("imprints", list); }
    public static ListTag activeShadows(Player player) { return raw(player).getList("active_shadows", Tag.TAG_STRING); }
    public static void setActiveShadows(Player player, ListTag list) { raw(player).put("active_shadows", list); }
    public static void addActiveShadow(Player player, UUID id) { ListTag list = activeShadows(player); list.add(StringTag.valueOf(id.toString())); setActiveShadows(player, list); }
    public static void addShadowCapacity(ServerPlayer player, int amount) { raw(player).putInt("shadow_capacity_bonus", Math.max(0, raw(player).getInt("shadow_capacity_bonus") + amount)); sync(player); }
    public static void setShadowCapacityBonus(ServerPlayer player, int amount) { raw(player).putInt("shadow_capacity_bonus", Math.max(0, amount)); sync(player); }
    public static boolean domainActive(Player player) { return raw(player).getBoolean("monarch_domain"); }
    public static void toggleDomain(ServerPlayer player) { raw(player).putBoolean("monarch_domain", !raw(player).getBoolean("monarch_domain")); sync(player); }
    public static void progressDaggerDamage(ServerPlayer player, int amount) { CompoundTag tag = raw(player); tag.putInt("quest_dagger_damage", tag.getInt("quest_dagger_damage") + Math.max(0, amount)); }

    public static void unlockByLevel(ServerPlayer player) {
        int level = getLevel(player);
        raw(player).putBoolean("skill_will_to_recover", true);
        if (level >= 5) raw(player).putBoolean("skill_stealth", true);
        if (level >= 10) { raw(player).putBoolean("skill_bloodlust", true); raw(player).putBoolean("skill_longevity", true); }
        if (level >= 15) raw(player).putBoolean("skill_quicksilver", true);
        if (level >= 20) { raw(player).putBoolean("skill_mutilation", true); raw(player).putBoolean("skill_immunity_detoxification", true); }
        if (level >= 25) { raw(player).putBoolean("skill_dagger_rush", true); raw(player).putBoolean("skill_tenacity", true); }
        if (level >= 30) raw(player).putBoolean("skill_rulers_authority", true);
        if (level >= 35) raw(player).putBoolean("skill_dragons_fear", true);
    }

    private static void checkDailyReset(ServerPlayer player, CompoundTag tag) {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        if (today.equals(tag.getString("daily_date"))) return;
        if (tag.getBoolean("awakened") && !tag.getString("daily_date").isBlank()
                && !tag.getBoolean("daily_claimed") && !tag.getBoolean("daily_complete")) {
            tag.putBoolean("penalty_pending", true);
        }
        resetDaily(tag);
    }
    public static void resetDaily(ServerPlayer player) { resetDaily(raw(player)); sync(player); }
    private static void resetDaily(CompoundTag tag) { tag.putString("daily_date", LocalDate.now(ZoneOffset.UTC).toString()); tag.putInt("daily_kills", 0); tag.putInt("daily_run", 0); tag.putInt("daily_pushups", 0); tag.putInt("daily_situps", 0); tag.putInt("daily_squats", 0); tag.putBoolean("daily_claimed", false); tag.putBoolean("daily_complete", false); tag.putLong("daily_last_exercise", 0L); }

    public static final String[] SKILLS = {"will_to_recover", "longevity", "immunity_detoxification", "tenacity", "advanced_dagger_techniques", "stealth", "bloodlust", "quicksilver", "mutilation", "dagger_rush", "rulers_authority", "dragons_fear", "shadow_extraction", "shadow_preservation", "shadow_exchange", "monarch_domain"};
    private static String sanitizeLabel(String value, int maxLength) {
        if (value == null) return "";
        String clean = value.replaceAll("[\\p{Cntrl}§]", "").trim();
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }
    private static String normalizeStat(String stat) { return stat.toLowerCase(Locale.ROOT).replace(" ", "_"); }
    private static boolean isPrimaryStat(String stat) { return stat.equals("strength") || stat.equals("agility") || stat.equals("stamina") || stat.equals("intelligence") || stat.equals("sense"); }
    private HunterData() {}
}
