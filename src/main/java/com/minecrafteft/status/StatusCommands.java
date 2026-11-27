package com.minecrafteft.status;

import com.minecrafteft.MinecraftEFTMod;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator-only controls for deterministic in-game integration checks. */
@Mod.EventBusSubscriber(modid=MinecraftEFTMod.MOD_ID)
public final class StatusCommands {
    @SubscribeEvent public static void registerStatusTestCommands(RegisterCommandsEvent event) {
        var root=Commands.literal("eftstatus").requires(source->source.hasPermission(2));
        for(String key:new String[]{"light","heavy","fracture","painkillers","clear"}) {
            root.then(Commands.literal(key).executes(context->{
                var player=context.getSource().getPlayerOrException();var state=StatusProvider.of(player);
                if(state==null) return 0;
                int fracturesBefore=(state.arm()?1:0)+(state.leg()?1:0);
                switch(key) {
                    case "light" -> state.addLight();case "heavy" -> state.addHeavy();case "fracture" -> state.fracture(player.getRandom().nextDouble());
                    case "painkillers" -> state.painkillers();case "clear" -> state.restore(new int[14]);
                }
                int fracturesAfter=(state.arm()?1:0)+(state.leg()?1:0);
                if(fracturesAfter>fracturesBefore) com.minecrafteft.profile.ProfileData.get(player).increment(player,"Fractures",fracturesAfter-fracturesBefore);
                StatusEvents.sync(player);context.getSource().sendSuccess(()->Component.translatable("command.minecrafteft.status_updated"),false);return 1;
            }));
        }
        event.getDispatcher().register(root);
    }
}
