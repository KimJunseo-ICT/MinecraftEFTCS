package com.minecrafteft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Tracking players need only the exterior, never the wearer's inventory. */
public record BackpackAppearancePacket(int entityId, boolean visible) {
    public BackpackAppearancePacket(FriendlyByteBuf buf) { this(buf.readVarInt(), buf.readBoolean()); }
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(entityId); buf.writeBoolean(visible); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.minecrafteft.client.BackpackLayer.update(entityId, visible));
        context.get().setPacketHandled(true);
    }
}
