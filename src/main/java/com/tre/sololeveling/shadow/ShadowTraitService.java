package com.tre.sololeveling.shadow;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/** Named-shadow traits are behavior hooks, not another layer of passive stat inflation. */
public final class ShadowTraitService {
    public static float adjustShadowDamage(Entity attacker, LivingEntity victim, float amount) {
        if (!ShadowSummoningService.isShadow(attacker)) return amount;
        if (trait(attacker) == ShadowStorage.Trait.EXECUTIONER && victim.getHealth() <= victim.getMaxHealth() * 0.35F) {
            return amount * 1.35F;
        }
        return amount;
    }

    public static void onKill(ServerPlayer owner, Entity shadow) {
        if (trait(shadow) == ShadowStorage.Trait.SOUL_SIPHON) {
            HunterData.setMana(owner, Math.min(HunterData.getMaxMana(owner), HunterData.getMana(owner) + 12));
        }
    }

    public static void onOwnerHurt(ServerPlayer owner) {
        for (UUID id : ShadowSummoningService.activeIds(owner)) {
            Entity entity = ShadowSummoningService.findEntity(owner.getServer(), id);
            if (!(entity instanceof Mob mob) || mob.level() != owner.level()) continue;
            ShadowStorage.Trait trait = trait(mob);
            if (trait == ShadowStorage.Trait.BULWARK && mob.distanceToSqr(owner) <= 12.0D * 12.0D) {
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
            } else if (trait == ShadowStorage.Trait.BLINK_GUARD && mob.distanceToSqr(owner) > 5.0D * 5.0D) {
                mob.teleportTo(owner.getX() + owner.getRandom().nextDouble() * 2.0D - 1.0D,
                        owner.getY(), owner.getZ() + owner.getRandom().nextDouble() * 2.0D - 1.0D);
                owner.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 0.6D,
                        mob.getZ(), 12, 0.25D, 0.4D, 0.25D, 0.04D);
            }
        }
    }

    private static ShadowStorage.Trait trait(Entity shadow) {
        CompoundTag data = shadow.getPersistentData();
        return ShadowStorage.Trait.parse(data.getString(ShadowSummoningService.TAG_TRAIT));
    }

    private ShadowTraitService() {}
}
