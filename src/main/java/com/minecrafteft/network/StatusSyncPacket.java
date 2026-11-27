package com.minecrafteft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record StatusSyncPacket(int[] values) {
    public StatusSyncPacket(FriendlyByteBuf buf) { this(buf.readVarIntArray(1100)); }
    public void encode(FriendlyByteBuf buf) { buf.writeVarIntArray(values); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.minecrafteft.client.StatusPresentation.accept(values)));
        context.setPacketHandled(true);
    }
}
