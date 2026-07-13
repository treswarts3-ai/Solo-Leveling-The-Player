package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Built-in abilities. Each implementation contains only its unique effect logic. */
public final class StandardAbilities {
    private static final String QUICK_UNTIL = "ability_quicksilver_until";
    private static final String BLOODLUST_UNTIL = "ability_bloodlust_until";
    private static final String MUTILATION_TARGET = "ability_mutilation_target";
    private static final String MUTILATION_HITS = "ability_mutilation_hits";
    private static final String MUTILATION_NEXT = "ability_mutilation_next";
    private static final String MUTILATION_DAMAGE = "ability_mutilation_damage";
    private static final String MUTILATION_LOCK = "ability_mutilation_lock";
    private static final String FEAR_CAST = "ability_dragons_fear_cast";
    private static final String FEAR_UNTIL = "ability_dragons_fear_until";
    private static final String FEAR_MARK_OWNER = "sl_fear_owner";
    private static final String FEAR_MARK_CAST = "sl_fear_cast";

    public static void registerAll(AbilityRegistry registry) {
        registry
                .register(new DashAbility())
                .register(new QuicksilverAbility())
                .register(new ShadowStepAbility())
                .register(new DaggerMasteryAbility())
                .register(new MutilationAbility())
                .register(new DaggerRushAbility())
                .register(new BloodlustAbility())
                .register(new AreaSlashAbility())
                .register(new StealthAbility())
                .register(new AuthorityAbility("rulers_authority", AuthorityMode.PUSH, 35, 100, 22.0D, true))
                .register(new AuthorityAbility("rulers_authority_pull", AuthorityMode.PULL, 35, 80, 22.0D, false))
                .register(new AuthorityAbility("rulers_authority_push", AuthorityMode.PUSH, 35, 80, 22.0D, false))
                .register(new AuthorityAbility("rulers_authority_hold", AuthorityMode.HOLD, 45, 160, 20.0D, false))
                .register(new AuthorityAbility("rulers_authority_throw", AuthorityMode.THROW, 30, 80, 22.0D, false))
                .register(new AuthorityAbility("rulers_authority_dash", AuthorityMode.DASH, 25, 100, 0.0D, false))
                .register(new AuthorityAbility("rulers_authority_flight", AuthorityMode.FLIGHT, 80, 600, 0.0D, false))
                .register(new EnhancedSensesAbility())
                .register(new MonarchDomainAbility())
                .register(new ShadowHookAbility("shadow_exchange", "Shadow Exchange", 80, 400, ShadowHook.EXCHANGE))
                .register(new ShadowHookAbility("shadow_extraction", "Shadow Extraction", 30, 100, ShadowHook.EXTRACTION))
                .register(new ShadowHookAbility("shadow_summoning", "Shadow Summoning", 20, 60, ShadowHook.SUMMONING))
                .register(new DragonsFearAbility())
                .alias("shadow_step_teleport", "shadow_step")
                .alias("monarchs_domain", "monarch_domain")
                .alias("ruler_authority", "rulers_authority");
    }

    private abstract static class BaseAbility implements Ability {
        private final AbilityDefinition definition;

        protected BaseAbility(AbilityDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final AbilityDefinition definition() {
            return definition;
        }
    }

    private static AbilityDefinition spec(String id, String name, String description, String category,
                                          AbilityUnlock unlock, int mana, int cooldown, double range,
                                          AbilityScaling scaling) {
        return new AbilityDefinition(id, name, description, category, unlock, mana, cooldown, range, scaling);
    }

