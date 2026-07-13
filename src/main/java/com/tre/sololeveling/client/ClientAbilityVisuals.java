package com.tre.sololeveling.client;

import com.tre.sololeveling.network.packet.AbilityVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-only animation/effect playback for trusted server visual events. */
public final class ClientAbilityVisuals {
    private record ActiveVisual(String abilityId, String animationId, long expiresAt) {}
    private static final Map<Integer, ActiveVisual> ACTIVE = new HashMap<>();

    public static void accept(AbilityVisualPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity caster = minecraft.level.getEntity(packet.casterId());
        Entity target = packet.targetId() < 0 ? null : minecraft.level.getEntity(packet.targetId());
        if (caster == null) return;

        if (packet.stage() == AbilityVisualPacket.Stage.INTERRUPTED
                || packet.stage() == AbilityVisualPacket.Stage.FAILED) {
            ACTIVE.remove(packet.casterId());
            burst(caster.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 10, 0.32D);
            return;
        }

        long now = minecraft.level.getGameTime();
        ACTIVE.put(packet.casterId(), new ActiveVisual(packet.abilityId(), packet.animationId(),
                now + Math.max(1, packet.durationTicks())));
        ParticleOptions startup = packet.abilityId().contains("fear") ? ParticleTypes.DRAGON_BREATH
                : packet.abilityId().contains("shadow") || packet.abilityId().contains("monarch")
                ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD;
        burst(caster.position().add(0.0D, 1.0D, 0.0D), startup,
                packet.stage() == AbilityVisualPacket.Stage.STARTUP ? 12 : 18, 0.42D);
        if (target != null) burst(target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D),
                ParticleTypes.ENCHANT, 10, 0.28D);
        if (packet.stage() == AbilityVisualPacket.Stage.ACTIVE && caster instanceof LivingEntity living
                && (packet.animationId().contains("dagger") || packet.animationId().contains("slash"))) {
            living.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) return;
        long now = minecraft.level.getGameTime();
        Iterator<Map.Entry<Integer, ActiveVisual>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ActiveVisual> entry = iterator.next();
            ActiveVisual visual = entry.getValue();
            if (now >= visual.expiresAt()) {
                iterator.remove();
                continue;
            }
            Entity caster = minecraft.level.getEntity(entry.getKey());
            if (caster == null) {
                iterator.remove();
                continue;
            }
            if (now % 2L != 0L) continue;
            ParticleOptions particle = visual.abilityId().equals("quicksilver") ? ParticleTypes.CLOUD
                    : visual.abilityId().equals("stealth") ? ParticleTypes.ASH
                    : visual.abilityId().equals("monarch_domain") ? ParticleTypes.REVERSE_PORTAL : null;
            if (particle != null) burst(caster.position().add(0.0D, 0.35D, 0.0D), particle, 2, 0.10D);
        }
    }

    private static void burst(Vec3 center, ParticleOptions particle, int count, double spread) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        for (int index = 0; index < Math.min(24, Math.max(0, count)); index++) {
            double x = (minecraft.level.random.nextDouble() - 0.5D) * spread;
            double y = (minecraft.level.random.nextDouble() - 0.5D) * spread;
            double z = (minecraft.level.random.nextDouble() - 0.5D) * spread;
            minecraft.level.addParticle(particle, center.x + x, center.y + y, center.z + z,
                    x * 0.05D, y * 0.05D, z * 0.05D);
        }
    }

    private ClientAbilityVisuals() {}
}
