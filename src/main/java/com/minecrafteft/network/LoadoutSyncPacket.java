package com.minecrafteft.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record LoadoutSyncPacket(CompoundTag data) {
    public LoadoutSyncPacket(FriendlyByteBuf buffer) { this(buffer.readNbt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeNbt(data); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                ()->()->com.minecrafteft.client.LoadoutClient.accept(data)));
        context.get().setPacketHandled(true);
    }
}
