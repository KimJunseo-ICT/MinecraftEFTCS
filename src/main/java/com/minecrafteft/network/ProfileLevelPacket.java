package com.minecrafteft.network;

import com.minecrafteft.inventory.LoadoutMenu;
import com.minecrafteft.profile.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ProfileLevelPacket(int direction) {
    public ProfileLevelPacket(FriendlyByteBuf buffer) { this(buffer.readInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeInt(direction); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();context.enqueueWork(()->{
            var player=context.getSender();
            if(player==null || !player.isCreative() || !(player.containerMenu instanceof LoadoutMenu) || direction!=1 && direction!=-1) return;
            ProfileData.get(player).level(player,direction);ProfileEvents.sync(player);
        });context.setPacketHandled(true);
    }
}
