package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Built-in abilities. Each implementation contains only its unique effect logic. */
public final class StandardAbilities {
    public static void registerAll(AbilityRegistry registry) {
        registry
                .register(new DashAbility())
                .register(new QuicksilverAbility())
                .register(new ShadowStepAbility())
                .register(new DaggerMasteryAbility())
                .register(new MutilationAbility())
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
                .alias("dagger_rush", "area_slash")
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

    private static AbilityDefinition definition(String id, String name, String description, String category,
                                                AbilityUnlock unlock, int mana, int cooldown, double range,
                                                AbilityScaling scaling) {
        return new AbilityDefinition(id, name, description, category, unlock, mana, cooldown, range, scaling);
    }

    private static final class DashAbility extends BaseAbility {
        private DashAbility() {
            super(definition("dash", "Dash", "A short agility-scaled burst in the facing direction.", "movement",
                    AbilityUnlock.awakened(), 12, 50, 0.0D, new AbilityScaling(0, 0.01D, 0, 0, 0)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            double force = Math.min(2.4D, 1.15D + HunterData.getStat(player, "agility") * 0.008D);
            Vec3 look = player.getLookAngle().normalize();
            player.setDeltaMovement(look.x * force, Math.max(0.12D, look.y * force + 0.08D), look.z * force);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            AbilityEffects.particles(context.level(), player.position().add(0, 0.8D, 0), ParticleTypes.CLOUD, 22, 0.3D);
            return AbilityResult.success("Dash activated.");
        }
    }

    private static final class QuicksilverAbility extends BaseAbility {
        private QuicksilverAbility() {
            super(definition("quicksilver", "Quicksilver", "Temporarily heightens movement speed and jumping.", "movement",
                    AbilityUnlock.skillOrLevel("quicksilver", 15), 20, 240, 0.0D, new AbilityScaling(0, 0.02D, 0, 0, 0)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            int amplifier = Math.min(4, 1 + HunterData.getStat(player, "agility") / 50);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, amplifier, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, Math.min(2, amplifier / 2), false, false, true));
            AbilityEffects.particles(context.level(), player.position().add(0, 0.5D, 0), ParticleTypes.END_ROD, 30, 0.5D);
            return AbilityResult.success("Quicksilver active for 10 seconds.");
        }
    }

    private static final class ShadowStepAbility extends BaseAbility {
        private ShadowStepAbility() {
            super(definition("shadow_step", "Shadow Step", "Teleport behind a targeted living enemy.", "movement",
                    AbilityUnlock.skill("shadow_exchange"), 55, 200, 18.0D, new AbilityScaling(0, 0.01D, 0, 0.01D, 0.01D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            LivingEntity target = AbilityTargeting.livingRayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No valid Shadow Step target.");
            Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.6D));
            if (!AbilityEffects.moveIfClear(player, behind)) return AbilityResult.failure("The destination is obstructed or unloaded.");
            AbilityEffects.particles(context.level(), target.position().add(0, 1, 0), ParticleTypes.PORTAL, 42, 0.7D);
            return AbilityResult.success("Shadow Step complete.");
        }
    }

    private static final class DaggerMasteryAbility extends BaseAbility {
        private DaggerMasteryAbility() {
            super(definition("dagger_mastery", "Dagger Mastery", "Assume a focused dagger stance that increases close-combat output.", "combat",
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
            super(definition("mutilation", "Mutilation", "Blink into dagger range and deliver a high-damage strike.", "combat",
                    AbilityUnlock.skillOrLevel("mutilation", 20), 45, 160, 12.0D,
                    new AbilityScaling(0.35D, 0.15D, 0, 0, 0.05D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Mutilation requires a dagger.");
            LivingEntity target = AbilityTargeting.livingRayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No valid Mutilation target.");
            Vec3 destination = target.position().subtract(target.getLookAngle().normalize().scale(1.5D));
            AbilityEffects.moveIfClear(player, destination);
            float damage = definition().scaling().apply(player, 8.0D);
            if (!AbilityEffects.dealDamage(player, target, damage)) return AbilityResult.failure("Mutilation could not damage the target.");
            AbilityEffects.particles(context.level(), target.position().add(0, target.getBbHeight() * 0.5D, 0), ParticleTypes.SWEEP_ATTACK, 14, 0.5D);
            AbilityEffects.particles(context.level(), target.position().add(0, 1, 0), ParticleTypes.PORTAL, 30, 0.8D);
            player.swing(InteractionHand.MAIN_HAND, true);
            return AbilityResult.success("Mutilation dealt " + Math.round(damage) + " damage.");
        }
    }

    private static final class BloodlustAbility extends BaseAbility {
        private BloodlustAbility() {
            super(definition("bloodlust", "Bloodlust", "Overwhelm nearby hostile mobs with fear, weakness, and slowness.", "combat",
                    AbilityUnlock.skillOrLevel("bloodlust", 10), 40, 500, 12.0D,
                    new AbilityScaling(0, 0, 0, 0.02D, 0.02D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            List<LivingEntity> targets = AbilityTargeting.nearbyLiving(player, definition().maximumRange(), entity -> entity instanceof Monster);
            for (LivingEntity living : targets) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                if (living instanceof Mob mob) mob.setTarget(null);
            }
            if (targets.isEmpty()) return AbilityResult.failure("No hostile targets are close enough.");
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 80, 3.0D);
            return AbilityResult.success("Bloodlust affected " + targets.size() + " target(s).");
        }
    }

    private static final class AreaSlashAbility extends BaseAbility {
        private AreaSlashAbility() {
            super(definition("area_slash", "Area Slash", "Sweep daggers through a forward cone and strike multiple targets.", "combat",
                    AbilityUnlock.skillOrLevel("dagger_rush", 25), 65, 360, 6.0D,
                    new AbilityScaling(0.25D, 0.25D, 0, 0, 0.04D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            if (!AbilityEffects.hasDagger(player)) return AbilityResult.failure("Area Slash requires a dagger.");
            List<LivingEntity> targets = AbilityTargeting.nearbyLiving(player, definition().maximumRange(),
                    entity -> AbilityTargeting.inFrontCone(player, entity, 0.20D));
            float damage = definition().scaling().apply(player, 7.0D);
            int hits = 0;
            for (LivingEntity target : targets) {
                if (AbilityEffects.dealDamage(player, target, damage)) {
                    hits++;
                    AbilityEffects.particles(context.level(), target.position().add(0, target.getBbHeight() * 0.5D, 0), ParticleTypes.SWEEP_ATTACK, 8, 0.35D);
                }
            }
            if (hits == 0) return AbilityResult.failure("Area Slash found no valid targets.");
            player.swing(InteractionHand.MAIN_HAND, true);
            return AbilityResult.success("Area Slash struck " + hits + " target(s).");
        }
    }

    private static final class StealthAbility extends BaseAbility {
        private StealthAbility() {
            super(definition("stealth", "Stealth", "Become invisible and slightly faster for fifteen seconds.", "utility",
                    AbilityUnlock.skillOrLevel("stealth", 5), 25, 400, 0.0D,
                    new AbilityScaling(0, 0.01D, 0, 0.01D, 0.01D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            HunterData.beginStealth(player, 300);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, false, true));
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.6D);
            return AbilityResult.success("Stealth active for 15 seconds.");
        }
    }

    private enum AuthorityMode { PULL, PUSH, HOLD, THROW, DASH, FLIGHT }

    private static final class AuthorityAbility extends BaseAbility {
        private final AuthorityMode mode;
        private final boolean stateOwner;

        private AuthorityAbility(String id, AuthorityMode mode, int mana, int cooldown, double range, boolean stateOwner) {
            super(definition(id, authorityName(mode), "Server-controlled telekinetic " + mode.name().toLowerCase() + ".", "utility",
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
            releaseHeld(player, false);
            revokeFlight(player);
        }

        private AbilityResult impulse(AbilityContext context, boolean pull) {
            ServerPlayer player = context.player();
            Entity target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No valid Ruler's Authority target.");
            Vec3 direction = player.getLookAngle().normalize();
            double force = Math.min(3.0D, 0.8D + HunterData.getStat(player, "strength") * 0.02D + HunterData.getStat(player, "intelligence") * 0.005D);
            if (target instanceof ItemEntity item) {
                Vec3 movement = pull ? player.getEyePosition().subtract(item.position()).normalize().scale(Math.max(0.8D, force)) : direction.scale(force);
                item.setDeltaMovement(movement);
                item.setPickUpDelay(0);
            } else if (pull) {
                Vec3 movement = player.getEyePosition().subtract(target.position()).normalize().scale(force);
                target.setDeltaMovement(movement.x, Math.max(0.15D, movement.y), movement.z);
            } else {
                target.setDeltaMovement(direction.x * force, 0.35D + direction.y * force, direction.z * force);
            }
            target.hurtMarked = true;
            AbilityEffects.particles(context.level(), target.position().add(0, target.getBbHeight() * 0.5D, 0), ParticleTypes.DRAGON_BREATH, 35, 0.7D);
            return AbilityResult.success("Ruler's Authority " + mode.name().toLowerCase() + " succeeded.");
        }

        private AbilityResult hold(AbilityContext context) {
            ServerPlayer player = context.player();
            Entity target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (target == null || target instanceof ItemEntity) return AbilityResult.failure("No valid living target to hold.");
            releaseHeld(player, false);
            CompoundTag tag = HunterData.mutable(player);
            tag.putString("authority_held", target.getUUID().toString());
            tag.putLong("authority_hold_until", player.level().getGameTime() + 120L);
            target.setNoGravity(true);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            return AbilityResult.success("Target held for up to 6 seconds.");
        }

        private AbilityResult throwTarget(AbilityContext context) {
            ServerPlayer player = context.player();
            Entity target = heldEntity(player);
            if (target == null) target = AbilityTargeting.rayTarget(player, definition().maximumRange());
            if (target == null) return AbilityResult.failure("No held or targeted entity to throw.");
            Vec3 force = player.getLookAngle().normalize().scale(3.0D + Math.min(2.0D, HunterData.getStat(player, "strength") * 0.015D));
            if (target instanceof LivingEntity living) AbilityEffects.dealDamage(player, living, 6.0F + HunterData.getStat(player, "strength") * 0.12F);
            if (target == heldEntity(player)) releaseHeld(player, false);
            target.setDeltaMovement(force.x, force.y + 0.25D, force.z);
            target.hurtMarked = true;
            return AbilityResult.success("Target thrown.");
        }

        private AbilityResult authorityDash(AbilityContext context) {
            ServerPlayer player = context.player();
            Vec3 look = player.getLookAngle().normalize();
            player.setDeltaMovement(look.x * 1.8D, Math.max(0.18D, look.y * 1.2D + 0.18D), look.z * 1.8D);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            AbilityEffects.particles(context.level(), player.position().add(0, 0.8D, 0), ParticleTypes.END_ROD, 24, 0.35D);
            return AbilityResult.success("Ruler's Authority dash activated.");
        }

        private AbilityResult flight(AbilityContext context) {
            ServerPlayer player = context.player();
            CompoundTag tag = HunterData.mutable(player);
            tag.putLong("authority_flight_until", player.level().getGameTime() + 200L);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.fallDistance = 0.0F;
            return AbilityResult.success("Controlled flight enabled for 10 seconds.");
        }

        private static String authorityName(AuthorityMode mode) {
            return "Ruler's Authority: " + mode.name().charAt(0) + mode.name().substring(1).toLowerCase();
        }
    }

    private static final class EnhancedSensesAbility extends BaseAbility {
        private EnhancedSensesAbility() {
            super(definition("enhanced_senses", "Enhanced Senses", "Reveal nearby threats and gain night vision.", "utility",
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
            super(definition("monarch_domain", "Monarch's Domain", "Toggle the mana-draining domain used by the shadow system.", "monarch",
                    AbilityUnlock.skill("monarch_domain"), 30, 100, 0.0D,
                    new AbilityScaling(0, 0, 0, 0.03D, 0.02D)));
        }

        @Override public int manaCost(ServerPlayer player) {
            return HunterData.domainActive(player) ? 0 : definition().manaCost();
        }

        @Override public AbilityResult activate(AbilityContext context) {
            HunterData.toggleDomain(context.player());
            boolean active = HunterData.domainActive(context.player());
            AbilityEffects.particles(context.level(), context.player().position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, active ? 90 : 30, 3.0D);
            return AbilityResult.success("Monarch's Domain " + (active ? "enabled" : "disabled") + ".");
        }
    }

    private enum ShadowHook { EXCHANGE, EXTRACTION, SUMMONING }

    private static final class ShadowHookAbility extends BaseAbility {
        private final ShadowHook hook;

        private ShadowHookAbility(String id, String name, int mana, int cooldown, ShadowHook hook) {
            super(definition(id, name, "Validated integration point for the shadow system.", "monarch",
                    AbilityUnlock.skill(id.equals("shadow_summoning") ? "shadow_preservation" : id), mana, cooldown, 24.0D, AbilityScaling.NONE));
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
            super(definition("dragons_fear", "Dragon's Fear", "Terrify and repel nearby hostile mobs.", "combat",
                    AbilityUnlock.skillOrLevel("dragons_fear", 35), 120, 900, 18.0D,
                    new AbilityScaling(0, 0, 0, 0.03D, 0.03D)));
        }

        @Override public AbilityResult activate(AbilityContext context) {
            ServerPlayer player = context.player();
            List<LivingEntity> targets = AbilityTargeting.nearbyLiving(player, definition().maximumRange(), entity -> entity instanceof Monster);
            for (LivingEntity living : targets) {
                if (living instanceof Mob mob) mob.setTarget(null);
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                Vec3 away = living.position().subtract(player.position()).normalize().scale(1.5D);
                living.setDeltaMovement(away.x, 0.2D, away.z);
                living.hurtMarked = true;
            }
            if (targets.isEmpty()) return AbilityResult.failure("No hostile targets were affected.");
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.SONIC_BOOM, 6, 2.0D);
            AbilityEffects.particles(context.level(), player.position().add(0, 1, 0), ParticleTypes.DRAGON_BREATH, 120, 5.0D);
            return AbilityResult.success("Dragon's Fear affected " + targets.size() + " target(s).");
        }
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
                || !HunterData.hasSkill(player, "rulers_authority")) {
            releaseHeld(player, false);
            return;
        }
        Vec3 position = player.getEyePosition().add(player.getLookAngle().normalize().scale(3.2D));
        if (!player.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(position))) {
            releaseHeld(player, false);
            return;
        }
        held.teleportTo(position.x, position.y - held.getBbHeight() * 0.45D, position.z);
        held.setDeltaMovement(Vec3.ZERO);
        held.setNoGravity(true);
        held.fallDistance = 0.0F;
        held.hurtMarked = true;
        if (player.tickCount % 5 == 0) AbilityEffects.particles(player.serverLevel(), held.position().add(0, held.getBbHeight() * 0.5D, 0), ParticleTypes.DRAGON_BREATH, 5, 0.25D);
    }

    private static void tickFlight(ServerPlayer player) {
        CompoundTag tag = HunterData.mutable(player);
        long until = tag.getLong("authority_flight_until");
        if (until <= 0L) return;
        if (player.level().getGameTime() >= until || !HunterData.hasSkill(player, "rulers_authority")) {
            revokeFlight(player);
            return;
        }
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0F;
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
