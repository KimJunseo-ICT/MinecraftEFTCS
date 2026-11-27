package com.minecrafteft.corpse;

import com.minecrafteft.inventory.LoadoutState;
import com.minecrafteft.registry.ModContent;
import com.minecrafteft.vitals.VitalState;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;

public final class CorpseEntity extends Entity implements IEntityAdditionalSpawnData {
    public static final double INTERACTION_DISTANCE=2.5;
    private static final EntityDataAccessor<Integer> VISIBLE=SynchedEntityData.defineId(CorpseEntity.class,EntityDataSerializers.INT);
    public static final int BACKPACK_VISIBLE=1<<6;
    private static final EquipmentSlot[] EQUIPMENT=EquipmentSlot.values();
    public final CorpseInventory loot=new CorpseInventory();
    private GameProfile profile=new GameProfile(new UUID(0,0),"Player");
    private CompoundTag appearance=new CompoundTag();

    public CorpseEntity(EntityType<? extends CorpseEntity> type, Level level) {
        super(type,level);
        noPhysics=true;
    }
    static CorpseEntity capture(ServerPlayer player) {
        var body=new CorpseEntity(ModContent.CORPSE.get(),player.level());
        body.profile=player.getGameProfile();
        body.moveTo(player.getX(),player.getY(),player.getZ(),player.yBodyRot,0);
        var state=LoadoutState.of(player);
        for (EquipmentSlot slot:EquipmentSlot.values()) {
            var stack=player.getItemBySlot(slot);
            if (!stack.isEmpty()) body.appearance.put(slot.getName(),stack.save(new CompoundTag()));
        }
        body.appearance.putInt("HeldSlot",player.getInventory().selected);
        body.appearance.putBoolean("Backpack",state.backpackVisible());
        int layers=0;
        for (var part:net.minecraft.world.entity.player.PlayerModelPart.values())
            if (player.isModelPartShown(part)) layers|=part.getMask();
        body.appearance.putByte("SkinParts",(byte)layers);
        body.appearance.putBoolean("LeftHanded",player.getMainArm()==HumanoidArm.LEFT);
        body.appearance.putDouble("MaxHealth",VitalState.MAX_HEALTH);
        var status=com.minecrafteft.status.StatusProvider.of(player);
        if(status!=null) body.appearance.putIntArray("StatusAtDeath",status.snapshot());
        ItemStack melee=state.equipment.getStackInSlot(LoadoutState.MELEE);
        if (!melee.isEmpty()) body.appearance.put("Melee",melee.save(new CompoundTag()));
        // Only the selected model and texture travel with the corpse.
        if (net.minecraftforge.fml.ModList.get().isLoaded("yes_steve_model")) {
            CompoundTag caps=player.saveWithoutId(new CompoundTag()).getCompound("ForgeCaps");
            CompoundTag model=caps.getCompound("yes_steve_model:model_id");
            if (!model.getBoolean("disabled")) {
                body.appearance.putString("Model",model.getString("model_id"));
                body.appearance.putString("Texture",model.getString("select_texture"));
            }
        }
        return body;
    }
    public GameProfile profile() { return profile; }
    public UUID owner() { return profile.getId(); }
    public CompoundTag appearance() { return appearance; }
    public boolean canLoot(Player player) {
        return isAlive() && player.isAlive() && !player.isSpectator() && player.level()==level()
                && player.distanceToSqr(this)<=INTERACTION_DISTANCE*INTERACTION_DISTANCE
                && (level().isClientSide || CorpseIndex.get(level().getServer()).current(this));
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (!canLoot(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) {
            LoadoutState.of(server).restoreMelee(server);
            NetworkHooks.openScreen(server,new SimpleMenuProvider((id,inv,p)->new CorpseMenu(id,inv,this),
                    Component.translatable("corpse.minecrafteft.title",profile.getName())),buf->buf.writeVarInt(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    public int visibleEquipment() { return entityData.get(VISIBLE); }
    void updateEquipment() {
        int mask=0;
        for (var slot:EQUIPMENT) {
            int index=switch(slot) {
                case MAINHAND -> appearance.getInt("HeldSlot");
                case OFFHAND -> 40;
                default -> 36+slot.getIndex();
            };
            if (index>=0 && index<=40 && !loot.items.getStackInSlot(index).isEmpty()) mask|=1<<slot.ordinal();
        }
        if (appearance.getBoolean("Backpack") && !loot.items.getStackInSlot(CorpseInventory.BAG).isEmpty()) mask|=BACKPACK_VISIBLE;
        entityData.set(VISIBLE,mask);
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (!CorpseIndex.get(level().getServer()).current(this)) discard();
            else updateEquipment();
        }
    }
    @Override public boolean shouldRenderAtSqrDistance(double squared) { return squared<=10000 || super.shouldRenderAtSqrDistance(squared); }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override protected void onBelowWorld() { /* A death in the void still owns its last corpse. */ }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return true; }
    @Override public boolean isCurrentlyGlowing() {
        return level().isClientSide && com.minecrafteft.client.CorpseRenderer.ownerOutline(this);
    }
    @Override protected void defineSynchedData() { entityData.define(VISIBLE,0); }
    private CompoundTag identity() {
        CompoundTag tag=new CompoundTag();tag.put("Profile",NbtUtils.writeGameProfile(new CompoundTag(),profile));
        tag.put("Appearance",appearance);return tag;
    }
    private void readIdentity(CompoundTag tag) {
        GameProfile stored=NbtUtils.readGameProfile(tag.getCompound("Profile"));
        if (stored!=null && stored.getId()!=null) profile=stored;
        appearance=tag.getCompound("Appearance");
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.merge(identity());tag.put("Loot",loot.save());
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        readIdentity(tag);
        if (tag.contains("Loot")) loot.load(tag.getCompound("Loot"));
        if (!appearance.contains("HeldSlot")) {
            var held=ItemStack.of(appearance.getCompound(EquipmentSlot.MAINHAND.getName()));
            int index=-1;
            for (int i=0;!held.isEmpty() && i<9;i++) {
                if (ItemStack.isSameItemSameTags(held,loot.items.getStackInSlot(i))) { index=i;break; }
            }
            appearance.putInt("HeldSlot",index);
        }
        updateEquipment();
    }
    @Override public void writeSpawnData(FriendlyByteBuf buf) { buf.writeNbt(identity()); }
    @Override public void readSpawnData(FriendlyByteBuf buf) { var tag=buf.readNbt();if(tag!=null) readIdentity(tag); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
