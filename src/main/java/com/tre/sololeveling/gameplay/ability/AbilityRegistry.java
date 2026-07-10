package com.tre.sololeveling.gameplay.ability;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Ordered registry with compatibility aliases for old UI and packet identifiers. */
public final class AbilityRegistry {
    private final Map<String, Ability> abilities = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public synchronized AbilityRegistry register(Ability ability) {
        String id = ability.definition().id();
        if (abilities.putIfAbsent(id, ability) != null) {
            throw new IllegalStateException("Duplicate ability id: " + id);
        }
        return this;
    }

    public synchronized AbilityRegistry alias(String alias, String targetId) {
        String normalizedAlias = AbilityDefinition.normalize(alias);
        String normalizedTarget = AbilityDefinition.normalize(targetId);
        if (normalizedAlias.isBlank() || normalizedTarget.isBlank()) {
            throw new IllegalArgumentException("Ability aliases cannot be blank");
        }
        aliases.put(normalizedAlias, normalizedTarget);
        return this;
    }

    public Optional<Ability> resolve(String rawId) {
        String id = AbilityDefinition.normalize(rawId);
        String target = aliases.getOrDefault(id, id);
        return Optional.ofNullable(abilities.get(target));
    }

    public Collection<Ability> all() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(abilities.keySet());
    }

    public Map<String, String> aliases() {
        return Collections.unmodifiableMap(aliases);
    }
}
