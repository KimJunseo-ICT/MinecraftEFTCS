package com.minecrafteft.block;

import com.minecrafteft.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Temporary, invisible mount; sneaking uses vanilla passenger dismount controls. */
public final class StashSeatEntity extends Entity {
    private BlockPos bench=BlockPos.ZERO;
    public StashSeatEntity(EntityType<? extends StashSeatEntity> type,Level level) {
        super(type,level);
        noPhysics=true;
    }
    public static void sit(Level level,BlockPos pos,Player player) {
        if(level.isClientSide || player.isPassenger()) return;
        if(!level.getEntitiesOfClass(StashSeatEntity.class,new AABB(pos).inflate(.1),s->s.bench.equals(pos)).isEmpty()) return;
        var state=level.getBlockState(pos);
        var decoration=ContainerStructureLayout.decorationAt(state.getValue(ContainerPartBlock.X),
                state.getValue(ContainerPartBlock.Y),state.getValue(ContainerPartBlock.Z));
        var seat=new StashSeatEntity(ModContent.STASH_SEAT.get(),level);
        seat.bench=pos.immutable();
        seat.setPos(pos.getX()+.5,pos.getY()+(decoration.deck()?.12:0)+.54,pos.getZ()+.5);
        seat.setYRot(state.getValue(ContainerPartBlock.FACING).toYRot());
        if(level.addFreshEntity(seat) && !player.startRiding(seat)) seat.discard();
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) return;
        var state=level().getBlockState(bench);
        if(getPassengers().isEmpty() || !(state.getBlock() instanceof ContainerPartBlock)
                || !ContainerStructureLayout.decorationAt(state.getValue(ContainerPartBlock.X),
                state.getValue(ContainerPartBlock.Y),state.getValue(ContainerPartBlock.Z)).seat()) discard();
    }
    // Keep the seated model above the boards; -.40 put its trousers through them.
    // Vanilla also supplies the player's own -.35 riding offset.
    @Override public double getPassengersRidingOffset() { return -.20; }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        var state=level().getBlockState(bench);
        if(state.getBlock() instanceof ContainerPartBlock) {
            var front=state.getValue(ContainerPartBlock.FACING);
            var point=Vec3.atBottomCenterOf(bench.relative(front)).add(0,.12,0);
            if(level().noCollision(passenger,passenger.getBoundingBox().move(point.subtract(passenger.position())))) return point;
        }
        return position().add(0,.2,0);
    }
    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
