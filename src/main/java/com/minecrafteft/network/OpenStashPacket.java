package com.minecrafteft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.minecrafteft.stash.StashService;

import java.util.function.Supplier;

public final class OpenStashPacket {
    public OpenStashPacket() {}
    public OpenStashPacket(FriendlyByteBuf ignored) {}
    public void encode(FriendlyByteBuf ignored) {}

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) StashService.openOwned(player);
        });
        context.setPacketHandled(true);
    }
}
