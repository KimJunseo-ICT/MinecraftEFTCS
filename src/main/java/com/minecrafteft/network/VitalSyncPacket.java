package com.minecrafteft.network;

import com.minecrafteft.vitals.VitalState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Owner-only, server-to-client snapshot. Clients can never submit vital values. */
public record VitalSyncPacket(double health, double energy, double hydration, double stamina, boolean sprintLocked,boolean sprintRestartRequired,int sprintRestartAcknowledgement) {
    public VitalSyncPacket(VitalState state) { this(state.health(), state.energy(), state.hydration(), state.stamina(), state.sprintLocked(),state.sprintRestartRequired(),state.sprintRestartAcknowledgement()); }
    public VitalSyncPacket(FriendlyByteBuf buffer) { this(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),buffer.readBoolean(),buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(health).writeDouble(energy).writeDouble(hydration).writeDouble(stamina).writeBoolean(sprintLocked).writeBoolean(sprintRestartRequired);
        buffer.writeVarInt(sprintRestartAcknowledgement);
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.minecrafteft.client.VitalHud.accept(this)));
        context.setPacketHandled(true);
    }
}
