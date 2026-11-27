package com.minecrafteft.stash;

public enum StashTier {
    ONE(1, 20),
    TWO(2, 40),
    THREE(3, 60),
    FOUR(4, 80);

    private final int level;
    private final int rows;

    StashTier(int level, int rows) {
        this.level = level;
        this.rows = rows;
    }

    public int level() {
        return level;
    }

    public int rows() {
        return rows;
    }

    public int slots() {
        return rows * 10;
    }

    public static StashTier fromLevel(int level) {
        for (StashTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return ONE;
    }
}
