package com.minecrafteft.vitals;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VitalConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue ENERGY_DRAIN, WATER_DRAIN, SPRINT_DRAIN,
            WALK_RECOVERY, IDLE_RECOVERY, ENERGY_DAMAGE, WATER_DAMAGE, FOOD_GAIN, WATER_GAIN, DEPRIVATION_INTERVAL;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.comment("Independent EFT survival values. Time rates use real simulation minutes/seconds.").push("vitals");
        ENERGY_DRAIN = builder.comment("Approximate raid-style baseline, energy points per minute.")
                .defineInRange("energyDrainPerMinute", 1.92, 0, 100);
        WATER_DRAIN = builder.comment("Hydration points per minute.").defineInRange("hydrationDrainPerMinute", 2.17, 0, 100);
        SPRINT_DRAIN = builder.defineInRange("sprintStaminaPerSecond", 8.0, .1, 100);
        WALK_RECOVERY = builder.defineInRange("walkingStaminaRecoveryPerSecond", 8.0, .1, 100);
        IDLE_RECOVERY = builder.defineInRange("idleStaminaRecoveryPerSecond", 12.0, .1, 100);
        DEPRIVATION_INTERVAL = builder.comment("Seconds before the first depletion hit and between later hits.")
                .defineInRange("deprivationDamageIntervalSeconds", 3.0, 1.0, 60.0);
        ENERGY_DAMAGE = builder.comment("HP lost per depletion pulse when energy is empty. Both empty resources stack.")
                .defineInRange("emptyEnergyDamagePerPulse", 1.0, 0, VitalState.MAX_HEALTH);
        WATER_DAMAGE = builder.defineInRange("emptyHydrationDamagePerPulse", 1.0, 0, VitalState.MAX_HEALTH);
        FOOD_GAIN = builder.comment("Energy restored per vanilla nutrition point. Raw meat instead removes 50 energy.")
                .defineInRange("energyPerNutrition", 2.0, 0, 100);
        WATER_GAIN = builder.comment("Hydration restored by drinking a water bottle. Awkward potion instead removes 50.")
                .defineInRange("waterBottleHydration", 25.0, 0, 100);
        builder.pop();
        SPEC = builder.build();
    }
    private VitalConfig() { }
}
