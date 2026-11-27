package com.minecrafteft.vitals;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/** Replace vanilla hunger ticking, keeping vanilla/mod food use animations and effects. */
public final class VitalFoodData extends FoodData {
    private final Player player;
    private VitalFoodData(Player player) { this.player = player; }
    public static void install(Player player) {
        if (player.getFoodData() instanceof VitalFoodData) return;
        // Player has no public food-data setter in Forge 1.20.1. Use Forge's
        // remapping-aware helper so this works in development and release jars.
        ObfuscationReflectionHelper.setPrivateValue(Player.class, player, new VitalFoodData(player), "f_36097_");
    }
    @Override public boolean needsFood() { return true; }
    @Override public void tick(Player player) { }
    @Override public void eat(int nutrition, float saturation) {
        // Also supports cake and mods that feed a player directly via FoodData.
        if (player instanceof ServerPlayer server) player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            state.energy(Math.max(0, nutrition) * VitalConfig.FOOD_GAIN.get());
            VitalEvents.sync(server, state);
        });
    }
    @Override public void eat(Item item, ItemStack stack) { eat(item, stack, player); }
    @Override public void eat(Item item, ItemStack stack, LivingEntity eater) {
        if (!(player instanceof ServerPlayer server)) return;
        var food = stack.getFoodProperties(eater);
        if (food == null) return;
        player.getCapability(VitalProvider.CAPABILITY).ifPresent(state -> {
            boolean raw = stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN)
                    || stack.is(Items.MUTTON) || stack.is(Items.RABBIT) || stack.is(Items.COD) || stack.is(Items.SALMON);
            state.energy(raw ? -50 : food.getNutrition() * VitalConfig.FOOD_GAIN.get());
            VitalEvents.sync(server, state);
        });
    }
    @Override public void addExhaustion(float amount) { }
    @Override public float getExhaustionLevel() { return 0; }
    @Override public float getSaturationLevel() { return 0; }
    @Override public int getFoodLevel() {
        if(com.minecrafteft.medical.Ai2Treatment.using(player)) return 6;
        // Vanilla's client sprint gate checks this before applying its sprint
        // speed, which avoids a start/stop speed pulse while holding sprint.
        return !player.isCreative() && !player.isSpectator()
                && player.getCapability(VitalProvider.CAPABILITY).map(state -> state.sprintLocked() || state.sprintRestartRequired()
                || (state.stamina() <= VitalState.RED_STAMINA && !player.isSprinting())).orElse(false) ? 6 : 20;
    }
    @Override public int getLastFoodLevel() { return getFoodLevel(); }
}
