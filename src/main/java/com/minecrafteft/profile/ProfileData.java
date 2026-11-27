package com.minecrafteft.profile;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Each world save owns its own UUID-indexed records, independent of vanilla XP and player deaths. */
public final class ProfileData extends SavedData {
    private final Map<UUID,CompoundTag> players=new HashMap<>();
    public static ProfileData get(ServerPlayer player) {
        return player.server.overworld().getDataStorage().computeIfAbsent(ProfileData::load,ProfileData::new,"minecrafteft_profiles");
    }
    private static ProfileData load(CompoundTag root) {
        var data=new ProfileData();var list=root.getList("Players",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) {
            var row=list.getCompound(i);
            if(row.hasUUID("Owner")) data.players.put(row.getUUID("Owner"),row.copy());
        }
        return data;
    }
    public CompoundTag player(ServerPlayer player) {
        return players.computeIfAbsent(player.getUUID(),id->{
            var row=new CompoundTag();row.putUUID("Owner",id);row.putLong("CreatedAt",System.currentTimeMillis());row.putInt("Level",1);setDirty();return row;
        });
    }
    public void increment(ServerPlayer player,String key,long amount) {
        var row=player(player);long previous=Math.max(0,row.getLong(key));
        row.putLong(key,previous>Long.MAX_VALUE-amount?Long.MAX_VALUE:previous+Math.max(0,amount));setDirty();
    }
    public void healed(ServerPlayer player,double amount) {
        if(!Double.isFinite(amount) || amount<=0) return;
        var row=player(player);row.putDouble("Healed",Math.max(0,row.getDouble("Healed"))+amount);setDirty();
    }
    public void level(ServerPlayer player,int direction) {
        var row=player(player);row.putInt("Level",(int)Math.max(1,Math.min(Integer.MAX_VALUE,(long)Math.max(1,row.getInt("Level"))+direction)));setDirty();
    }
    @Override public CompoundTag save(CompoundTag root) {
        var list=new ListTag();players.values().forEach(row->list.add(row.copy()));root.put("Players",list);return root;
    }
}
