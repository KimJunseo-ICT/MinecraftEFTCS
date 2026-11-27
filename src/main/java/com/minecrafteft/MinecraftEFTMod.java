package com.minecrafteft;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.minecrafteft.network.ModNetwork;
import com.minecrafteft.registry.ModContent;

/** Main entry point for the Minecraft EFT mod. */
@Mod(MinecraftEFTMod.MOD_ID)
public final class MinecraftEFTMod {
    public static final String MOD_ID = "minecrafteft";

    public MinecraftEFTMod(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModContent.register(modBus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.minecrafteft.vitals.VitalConfig.SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.minecrafteft.corpse.CorpseTestDrops.SPEC,"minecrafteft-corpse-test-server.toml");
        ModNetwork.register();
    }
}
