package com.tre.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tre.sololeveling.config.ModConfigs;
import com.tre.sololeveling.data.HunterData;
import com.tre.sololeveling.gameplay.ability.AbilityService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

/** Administrative shortcut for full-power gameplay and ability testing. */
public final class GodPowersCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("sl"));
        dispatcher.register(root("sololeveling"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .then(Commands.literal("givemegodpowers")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> grant(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> grant(context.getSource(), EntityArgument.getPlayer(context, "player")))));
    }

    private static int grant(CommandSourceStack source, ServerPlayer player) {
        HunterData.awaken(player);
        HunterData.setLevel(player, ModConfigs.MAX_HUNTER_LEVEL.get());

        CompoundTag tag = HunterData.mutable(player);
        int maximumStat = ModConfigs.MAX_PRIMARY_STAT.get();
        for (String stat : HunterData.PRIMARY_STATS) tag.putInt(stat, maximumStat);
        for (String skill : HunterData.SKILLS) tag.putBoolean("skill_" + skill, true);
        for (String abilityId : AbilityService.ids()) tag.putBoolean("skill_" + abilityId, true);

        tag.putBoolean("god_powers", true);
        tag.putBoolean("black_heart", true);
        tag.putInt("level", ModConfigs.MAX_HUNTER_LEVEL.get());
        tag.putInt("rewarded_level", ModConfigs.MAX_HUNTER_LEVEL.get());
        tag.putInt("xp", 0);
        tag.putInt("stat_points", Integer.MAX_VALUE);
        tag.putInt("gold", Integer.MAX_VALUE);
        tag.putInt("shadow_capacity_bonus", 1_000);
        tag.putInt("skill_evolution_tokens", 99);
        tag.putInt("progression_stage", 5);
        tag.putString("active_main_quest", "completed");
        tag.putString("job", "Shadow Monarch");
        tag.putString("title", "The Shadow Monarch");

        for (String key : new ArrayList<>(tag.getAllKeys())) {
            if (key.startsWith("cooldown_")) tag.remove(key);
        }

        HunterData.setHunterRank(player, "Shadow Monarch");
        HunterData.recalculateAttributes(player);
        tag.putInt("mana", HunterData.getMaxMana(player));
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        HunterData.sync(player);

        source.sendSuccess(() -> Component.literal("God powers granted to " + player.getScoreboardName()
                + ": maximum level and stats, every ability, 99 evolution tokens, full mana, and 2,147,483,647 gold.")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
        return 1;
    }

    private GodPowersCommand() {
    }
}
