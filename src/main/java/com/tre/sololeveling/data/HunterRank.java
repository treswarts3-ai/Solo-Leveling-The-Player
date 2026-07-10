package com.tre.sololeveling.data;

import java.util.Locale;

public enum HunterRank {
    E(0, "E-Rank", 1),
    D(1, "D-Rank", 10),
    C(2, "C-Rank", 20),
    B(3, "B-Rank", 30),
    A(4, "A-Rank", 40),
    S(5, "S-Rank", 60),
    NATIONAL(6, "National-Level", 80),
    SHADOW_MONARCH(7, "Shadow Monarch", 100);

    private final int tier;
    private final String displayName;
    private final int minimumLevel;

    HunterRank(int tier, String displayName, int minimumLevel) {
        this.tier = tier;
        this.displayName = displayName;
        this.minimumLevel = minimumLevel;
    }

    public int tier() { return tier; }
    public String displayName() { return displayName; }
    public int minimumLevel() { return minimumLevel; }

    public static HunterRank automatic(int level, boolean blackHeart) {
        if (level >= SHADOW_MONARCH.minimumLevel && blackHeart) return SHADOW_MONARCH;
        if (level >= NATIONAL.minimumLevel) return NATIONAL;
        if (level >= S.minimumLevel) return S;
        if (level >= A.minimumLevel) return A;
        if (level >= B.minimumLevel) return B;
        if (level >= C.minimumLevel) return C;
        if (level >= D.minimumLevel) return D;
        return E;
    }

    public static HunterRank byTier(int tier) {
        int clamped = Math.max(E.tier, Math.min(SHADOW_MONARCH.tier, tier));
        for (HunterRank rank : values()) if (rank.tier == clamped) return rank;
        return E;
    }

    public static HunterRank parse(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.equals("auto") || normalized.equals("automatic")) return null;
        for (HunterRank rank : values()) {
            String display = rank.displayName.toLowerCase(Locale.ROOT).replace('-', ' ');
            if (normalized.equals(display) || normalized.equals(rank.name().toLowerCase(Locale.ROOT).replace('_', ' '))) return rank;
        }
        if (normalized.matches("[0-7]")) return byTier(Integer.parseInt(normalized));
        return null;
    }
}
