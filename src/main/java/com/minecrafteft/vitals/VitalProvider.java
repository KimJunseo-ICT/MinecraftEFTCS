package com.minecrafteft.vitals;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class VitalProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<VitalState> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() { });
    private final VitalState state = new VitalState();
    private LazyOptional<VitalState> optional = LazyOptional.of(() -> state);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if(capability != CAPABILITY) return LazyOptional.empty();
        if(!optional.isPresent()) optional = LazyOptional.of(() -> state);
        return optional.cast();
    }
    public void invalidate() { optional.invalidate(); }
    @Override public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        tag.putDouble("Health", state.health());
        tag.putDouble("MaxHealth", VitalState.MAX_HEALTH);
        tag.putDouble("Energy", state.energy());
        tag.putDouble("Hydration", state.hydration());
        tag.putDouble("Stamina", state.stamina());
        tag.putDouble("MaxStamina",VitalState.MAX_STAMINA);
        tag.putDouble("StaminaRecoveryDelay",state.staminaRecoveryDelay());
        tag.putBoolean("SprintRestartRequired",state.sprintRestartRequired());
        tag.putBoolean("SprintLocked", state.sprintLocked());
        tag.putDouble("DeprivationElapsed", state.deprivationElapsed());
        return tag;
    }
    @Override public void deserializeNBT(CompoundTag tag) {
        // ver13 saves had a 200 HP maximum and no MaxHealth field.
        double health = tag.contains("Health") ? VitalState.savedHealth(tag.getDouble("Health"),
                tag.contains("MaxHealth") ? tag.getDouble("MaxHealth") : 200) : VitalState.MAX_HEALTH;
        state.update(health,
                tag.contains("Energy") ? tag.getDouble("Energy") : 100,
                tag.contains("Hydration") ? tag.getDouble("Hydration") : 100,
                tag.contains("Stamina") ? VitalState.savedStamina(tag.getDouble("Stamina"),tag.contains("MaxStamina")?tag.getDouble("MaxStamina"):100) : VitalState.MAX_STAMINA,
                tag.getBoolean("SprintLocked"));
        state.restoreDeprivationElapsed(tag.getDouble("DeprivationElapsed"));
        state.restoreSprintRuntime(tag.getDouble("StaminaRecoveryDelay"),tag.getBoolean("SprintRestartRequired"));
    }
}
