package com.tre.sololeveling.quest;

import com.tre.sololeveling.data.HunterData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class QuestReward {
    public static final QuestReward NONE = builder().build();

    private final int xp;
    private final int gold;
    private final int statPoints;
    private final List<ItemGrant> items;
    private final List<String> skills;

    private QuestReward(Builder builder) {
        this.xp = Math.max(0, builder.xp);
        this.gold = Math.max(0, builder.gold);
        this.statPoints = Math.max(0, builder.statPoints);
        this.items = List.copyOf(builder.items);
        this.skills = List.copyOf(builder.skills);
    }

    public void apply(ServerPlayer player) {
        if (xp > 0) HunterData.addXp(player, xp);
        if (gold > 0) HunterData.addGold(player, gold);
        if (statPoints > 0) HunterData.addStatPoints(player, statPoints);
        for (String skill : skills) HunterData.unlockSkill(player, skill);
        for (ItemGrant grant : items) {
            Item item = ForgeRegistries.ITEMS.getValue(grant.itemId());
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, grant.count());
            if (!HunterData.storeSystemItem(player, stack.copy())) {
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
    }

    public int xp() { return xp; }
    public int gold() { return gold; }
    public int statPoints() { return statPoints; }
    public List<ItemGrant> items() { return items; }
    public List<String> skills() { return skills; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int xp;
        private int gold;
        private int statPoints;
        private final List<ItemGrant> items = new ArrayList<>();
        private final List<String> skills = new ArrayList<>();

        public Builder xp(int value) { xp = value; return this; }
        public Builder gold(int value) { gold = value; return this; }
        public Builder statPoints(int value) { statPoints = value; return this; }
        public Builder item(String id, int count) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null && count > 0) items.add(new ItemGrant(parsed, count));
            return this;
        }
        public Builder skill(String id) {
            if (id != null && !id.isBlank()) skills.add(id.toLowerCase(java.util.Locale.ROOT));
            return this;
        }
        public QuestReward build() { return new QuestReward(this); }
    }

    public record ItemGrant(ResourceLocation itemId, int count) {
        public ItemGrant { count = Math.max(1, count); }
    }
}
