package com.minecrafteft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ProfileSyncPacket(CompoundTag data) {
    public ProfileSyncPacket(FriendlyByteBuf buffer) { this(buffer.readNbt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeNbt(data); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.minecrafteft.client.ProfileView.accept(data)));
        context.setPacketHandled(true);
    }
}
