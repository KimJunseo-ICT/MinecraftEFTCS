package com.minecrafteft.client;

import com.minecrafteft.MinecraftEFTMod;
import com.minecrafteft.inventory.DogtagItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID,value=Dist.CLIENT)
public final class DogtagDetails {
    private static ItemStack expanded=ItemStack.EMPTY;
    private static boolean pressed;
    private static boolean shift(int key) { return key==GLFW.GLFW_KEY_LEFT_SHIFT || key==GLFW.GLFW_KEY_RIGHT_SHIFT; }
    private static boolean recorded(ItemStack stack) {
        if (!(stack.getItem() instanceof DogtagItem) || !stack.hasTag()
                || !stack.getTag().contains("DeathTime",net.minecraft.nbt.Tag.TAG_STRING)) return false;
        var player=net.minecraft.client.Minecraft.getInstance().player;
        return player==null || !player.getCapability(com.minecrafteft.inventory.LoadoutProvider.CAPABILITY)
                .map(state->state.equipment.getStackInSlot(com.minecrafteft.inventory.LoadoutState.DOGTAG)==stack).orElse(false);
    }
    @SubscribeEvent public static void press(ScreenEvent.KeyPressed.Pre event) {
        if (!shift(event.getKeyCode()) || pressed) return;
        pressed=true;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        var slot=screen.getSlotUnderMouse();
        if (slot==null || !recorded(slot.getItem())) return;
        var stack=slot.getItem();
        expanded=ItemStack.isSameItemSameTags(stack,expanded)?ItemStack.EMPTY:stack.copy();
    }
    @SubscribeEvent public static void release(ScreenEvent.KeyReleased.Pre event) {
        if (shift(event.getKeyCode())) pressed=false;
    }
    @SubscribeEvent public static void close(ScreenEvent.Closing event) { expanded=ItemStack.EMPTY;pressed=false; }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack=event.getItemStack();
        if (!recorded(stack)) return;
        var lines=event.getToolTip();
        boolean open=ItemStack.isSameItemSameTags(stack,expanded);
        if (open) {
            var tag=stack.hasTag()?stack.getTag():new net.minecraft.nbt.CompoundTag();
            lines.removeIf(line->line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents content
                    && content.getKey().equals("tooltip.minecrafteft.dogtag_owner"));
            lines.add(field("name",tag.contains("OwnerName")?tag.getString("OwnerName"):"-"));
            lines.add(field("level",tag.contains("Level")?tag.getInt("Level"):1));
            lines.add(field("time",tag.contains("DeathTime")?tag.getString("DeathTime"):"-"));
            Component cause=value(tag.getString("Cause"));
            var gunId=net.minecraft.resources.ResourceLocation.tryParse(tag.getString("GunId"));
            if (tag.contains("GunId") && gunId!=null) {
                var gun=com.tacz.guns.api.TimelessAPI.getClientGunIndex(gunId)
                        .map(index->Component.translatable(index.getName())).orElseGet(()->Component.literal(gunId.toString()));
                cause=Component.translatable("corpse.minecrafteft.shot",gun);
            }
            lines.add((tag.contains("StatusCause")?Component.translatable("corpse.minecrafteft.status_death",cause)
                    :Component.translatable("corpse.minecrafteft.cause",value(tag.getString("Killer")),cause)).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("corpse.minecrafteft."+(open?"hide":"details")).withStyle(ChatFormatting.DARK_GRAY));
    }
    private static Component field(String key,Object value) { return Component.translatable("corpse.minecrafteft."+key,value).withStyle(ChatFormatting.GRAY); }
    private static Component value(String serialized) {
        if (!serialized.isEmpty()) {
            try { var c=Component.Serializer.fromJson(serialized);if(c!=null) return c; }
            catch (com.google.gson.JsonParseException ignored) { }
        }
        return Component.literal("-");
    }
}
