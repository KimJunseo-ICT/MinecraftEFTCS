package com.minecrafteft.status;

import com.minecrafteft.registry.ModContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;

/** A temporary visual decal, not a block: cannot displace terrain or become an item. */
public final class BloodMarkEntity extends Entity {
    public BloodMarkEntity(EntityType<? extends BloodMarkEntity> type,Level level) { super(type,level);noPhysics=true; }
    public static void leave(ServerPlayer player) {
        var start=player.position().add(0,.15,0);
        var hit=player.level().clip(new ClipContext(start,start.add(0,-3,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK || hit.getDirection()!=net.minecraft.core.Direction.UP) return;
        var mark=new BloodMarkEntity(ModContent.BLOOD_MARK.get(),player.level());
        mark.setPos(hit.getLocation().add(0,.006,0));player.level().addFreshEntity(mark);
    }
    @Override public void tick() { super.tick();if(!level().isClientSide && tickCount>=1200) discard(); }
    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
