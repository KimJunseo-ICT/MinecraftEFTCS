package com.minecrafteft.status;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;

public final class StatusProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<StatusState> CAPABILITY=CapabilityManager.get(new CapabilityToken<>() { });
    private final StatusState state=new StatusState();
    private LazyOptional<StatusState> optional=LazyOptional.of(()->state);
    public static StatusState of(LivingEntity entity) { return entity.getCapability(CAPABILITY).resolve().orElse(null); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side) {
        if(cap!=CAPABILITY) return LazyOptional.empty();
        if(!optional.isPresent()) optional=LazyOptional.of(()->state);
        return optional.cast();
    }
    public void invalidate() { optional.invalidate(); }
    @Override public CompoundTag serializeNBT() { var tag=new CompoundTag();tag.putIntArray("Status",state.snapshot());return tag; }
    @Override public void deserializeNBT(CompoundTag tag) { state.restore(tag.getIntArray("Status")); }
}
