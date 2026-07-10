package com.tre.sololeveling.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central registration point. Content workers may register definitions without changing quest runtime code. */
public final class QuestRegistry {
    private static final Map<ResourceLocation, QuestDefinition> QUESTS = new LinkedHashMap<>();

    public static QuestDefinition register(QuestDefinition definition) {
        QuestDefinition previous = QUESTS.putIfAbsent(definition.id(), definition);
        if (previous != null) throw new IllegalStateException("Duplicate quest id: " + definition.id());
        return definition;
    }

    public static QuestDefinition get(ResourceLocation id) { return QUESTS.get(id); }
    public static QuestDefinition get(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? null : get(parsed);
    }
    public static Collection<QuestDefinition> values() { return Collections.unmodifiableCollection(QUESTS.values()); }
    public static String[] ids() { return QUESTS.keySet().stream().map(ResourceLocation::toString).toArray(String[]::new); }

    private QuestRegistry() { }
}