    private static final class DashAbility extends BaseAbility {
        private DashAbility() {
            super(spec("dash", "Dash", "A short agility-scaled burst in the facing direction.", "movement",
                    AbilityUnlock.awakened(), 12, 50, 0.0D, new AbilityScaling(0, 0.01D, 0, 0, 0)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            double force = Math.min(2.4D, 1.15D + HunterData.getStat(player, "agility") * 0.008D);
            Vec3 look = player.getLookAngle().normalize();
            AbilityEffects.setClampedVelocity(player,
                    new Vec3(look.x * force, Math.max(0.12D, look.y * force + 0.08D), look.z * force),
                    2.4D, 0.8D);
            player.fallDistance = 0.0F;
            AbilityEffects.particles(context.level(), player.position().add(0, 0.8D, 0), ParticleTypes.CLOUD, 22, 0.3D);
            return AbilityResult.success("Dash activated.");
        }
    }

    private static final class QuicksilverAbility extends BaseAbility {
        private QuicksilverAbility() {
            super(spec("quicksilver", "Quicksilver", "Explode into a controlled burst and leave a short speed trail.", "movement",
                    AbilityUnlock.skillOrLevel("quicksilver", 15), 24, 220, 0.0D,
                    new AbilityScaling(0, 0.02D, 0, 0, 0)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            int agility = HunterData.getStat(player, "agility");
            int amplifier = Math.min(3, 1 + agility / 70);
            int duration = 120;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, Math.min(1, amplifier / 2), false, false, true));
            Vec3 look = player.getLookAngle().normalize();
            double burst = Math.min(1.95D, 1.15D + agility * 0.006D);
            AbilityEffects.setClampedVelocity(player,
                    new Vec3(look.x * burst, Math.max(0.08D, look.y * 0.45D + 0.08D), look.z * burst),
                    1.95D, 0.45D);
            player.fallDistance = 0.0F;
            HunterData.mutable(player).putLong(QUICK_UNTIL, player.level().getGameTime() + duration);
            context.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.9F, 1.7F);
            AbilityEffects.ring(context.level(), player.position().add(0, 0.15D, 0), ParticleTypes.END_ROD, 1.2D, 24);
            AbilityEffects.particles(context.level(), player.position().add(0, 0.6D, 0), ParticleTypes.CLOUD, 28, 0.45D);
            return AbilityResult.success("Burst active for 6 seconds.");
        }

        @Override public void tick(ServerPlayer player) {
            long until = HunterData.mutable(player).getLong(QUICK_UNTIL);
            if (until <= 0L) return;
            long now = player.level().getGameTime();
            if (now >= until || !player.isAlive()) {
                HunterData.mutable(player).putLong(QUICK_UNTIL, 0L);
                return;
            }
            if (player.tickCount % 2 == 0 && player.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
                AbilityEffects.particles(player.serverLevel(), player.position().add(0, 0.2D, 0),
                        ParticleTypes.END_ROD, 2, 0.12D);
                AbilityEffects.particles(player.serverLevel(), player.position().add(0, 0.1D, 0),
                        ParticleTypes.CLOUD, 2, 0.10D);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            HunterData.mutable(player).putLong(QUICK_UNTIL, 0L);
        }
    }

    private static final class ShadowStepAbility extends BaseAbility {
        private ShadowStepAbility() {
            super(spec("shadow_step", "Shadow Step", "Teleport behind a targeted living enemy.", "movement",
                    AbilityUnlock.skill("shadow_exchange"), 55, 200, 18.0D,
                    new AbilityScaling(0, 0.01D, 0, 0.01D, 0.01D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            LivingEntity target = AbilityTargeting.livingRayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No visible Shadow Step target.");
            Vec3 destination = AbilityEffects.safeNearTarget(player, target, 1.6D);
            if (destination == null || !AbilityEffects.moveIfClear(player, destination)) {
                return AbilityResult.failure("The destination is obstructed or unsupported.");
            }
            AbilityEffects.particles(context.level(), target.position().add(0, 1, 0), ParticleTypes.PORTAL, 42, 0.7D);
            context.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            return AbilityResult.success("Shadow Step complete.");
        }
    }

    private static final class DaggerMasteryAbility extends BaseAbility {
        private DaggerMasteryAbility() {
            super(spec("dagger_mastery", "Dagger Mastery", "Assume a focused dagger stance that increases close-combat output.", "combat",
                    AbilityUnlock.skillOrLevel("advanced_dagger_techniques", 10), 18, 300, 0.0D,
                    new AbilityScaling(0.10D, 0.08D, 0, 0, 0.02D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Dagger Mastery requires a dagger.");
            int amplifier = Math.min(3, HunterData.getStat(player, "agility") / 60);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, amplifier, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, Math.min(2, amplifier + 1), false, false, true));
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.CRIT, 28, 0.6D);
            return AbilityResult.success("Dagger Mastery stance active for 15 seconds.");
        }
    }

    private static final class MutilationAbility extends BaseAbility {
        private MutilationAbility() {
            super(spec("mutilation", "Mutilation", "Lock into dagger range and execute four rapid precision cuts.", "combat",
                    AbilityUnlock.skillOrLevel("mutilation", 20), 48, 180, 7.0D,
                    new AbilityScaling(0.24D, 0.16D, 0, 0, 0.04D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Mutilation requires a dagger.");
            LivingEntity target = AbilityTargeting.livingRayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No visible Mutilation target within 7 blocks.");
            if (player.distanceToSqr(target) > 4.0D * 4.0D) {
                Vec3 destination = AbilityEffects.safeNearTarget(player, target, 1.55D);
                if (destination == null || !AbilityEffects.moveIfClear(player, destination)) {
                    return AbilityResult.failure("No safe dagger position is available.");
                }
            }
            CompoundTag tag = HunterData.mutable(player);
            tag.putString(MUTILATION_TARGET, target.getUUID().toString());
            tag.putInt(MUTILATION_HITS, 4);
            tag.putLong(MUTILATION_NEXT, player.level().getGameTime());
            tag.putFloat(MUTILATION_DAMAGE, definition().scaling().apply(player, 3.2D));
            tag.putLong(MUTILATION_LOCK, player.level().getGameTime() + 14L);
            player.setDeltaMovement(Vec3.ZERO);
            context.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.9F, 1.45F);
            AbilityEffects.particles(context.level(), target.position().add(0, 1, 0), ParticleTypes.PORTAL, 18, 0.4D);
            return AbilityResult.success("Four-cut sequence engaged.");
        }

        @Override public void tick(ServerPlayer player) {
            CompoundTag tag = HunterData.mutable(player);
            if (tag.getInt(MUTILATION_HITS) <= 0 || tag.getString(MUTILATION_TARGET).isBlank()) return;
            long now = player.level().getGameTime();
            if (now < tag.getLong(MUTILATION_LOCK)) {
                Vec3 movement = player.getDeltaMovement();
                AbilityEffects.setClampedVelocity(player,
                        new Vec3(movement.x * 0.18D, movement.y, movement.z * 0.18D), 0.18D, 0.55D);
            }
            if (now < tag.getLong(MUTILATION_NEXT)) return;
            LivingEntity target = livingByUuid(player, tag.getString(MUTILATION_TARGET));
            if (target == null || !AbilityTargeting.isValidTarget(player, target)
                    || player.distanceToSqr(target) > 7.5D * 7.5D || !AbilityEffects.hasDagger(player)) {
                clearMutilation(player);
                return;
            }
            float damage = Math.max(0.5F, tag.getFloat(MUTILATION_DAMAGE));
            int remaining = tag.getInt(MUTILATION_HITS);
            if (!AbilityEffects.dealAbilityDamage(player, target, damage, "mutilation")) {
                clearMutilation(player);
                return;
            }
            player.swing(remaining % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
            AbilityEffects.particles(player.serverLevel(), target.position().add(0, target.getBbHeight() * 0.55D, 0),
                    remaining == 1 ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK,
                    remaining == 1 ? 18 : 7, 0.32D);
            player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.7F, 1.0F + (4 - remaining) * 0.16F);
            remaining--;
            if (remaining <= 0 || !target.isAlive()) {
                AbilityEffects.particles(player.serverLevel(), target.position().add(0, 1, 0), ParticleTypes.CRIT, 24, 0.55D);
                clearMutilation(player);
            } else {
                tag.putInt(MUTILATION_HITS, remaining);
                tag.putLong(MUTILATION_NEXT, now + 3L);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            clearMutilation(player);
        }
    }

    private static final class DaggerRushAbility extends BaseAbility {
        private DaggerRushAbility() {
            super(spec("dagger_rush", "Dagger Rush", "Rush to a visible target and strike on arrival.", "combat",
                    AbilityUnlock.skillOrLevel("dagger_rush", 25), 58, 260, 14.0D,
                    new AbilityScaling(0.28D, 0.22D, 0, 0, 0.04D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Dagger Rush requires a dagger.");
            LivingEntity target = AbilityTargeting.livingRayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No visible target within 14 blocks.");
            Vec3 destination = AbilityEffects.safeNearTarget(player, target, 1.45D);
            if (destination == null) return AbilityResult.failure("The target has no safe arrival position.");
            Vec3 origin = player.position();
            if (!AbilityEffects.moveIfClear(player, destination)) return AbilityResult.failure("Dagger Rush path is obstructed.");
            AbilityEffects.particles(context.level(), origin.add(0, 0.8D, 0), ParticleTypes.REVERSE_PORTAL, 22, 0.45D);
            AbilityEffects.particles(context.level(), destination.add(0, 0.8D, 0), ParticleTypes.SWEEP_ATTACK, 12, 0.45D);
            float damage = definition().scaling().apply(player, AbilityEffects.isBoss(target) ? 5.0D : 7.0D);
            if (!AbilityEffects.dealAbilityDamage(player, target, damage, "dagger_rush")) {
                return AbilityResult.failure("Dagger Rush reached the target but the strike was resisted.");
            }
            player.swing(InteractionHand.MAIN_HAND, true);
            context.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1.0F, 1.25F);
            return AbilityResult.success("Gap closed and target struck.");
        }
    }

    private static final class BloodlustAbility extends BaseAbility {
        private BloodlustAbility() {
            super(spec("bloodlust", "Bloodlust", "Trade defense for a temporary surge of offensive power.", "combat",
                    AbilityUnlock.skillOrLevel("bloodlust", 10), 38, 420, 0.0D,
                    new AbilityScaling(0.08D, 0.04D, 0, 0, 0)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            int amplifier = Math.min(2, HunterData.getStat(player, "strength") / 70);
            int duration = 160;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amplifier, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false, true));
            HunterData.mutable(player).putLong(BLOODLUST_UNTIL, player.level().getGameTime() + duration);
            context.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_AMBIENT,
                    SoundSource.PLAYERS, 0.9F, 1.35F);
            AbilityEffects.ring(context.level(), player.position().add(0, 0.15D, 0), ParticleTypes.DAMAGE_INDICATOR, 2.2D, 30);
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.CRIMSON_SPORE, 40, 0.8D);
            return AbilityResult.success("Offense increased for 8 seconds; incoming damage is 20% higher.");
        }

        @Override public void tick(ServerPlayer player) {
            CompoundTag tag = HunterData.mutable(player);
            long until = tag.getLong(BLOODLUST_UNTIL);
            if (until <= 0L) return;
            if (player.level().getGameTime() >= until || !player.isAlive()) {
                tag.putLong(BLOODLUST_UNTIL, 0L);
                return;
            }
            if (player.tickCount % 10 == 0) {
                AbilityEffects.ring(player.serverLevel(), player.position().add(0, 0.12D, 0),
                        ParticleTypes.DAMAGE_INDICATOR, 1.4D, 14);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            HunterData.mutable(player).putLong(BLOODLUST_UNTIL, 0L);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
        }
    }

    private static final class AreaSlashAbility extends BaseAbility {
        private AreaSlashAbility() {
            super(spec("area_slash", "Area Slash", "Sweep daggers through a forward cone and strike multiple targets.", "combat",
                    AbilityUnlock.skillOrLevel("dagger_rush", 25), 65, 360, 6.0D,
                    new AbilityScaling(0.25D, 0.25D, 0, 0, 0.04D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Area Slash requires a dagger.");
            List<LivingEntity> targets = AbilityTargeting.nearbyVisible(player, definition().maximumRange(),
                    entity -> AbilityTargeting.inFrontCone(player, entity, 0.20D));
            float damage = definition().scaling().apply(player, 7.0D);
            int hits = 0;
            for (LivingEntity target : targets) {
                if (AbilityEffects.dealAbilityDamage(player, target, damage, "area_slash")) {
                    hits++;
                    AbilityEffects.particles(context.level(), target.position().add(0, target.getBbHeight() * 0.5D, 0),
                            ParticleTypes.SWEEP_ATTACK, 8, 0.35D);
                }
            }
            if (hits == 0) return AbilityResult.failure("Area Slash found no visible targets.");
            player.swing(InteractionHand.MAIN_HAND, true);
            return AbilityResult.success("Area Slash struck " + hits + " target(s).");
        }
    }

    private static final class StealthAbility extends BaseAbility {
        private StealthAbility() {
            super(spec("stealth", "Stealth", "Fade from sight and reduce hostile detection for ten seconds.", "utility",
                    AbilityUnlock.skillOrLevel("stealth", 5), 25, 320, 0.0D,
                    new AbilityScaling(0, 0.01D, 0, 0.01D, 0.01D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            HunterData.beginStealth(player, 200);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false, true));
            context.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.8F, 0.75F);
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 30, 0.65D);
            return AbilityResult.success("Stealth active for 10 seconds; attacks and damage break it.");
        }

        @Override public void tick(ServerPlayer player) {
            if (!HunterData.isStealthed(player)) return;
            long now = player.level().getGameTime();
            if (now >= HunterData.stealthUntil(player) || !player.isAlive()) {
                exitStealth(player, true);
                return;
            }
            if (player.tickCount % 20 == 0) {
                for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class,
                        player.getBoundingBox().inflate(12.0D), candidate -> candidate.getTarget() == player)) {
                    if (mob.distanceToSqr(player) > 4.0D * 4.0D && !mob.hasLineOfSight(player)) mob.setTarget(null);
                }
                AbilityEffects.particles(player.serverLevel(), player.position().add(0, 0.8D, 0),
                        ParticleTypes.ASH, 4, 0.25D);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            if (HunterData.isStealthed(player)) exitStealth(player, false);
        }
    }

    private enum AuthorityMode { PULL, PUSH, HOLD, THROW, DASH, FLIGHT }

    private static final class AuthorityAbility extends BaseAbility {
        private final AuthorityMode mode;
        private final boolean stateOwner;

        private AuthorityAbility(String id, AuthorityMode mode, int mana, int cooldown, double range, boolean stateOwner) {
            super(spec(id, authorityName(mode), "Server-controlled telekinetic " + mode.name().toLowerCase() + ".", "utility",
                    AbilityUnlock.skillOrLevel("rulers_authority", 30), mana, cooldown, range,
                    new AbilityScaling(0.02D, 0.005D, 0, 0.01D, 0.01D)));
            this.mode = mode;
            this.stateOwner = stateOwner;
        }

        @Override public AbilityResult activate(AbilityContext context) {
            return switch (mode) {
                case PULL -> impulse(context, true);
                case PUSH -> impulse(context, false);
                case HOLD -> hold(context);
                case THROW -> throwTarget(context);
                case DASH -> authorityDash(context);
                case FLIGHT -> flight(context);
            };
        }

        @Override public void tick(ServerPlayer player) {
            if (!stateOwner) return;
            tickHold(player);
            tickFlight(player);
        }

        @Override public void cancel(ServerPlayer player) {
            if (!stateOwner) return;
            releaseHeld(player, true);
            revokeFlight(player);
        }

        private AbilityResult impulse(AbilityContext context, boolean pull) {
            ServerPlayer player = context.player();
            Entity target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No visible Ruler's Authority target.");
            Vec3 direction = player.getLookAngle().normalize();
            double baseForce = Math.min(2.5D, 0.75D + HunterData.getStat(player, "strength") * 0.015D
                    + HunterData.getStat(player, "intelligence") * 0.004D);
            double resistance = AbilityEffects.isBoss(target) ? 0.25D : 1.0D;
            double force = baseForce * resistance;
            Vec3 movement;
            if (target instanceof ItemEntity item) {
                movement = pull ? player.getEyePosition().subtract(item.position()).normalize().scale(Math.max(0.75D, force))
                        : direction.scale(Math.max(0.75D, force));
                item.setPickUpDelay(0);
            } else if (pull) {
                movement = player.getEyePosition().subtract(target.position()).normalize().scale(force);
                movement = new Vec3(movement.x, Math.max(0.10D, movement.y), movement.z);
            } else {
                movement = new Vec3(direction.x * force, 0.22D + direction.y * force, direction.z * force);
            }
            AbilityEffects.setClampedVelocity(target, movement, 2.5D, 0.9D);
            AbilityEffects.particles(context.level(), target.position().add(0, target.getBbHeight() * 0.5D, 0),
                    ParticleTypes.DRAGON_BREATH, 28, 0.55D);
            context.level().playSound(null, target.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.75F, pull ? 0.85F : 1.25F);
            return AbilityResult.success("Ruler's Authority " + mode.name().toLowerCase()
                    + (resistance < 1.0D ? " partially moved the resistant target." : " succeeded."));
        }

        private AbilityResult hold(AbilityContext context) {
            ServerPlayer player = context.player();
            Entity target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (!(target instanceof LivingEntity living)) return AbilityResult.failure("No valid living target to hold.");
            if (AbilityEffects.isBoss(target)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                AbilityEffects.particles(context.level(), living.position().add(0, 1, 0), ParticleTypes.DRAGON_BREATH, 18, 0.45D);
                return AbilityResult.success("The boss resisted suspension but was slowed.");
            }
            releaseHeld(player, true);
            CompoundTag tag = HunterData.mutable(player);
            tag.putString("authority_held", target.getUUID().toString());
            long duration = target instanceof ServerPlayer ? 40L : 100L;
            tag.putLong("authority_hold_until", player.level().getGameTime() + duration);
            target.setNoGravity(true);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            return AbilityResult.success("Target suspended for up to " + (duration / 20L) + " seconds.");
        }

        private AbilityResult throwTarget(AbilityContext context) {
            ServerPlayer player = context.player();
            Entity target = heldEntity(player);
            if (target == null) target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No held or visible entity to throw.");
            boolean resistant = AbilityEffects.isBoss(target);
            double forceValue = resistant ? 0.7D : Math.min(2.8D,
                    1.7D + HunterData.getStat(player, "strength") * 0.01D);
            Vec3 force = player.getLookAngle().normalize().scale(forceValue).add(0.0D, resistant ? 0.08D : 0.22D, 0.0D);
            if (target instanceof LivingEntity living) {
                AbilityEffects.dealAbilityDamage(player, living,
                        resistant ? 3.0F : 6.0F + HunterData.getStat(player, "strength") * 0.10F,
                        "rulers_authority_throw");
            }
            if (target == heldEntity(player)) releaseHeld(player, false);
            AbilityEffects.setClampedVelocity(target, force, 2.8D, 0.9D);
            return AbilityResult.success(resistant ? "The target resisted most of the throw." : "Target thrown.");
        }

        private AbilityResult authorityDash(AbilityContext context) {
            ServerPlayer player = context.player();
            Vec3 look = player.getLookAngle().normalize();
            AbilityEffects.setClampedVelocity(player,
                    new Vec3(look.x * 1.8D, Math.max(0.18D, look.y * 1.2D + 0.18D), look.z * 1.8D),
                    1.8D, 0.65D);
            player.fallDistance = 0.0F;
            AbilityEffects.particles(context.level(), player.position().add(0, 0.8D, 0), ParticleTypes.END_ROD, 24, 0.35D);
            return AbilityResult.success("Ruler's Authority dash activated.");
        }

        private AbilityResult flight(AbilityContext context) {
            ServerPlayer player = context.player();
            CompoundTag tag = HunterData.mutable(player);
            tag.putLong("authority_flight_until", player.level().getGameTime() + 160L);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.fallDistance = 0.0F;
            return AbilityResult.success("Controlled flight enabled for 8 seconds.");
        }

        private static String authorityName(AuthorityMode mode) {
            return "Ruler's Authority: " + mode.name().charAt(0) + mode.name().substring(1).toLowerCase();
        }
    }

    private static final class EnhancedSensesAbility extends BaseAbility {
        private EnhancedSensesAbility() {
            super(spec("enhanced_senses", "Enhanced Senses", "Reveal nearby threats and gain night vision.", "utility",
                    AbilityUnlock.skillOrLevel("", 12), 22, 300, 18.0D,
                    new AbilityScaling(0, 0, 0, 0.02D, 0.08D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false, true));
            List<LivingEntity> targets = AbilityTargeting.nearbyLiving(player, definition().maximumRange(), entity -> entity instanceof Monster);
            for (LivingEntity target : targets) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.ENCHANT, 36, 1.0D);
            return AbilityResult.success("Enhanced Senses revealed " + targets.size() + " threat(s).");
        }
    }

    private static final class MonarchDomainAbility extends BaseAbility {
        private MonarchDomainAbility() {
            super(spec("monarch_domain", "Monarch's Domain", "Toggle the mana-draining domain used by the shadow system.", "monarch",
                    AbilityUnlock.skill("monarch_domain"), 30, 100, 0.0D,
                    new AbilityScaling(0, 0, 0, 0.03D, 0.02D)));
        }

        @Override public int manaCost(ServerPlayer player) {
            return HunterData.domainActive(player) ? 0 : definition().manaCost();
        }

        @Override public AbilityResult activate(AbilityContext context) {
            HunterData.toggleDomain(context.player());
            boolean active = HunterData.domainActive(context.player());
            AbilityEffects.particles(context.level(), context.player().position().add(0, 1, 0),
                    ParticleTypes.REVERSE_PORTAL, active ? 64 : 24, active ? 2.5D : 1.2D);
            context.level().playSound(null, context.player().blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.85F, active ? 0.6F : 1.3F);
            return AbilityResult.success("Monarch's Domain " + (active ? "enabled" : "disabled") + ".");
        }

        @Override public void tick(ServerPlayer player) {
            if (HunterData.domainActive(player) && player.tickCount % 10 == 0) {
                AbilityEffects.ring(player.serverLevel(), player.position().add(0, 0.12D, 0),
                        ParticleTypes.REVERSE_PORTAL, 4.0D, 24);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            // Domain is a live, mana-draining combat stance rather than persistent
            // progression. Never restore it after death, logout, dimension travel,
            // or a server restart.
            HunterData.mutable(player).putBoolean("monarch_domain", false);
        }
    }

    private enum ShadowHook { EXCHANGE, EXTRACTION, SUMMONING }

    private static final class ShadowHookAbility extends BaseAbility {
        private final ShadowHook hook;

        private ShadowHookAbility(String id, String name, int mana, int cooldown, ShadowHook hook) {
            super(spec(id, name, "Validated integration point for the shadow system.", "monarch",
                    AbilityUnlock.skill(id.equals("shadow_summoning") ? "shadow_preservation" : id),
                    mana, cooldown, 24.0D, AbilityScaling.NONE));
            this.hook = hook;
        }

        @Override public AbilityResult activate(AbilityContext context) {
            return switch (hook) {
                case EXCHANGE -> AbilityIntegrationHooks.shadows().exchange(context.player());
                case EXTRACTION -> AbilityIntegrationHooks.shadows().extract(context.player());
                case SUMMONING -> AbilityIntegrationHooks.shadows().summon(context.player());
            };
        }
    }

    private static final class DragonsFearAbility extends BaseAbility {
        private DragonsFearAbility() {
            super(spec("dragons_fear", "Dragon's Fear", "Release an expanding terror wave that disrupts hostile enemies.", "combat",
                    AbilityUnlock.skillOrLevel("dragons_fear", 35), 110, 800, 18.0D,
                    new AbilityScaling(0, 0, 0, 0.03D, 0.03D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            long cast = player.level().getGameTime();
            CompoundTag tag = HunterData.mutable(player);
            tag.putLong(FEAR_CAST, cast);
            tag.putLong(FEAR_UNTIL, cast + 12L);
            context.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                    SoundSource.PLAYERS, 1.7F, 0.72F);
            AbilityEffects.particles(context.level(), player.position().add(0, 1.0D, 0), ParticleTypes.SONIC_BOOM, 4, 0.35D);
            return AbilityResult.success("An 18-block fear wave is expanding.");
        }

        @Override public void tick(ServerPlayer player) {
            CompoundTag tag = HunterData.mutable(player);
            long until = tag.getLong(FEAR_UNTIL);
            if (until <= 0L) return;
            long now = player.level().getGameTime();
            long cast = tag.getLong(FEAR_CAST);
            if (now > until || !player.isAlive()) {
                tag.putLong(FEAR_UNTIL, 0L);
                return;
            }
            if ((now - cast) % 2L != 0L) return;
            double radius = Math.min(definition().maximumRange(), 2.0D + (now - cast) * 1.5D);
            AbilityEffects.ring(player.serverLevel(), player.position().add(0, 0.2D, 0),
                    ParticleTypes.DRAGON_BREATH, radius, 36);
            for (LivingEntity living : AbilityTargeting.nearbyVisible(player, radius, entity -> entity instanceof Monster)) {
                CompoundTag targetTag = living.getPersistentData();
                if (targetTag.hasUUID(FEAR_MARK_OWNER)
                        && player.getUUID().equals(targetTag.getUUID(FEAR_MARK_OWNER))
                        && targetTag.getLong(FEAR_MARK_CAST) == cast) continue;
                targetTag.putUUID(FEAR_MARK_OWNER, player.getUUID());
                targetTag.putLong(FEAR_MARK_CAST, cast);
                applyFear(player, living);
            }
        }

        @Override public void cancel(ServerPlayer player) {
            HunterData.mutable(player).putLong(FEAR_UNTIL, 0L);
        }
    }

    private static void applyFear(ServerPlayer player, LivingEntity living) {
        boolean boss = AbilityEffects.isBoss(living);
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, boss ? 80 : 140, boss ? 0 : 2));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? 50 : 100, boss ? 0 : 2));
        if (!boss && living instanceof Mob mob) {
            mob.setTarget(null);
            Vec3 away = living.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-6D) away = new Vec3(1.0D, 0.0D, 0.0D);
            Vec3 flee = living.position().add(away.normalize().scale(8.0D));
            mob.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.25D);
            AbilityEffects.setClampedVelocity(living, away.normalize().scale(0.75D).add(0.0D, 0.12D, 0.0D), 0.75D, 0.3D);
        }
        AbilityEffects.particles(player.serverLevel(), living.position().add(0, living.getBbHeight() * 0.65D, 0),
                boss ? ParticleTypes.SMOKE : ParticleTypes.SOUL, boss ? 8 : 14, 0.45D);
    }

    private static LivingEntity livingByUuid(ServerPlayer player, String raw) {
        try {
            Entity entity = player.serverLevel().getEntity(UUID.fromString(raw));
            return entity instanceof LivingEntity living ? living : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void clearMutilation(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        tag.putString(MUTILATION_TARGET, "");
        tag.putInt(MUTILATION_HITS, 0);
        tag.putLong(MUTILATION_NEXT, 0L);
        tag.putFloat(MUTILATION_DAMAGE, 0.0F);
        tag.putLong(MUTILATION_LOCK, 0L);
    }

    private static void exitStealth(ServerPlayer player, boolean expired) {
        HunterData.endStealth(player);
        AbilityEffects.particles(player.serverLevel(), player.position().add(0, 1, 0), ParticleTypes.SMOKE, 18, 0.45D);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.PLAYERS, 0.55F, 1.15F);
        if (expired) player.displayClientMessage(Component.literal("[STEALTH ENDED]"), true);
    }

    private static Entity heldEntity(ServerPlayer player) {
        String raw = HunterData.mutable(player).getString("authority_held");
        if (raw.isBlank()) return null;
        try {
            return player.serverLevel().getEntity(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void tickHold(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getString("authority_held").isBlank()) return;
        Entity held = heldEntity(player);
        if (held == null || !held.isAlive() || held.level() != player.level()
                || player.level().getGameTime() >= tag.getLong("authority_hold_until")
                || !HunterData.hasSkill(player, "rulers_authority")
                || player.distanceToSqr(held) > 24.0D * 24.0D
                || !player.hasLineOfSight(held)) {
            releaseHeld(player, true);
            return;
        }
        if (player.tickCount % 20 == 0 && !HunterData.spendMana(player, 3)) {
            releaseHeld(player, true);
            return;
        }
        Vec3 position = player.getEyePosition().add(player.getLookAngle().normalize().scale(3.2D));
        if (!player.serverLevel().hasChunkAt(BlockPos.containing(position))) {
            releaseHeld(player, true);
            return;
        }
        Vec3 destination = position.subtract(0.0D, held.getBbHeight() * 0.45D, 0.0D);
        AABB moved = held.getBoundingBox().move(destination.subtract(held.position()));
        if (!player.serverLevel().noCollision(held, moved)) {
            releaseHeld(player, true);
            return;
        }
        held.teleportTo(destination.x, destination.y, destination.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.setNoGravity(true);
        held.fallDistance = 0.0F;
        held.hurtMarked = true;
        if (player.tickCount % 5 == 0) {
            AbilityEffects.particles(player.serverLevel(), held.position().add(0, held.getBbHeight() * 0.5D, 0),
                    ParticleTypes.DRAGON_BREATH, 5, 0.25D);
        }
    }

    private static void tickFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        long until = tag.getLong("authority_flight_until");
        if (until <= 0L) return;
        if (player.level().getGameTime() >= until || !HunterData.hasSkill(player, "rulers_authority") || !player.isAlive()) {
            revokeFlight(player);
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
        if (player.tickCount % 5 == 0) {
            AbilityEffects.particles(player.serverLevel(), player.position().add(0, 0.2D, 0), ParticleTypes.END_ROD, 3, 0.2D);
        }
    }

    private static void releaseHeld(ServerPlayer player, boolean discardVelocity) {
        Entity held = heldEntity(player);
        if (held != null) {
            held.setNoGravity(false);
            held.fallDistance = 0.0F;
            if (discardVelocity) held.setDeltaMovement(Vec3.ZERO);
            held.hurtMarked = true;
        }
        CompoundTag tag = HunterData.mutable(player);
        tag.putString("authority_held", "");
        tag.putLong("authority_hold_until", 0L);
    }

    private static void revokeFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        if (tag.getLong("authority_flight_until") <= 0L) return;
        tag.putLong("authority_flight_until", 0L);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
    }

    private StandardAbilities() {
    }
}
