package com.minecrafteft.network;

import com.minecrafteft.vitals.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Input intent only. The server validates recovery and owns the exhaustion latch. */
public final class SprintRestartPacket {
    private final int request;
    public SprintRestartPacket(int request) { this.request=request; }
    public SprintRestartPacket(FriendlyByteBuf buf) { this(buf.readVarInt()); }
    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(request); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->{
            var player=context.getSender();if(player==null || !player.isAlive() || player.isSpectator()) return;
            var status=com.minecrafteft.status.StatusProvider.of(player);
            player.getCapability(VitalProvider.CAPABILITY).ifPresent(state->{
                if(!player.isUsingItem() && (status==null || !status.sprintBlocked())) state.restartSprint();
                state.acknowledgeSprintRestart(request);VitalEvents.sync(player,state);
            });
        });
        context.setPacketHandled(true);
    }
}
