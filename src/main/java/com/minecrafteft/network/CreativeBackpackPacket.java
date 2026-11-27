package com.minecrafteft.network;

import com.minecrafteft.inventory.LoadoutEvents;
import com.minecrafteft.inventory.LoadoutState;
import com.minecrafteft.inventory.StorageEquipment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Like a vanilla creative slot update, the creative item picker owns its cursor. */
public record CreativeBackpackPacket(ItemStack stack) {
    public CreativeBackpackPacket(FriendlyByteBuf buf) { this(buf.readItem()); }
    public void encode(FriendlyByteBuf buf) { buf.writeItem(stack); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null && player.isCreative() && player.isAlive() && player.containerMenu == player.inventoryMenu) {
            var state = LoadoutState.of(player);
            if (!state.creativeCustom && (stack.isEmpty() || (stack.getCount() == 1 && StorageEquipment.accepts(stack, "backpack"))))
                state.equipment.setStackInSlot(LoadoutState.BACKPACK, stack.copy());
            LoadoutEvents.sync(player);
        }
        context.get().setPacketHandled(true);
    }
}
