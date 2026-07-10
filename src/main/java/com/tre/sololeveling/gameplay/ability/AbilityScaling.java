package com.tre.sololeveling.gameplay.ability;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.server.level.ServerPlayer;

/** Coefficients used by damage and utility abilities. */
public record AbilityScaling(double strength, double agility, double stamina, double intelligence, double sense) {
    public static final AbilityScaling NONE = new AbilityScaling(0, 0, 0, 0, 0);

    public float apply(ServerPlayer player, double baseValue) {
        double value = baseValue
                + HunterData.getStat(player, "strength") * strength
                + HunterData.getStat(player, "agility") * agility
                + HunterData.getStat(player, "stamina") * stamina
                + HunterData.getStat(player, "intelligence") * intelligence
                + HunterData.getStat(player, "sense") * sense;
        return (float)Math.max(0.0D, Math.min(Float.MAX_VALUE, value));
    }

    public String summary() {
        return "STR " + strength + ", AGI " + agility + ", STA " + stamina + ", INT " + intelligence + ", SEN " + sense;
    }
}
