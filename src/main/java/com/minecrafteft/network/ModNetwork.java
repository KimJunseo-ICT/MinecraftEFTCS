package com.minecrafteft.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.minecrafteft.MinecraftEFTMod;

import java.util.Optional;

public final class ModNetwork {
    private static final String PROTOCOL = "16";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MinecraftEFTMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(ProfileSyncPacket.class,9,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProfileSyncPacket::encode).decoder(ProfileSyncPacket::new).consumerMainThread(ProfileSyncPacket::handle).add();
        CHANNEL.messageBuilder(ProfileLevelPacket.class,10,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(ProfileLevelPacket::encode).decoder(ProfileLevelPacket::new).consumerMainThread(ProfileLevelPacket::handle).add();
        CHANNEL.messageBuilder(SprintRestartPacket.class,8,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(SprintRestartPacket::encode).decoder(SprintRestartPacket::new).consumerMainThread(SprintRestartPacket::handle).add();
        CHANNEL.messageBuilder(StatusSyncPacket.class,7,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatusSyncPacket::encode).decoder(StatusSyncPacket::new).consumerMainThread(StatusSyncPacket::handle).add();
        CHANNEL.messageBuilder(OpenStashPacket.class, 0)
                .encoder(OpenStashPacket::encode)
                .decoder(OpenStashPacket::new)
                .consumerMainThread(OpenStashPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScrollStashPacket.class, 1)
                .encoder(ScrollStashPacket::encode)
                .decoder(ScrollStashPacket::new)
                .consumerMainThread(ScrollStashPacket::handle)
                .add();
        CHANNEL.messageBuilder(VitalSyncPacket.class, 2, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VitalSyncPacket::encode)
                .decoder(VitalSyncPacket::new)
                .consumerMainThread(VitalSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(LoadoutActionPacket.class,3,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(LoadoutActionPacket::encode).decoder(LoadoutActionPacket::new).consumerMainThread(LoadoutActionPacket::handle).add();
        CHANNEL.messageBuilder(CreativeBackpackPacket.class,5,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreativeBackpackPacket::encode).decoder(CreativeBackpackPacket::new).consumerMainThread(CreativeBackpackPacket::handle).add();
        CHANNEL.messageBuilder(BackpackAppearancePacket.class,6,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackpackAppearancePacket::encode).decoder(BackpackAppearancePacket::new).consumerMainThread(BackpackAppearancePacket::handle).add();
        CHANNEL.messageBuilder(LoadoutSyncPacket.class,4,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LoadoutSyncPacket::encode).decoder(LoadoutSyncPacket::new).consumerMainThread(LoadoutSyncPacket::handle).add();
    }
}
