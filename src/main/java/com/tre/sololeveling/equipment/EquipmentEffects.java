package com.tre.sololeveling.equipment;

import com.tre.sololeveling.SoloLevelingMod;
import com.tre.sololeveling.data.HunterData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SoloLevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EquipmentEffects {
    private static final Map<EquipmentStat, UUID> MODIFIER_IDS = new EnumMap<>(EquipmentStat.class);

    static {
        for (EquipmentStat stat : EquipmentStat.values()) {
            MODIFIER_IDS.put(stat, UUID.nameUUIDFromBytes(("sololeveling:equipment:" + stat.name()).getBytes(StandardCharsets.UTF_8)));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) return;
        int interval;
        try { interval = EquipmentConfig.EFFECT_REFRESH_TICKS.get(); }
        catch (IllegalStateException ignored) { interval = 20; }
        if (player.tickCount % Math.max(1, interval) != 0) return;
        reconcile(player);
    }

    public static void reconcile(ServerPlayer player) {
        Map<EquipmentStat, Double> totals = totals(player);
        for (EquipmentStat stat : EquipmentStat.values()) {
            Attribute attribute = stat.vanillaAttribute();
            if (attribute == null) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            UUID id = MODIFIER_IDS.get(stat);
            AttributeModifier old = instance.getModifier(id);
            if (old != null) instance.removeModifier(old);
            double amount = totals.getOrDefault(stat, 0.0D);
            if (Math.abs(amount) > 0.0000001D) {
                instance.addTransientModifier(new AttributeModifier(id, "Solo Leveling Equipment " + stat.name(),
                        amount, AttributeModifier.Operation.ADDITION));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    public static Map<EquipmentStat, Double> totals(Player player) {
        EnumMap<EquipmentStat, Double> totals = new EnumMap<>(EquipmentStat.class);
        Set<String> equippedIds = new HashSet<>();
        Set<UUID> seenInstances = new HashSet<>();

        addStack(player.getMainHandItem(), totals, equippedIds, seenInstances);
        addStack(player.getOffhandItem(), totals, equippedIds, seenInstances);
        for (ItemStack stack : player.getInventory().armor) addStack(stack, totals, equippedIds, seenInstances);

        for (AccessorySlot slot : AccessorySlot.values()) {
            if (slot == AccessorySlot.NONE) continue;
            String id = HunterData.equipped(player, slot.storageKey());
            if (id == null || id.isBlank()) continue;
            EquipmentCatalog.find(id).ifPresent(definition -> {
                equippedIds.add(definition.id());
                addBonuses(totals, definition.bonuses(), 0);
            });
        }

        boolean setBonuses;
        try { setBonuses = EquipmentConfig.ENABLE_SET_BONUSES.get(); }
        catch (IllegalStateException ignored) { setBonuses = true; }
        if (setBonuses) {
            for (EquipmentSetDefinition set : EquipmentCatalog.sets()) {
                if (equippedIds.containsAll(set.requiredItems())) addBonuses(totals, set.bonuses(), 0);
            }
        }
        return Map.copyOf(totals);
    }

    private static void addStack(ItemStack stack, EnumMap<EquipmentStat, Double> totals,
                                 Set<String> equippedIds, Set<UUID> seenInstances) {
        if (stack.isEmpty()) return;
        EquipmentCatalog.find(stack).ifPresent(definition -> {
            UUID instance = EquipmentData.instanceId(stack, definition);
            if (!seenInstances.add(instance)) return;
            equippedIds.add(definition.id());
            addBonuses(totals, definition.bonuses(), EquipmentData.upgradeLevel(stack, definition));
        });
    }

    private static void addBonuses(EnumMap<EquipmentStat, Double> totals, Iterable<StatBonus> bonuses, int upgradeLevel) {
        double multiplier;
        try { multiplier = EquipmentConfig.UPGRADE_BONUS_MULTIPLIER.get(); }
        catch (IllegalStateException ignored) { multiplier = 1.0D; }
        for (StatBonus bonus : bonuses) {
            totals.merge(bonus.stat(), bonus.valueAt(upgradeLevel, multiplier), Double::sum);
        }
    }

    public static String registryId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private EquipmentEffects() {}
}
