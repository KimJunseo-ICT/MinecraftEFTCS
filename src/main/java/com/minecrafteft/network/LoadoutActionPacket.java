package com.minecrafteft.network;

import com.minecrafteft.inventory.*;
import com.minecrafteft.registry.ModContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.*;
import java.util.function.Supplier;

public record LoadoutActionPacket(int action,int value) {
    public static final int OPEN=0,VANILLA=1,MELEE=2,SELECT=3;
    public LoadoutActionPacket(FriendlyByteBuf buffer) { this(buffer.readVarInt(),buffer.readVarInt()); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(action);buffer.writeVarInt(value); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()-> {
            var player=context.get().getSender();if(player==null || !player.isAlive() || player.isSpectator()) return;
            var state=LoadoutState.of(player);
            if(action==OPEN) {
                if(player.containerMenu!=player.inventoryMenu) return;
                if(player.isCreative() && !CreativeLoadoutTransition.apply(player)) return;
                state.creativeCustom=player.isCreative();state.restoreMelee(player);
                if(player.isCreative()) state.migrated=false;
                LoadoutInventory.normalize(player);LoadoutEvents.sync(player);
                NetworkHooks.openScreen(player,new SimpleMenuProvider((id,inventory,who)->new LoadoutMenu(id,inventory),Component.translatable("container.minecrafteft.loadout")));
            } else if(action==VANILLA && player.isCreative()) { state.restoreMelee(player);state.creativeCustom=false;player.doCloseContainer();LoadoutEvents.sync(player); }
            else if(action==MELEE && LoadoutState.custom(player) && player.containerMenu==player.inventoryMenu) { state.toggleMelee(player);LoadoutEvents.sync(player);player.inventoryMenu.broadcastChanges(); }
            else if(action==SELECT && value>=0 && value<9) { state.restoreMelee(player);player.getInventory().selected=value;LoadoutEvents.sync(player);player.inventoryMenu.broadcastChanges(); }
        });context.get().setPacketHandled(true);
    }
}
