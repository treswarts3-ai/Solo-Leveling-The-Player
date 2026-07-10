package com.tre.sololeveling.gameplay;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/** Server-side passive skill processing. */
public final class PassiveHandler {
    private static final int RECOVERY_COMBAT_DELAY = 20 * 8;

    public static void tick(ServerPlayer player) {
        if (!HunterData.isAwakened(player)) return;
        if (player.tickCount % 20 == 0) {
            tickRecovery(player);
            tickDetoxification(player);
            tickLongevity(player);
            tickStealth(player);
        }
    }

    private static void tickRecovery(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "will_to_recover") || HunterData.ticksSinceCombat(player) < RECOVERY_COMBAT_DELAY) return;
        if (player.getHealth() <= 0.0F || player.getHealth() >= player.getMaxHealth()) return;
        float amount = 0.5F + HunterData.getStat(player, "stamina") * 0.01F;
        if (player.isSleeping()) amount *= 4.0F;
        player.heal(Math.min(4.0F, amount));
    }

    private static void tickDetoxification(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "immunity_detoxification")) return;
        MobEffectInstance poison = player.getEffect(MobEffects.POISON);
        if (poison == null) return;
        int level = HunterData.getLevel(player);
        if (level >= 50 || poison.getAmplifier() == 0) {
            player.removeEffect(MobEffects.POISON);
            if (player.tickCount % 100 == 0) {
                player.sendSystemMessage(Component.literal("[SYSTEM] Toxin purified.").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private static void tickLongevity(ServerPlayer player) {
        if (!HunterData.hasSkill(player, "longevity") || player.tickCount % 100 != 0) return;
        List<MobEffectInstance> replacements = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() != MobEffectCategory.HARMFUL || effect.isInfiniteDuration()) continue;
            int reduced = Math.max(1, effect.getDuration() - Math.max(20, effect.getDuration() / 5));
            replacements.add(new MobEffectInstance(effect.getEffect(), reduced, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
        for (MobEffectInstance replacement : replacements) {
            player.removeEffect(replacement.getEffect());
            player.addEffect(replacement);
        }
    }

    private static void tickStealth(ServerPlayer player) {
        if (!HunterData.isStealthed(player)) return;
        if (player.level().getGameTime() >= HunterData.stealthUntil(player)
                || !HunterData.hasSkill(player, "stealth")
                || !HunterData.spendMana(player, 2)) {
            HunterData.endStealth(player);
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, true));
    }

    public static void breakStealth(ServerPlayer player) {
        if (HunterData.isStealthed(player)) HunterData.endStealth(player);
    }

    private PassiveHandler() {}
}
