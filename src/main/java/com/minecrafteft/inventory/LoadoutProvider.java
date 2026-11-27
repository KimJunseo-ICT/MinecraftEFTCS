package com.minecrafteft.inventory;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public final class LoadoutProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<LoadoutState> CAPABILITY=CapabilityManager.get(new CapabilityToken<>() { });
    private final LoadoutState state=new LoadoutState();
    private LazyOptional<LoadoutState> optional=LazyOptional.of(()->state);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) {
        if(cap!=CAPABILITY) return LazyOptional.empty();
        // Entity.reviveCaps reopens the dispatcher for Clone, not the provider's old handle.
        if(!optional.isPresent()) optional=LazyOptional.of(()->state);
        return optional.cast();
    }
    public void invalidate() { optional.invalidate(); }
    @Override public CompoundTag serializeNBT() { return state.save(); }
    @Override public void deserializeNBT(CompoundTag tag) { state.load(tag); }
}
