package com.minecrafteft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.minecrafteft.stash.StashMenu;

import java.util.function.Supplier;

public final class ScrollStashPacket {
    private final int containerId;
    private final int firstRow;

    public ScrollStashPacket(int containerId, int firstRow) {
        this.containerId = containerId;
        this.firstRow = firstRow;
    }

    public ScrollStashPacket(FriendlyByteBuf buffer) {
        this(buffer.readInt(), buffer.readInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(containerId);
        buffer.writeInt(firstRow);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof StashMenu menu
                    && menu.containerId == containerId) {
                menu.setScrollRow(firstRow);
                menu.broadcastFullState();
            }
        });
        context.setPacketHandled(true);
    }
}
