package com.tre.sololeveling.equipment;

import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.ability.AbilityEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EquipmentAbilityHooks {
    @FunctionalInterface
    public interface HitHook {
        void apply(ItemStack stack, LivingEntity target, LivingEntity attacker);
    }

    private static final Map<String, HitHook> HIT_HOOKS = new ConcurrentHashMap<>();

    static {
        registerHit("venom", (stack, target, attacker) -> {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        });
        registerHit("knight_killer", (stack, target, attacker) -> {
            if (target.getArmorValue() > 0) {
                bonusDamage("knight_killer", attacker, target,
                        Math.min(5.0F, 1.0F + target.getArmorValue() * 0.12F));
            }
        });
        registerHit("demon_fire", (stack, target, attacker) -> {
            target.setSecondsOnFire(5);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            particles(target, ParticleTypes.SOUL_FIRE_FLAME, 20);
        });
        registerHit("moonshadow", (stack, target, attacker) -> {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            bonusDamage("moonshadow", attacker, target,
                    4.0F + EquipmentData.upgradeLevel(stack, EquipmentCatalog.require("moonshadow_dagger")) * 0.5F);
            particles(target, ParticleTypes.SCULK_SOUL, 18);
        });
        registerHit("shadow_reaper", (stack, target, attacker) -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 70, 0));
            bonusDamage("shadow_reaper", attacker, target,
                    Math.min(10.0F, 2.0F + target.getMaxHealth() * 0.06F));
            particles(target, ParticleTypes.REVERSE_PORTAL, 24);
        });
        registerHit("demon_pair", (stack, target, attacker) -> {
            if (attacker instanceof ServerPlayer player
                    && paired(player, "demon_kings_dagger_left", "demon_kings_dagger_right")) {
                bonusDamage("demon_pair", attacker, target, 6.0F);
            }
        });
        registerHit("kamish_pair", (stack, target, attacker) -> {
            if (attacker instanceof ServerPlayer player) {
                bonusDamage("kamish_pair", attacker, target,
                        Math.min(12.0F, HunterData.getStat(player, "strength") * 0.10F));
                if (paired(player, "kamishs_wrath_left", "kamishs_wrath_right")) {
                    bonusDamage("kamish_pair_dual", attacker, target, 8.0F);
                }
            }
        });
        registerHit("antares_flame", (stack, target, attacker) -> {
            target.setSecondsOnFire(8);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            bonusDamage("antares_flame", attacker, target, 12.0F);
        });
    }

    public static void registerHit(String id, HitHook hook) {
        if (id == null || id.isBlank() || hook == null) throw new IllegalArgumentException("Invalid equipment hit hook");
        HIT_HOOKS.put(id, hook);
    }

    public static void fireHit(String id, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HitHook hook = HIT_HOOKS.get(id);
        if (hook != null) hook.apply(stack, target, attacker);
    }

    private static boolean paired(ServerPlayer player, String left, String right) {
        String main = EquipmentEffects.registryId(player.getMainHandItem());
        String off = EquipmentEffects.registryId(player.getOffhandItem());
        return (main.endsWith(left) && off.endsWith(right)) || (main.endsWith(right) && off.endsWith(left));
    }

    private static void bonusDamage(String hookId, LivingEntity attacker, LivingEntity target, float amount) {
        AbilityEffects.dealEquipmentDamage(attacker, target, amount, hookId);
    }

    private static void particles(LivingEntity target, net.minecraft.core.particles.ParticleOptions particle, int count) {
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(particle, target.getX(), target.getY() + 1.0D, target.getZ(),
                    Math.min(32, count), 0.4D, 0.6D, 0.4D, 0.03D);
        }
    }

    private EquipmentAbilityHooks() {}
}
