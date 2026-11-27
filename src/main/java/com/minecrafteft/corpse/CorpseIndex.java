package com.minecrafteft.corpse;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Global across dimensions, including corpses in chunks that are currently unloaded. */
final class CorpseIndex extends SavedData {
    private final Map<UUID,UUID> latest=new HashMap<>();

    static CorpseIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CorpseIndex::load,CorpseIndex::new,"minecrafteft_corpses");
    }
    boolean current(CorpseEntity body) {
        return body.getUUID().equals(latest.get(body.owner()));
    }
    void replace(MinecraftServer server,CorpseEntity body) {
        UUID old=latest.put(body.owner(),body.getUUID());
        setDirty();
        if (old!=null && !old.equals(body.getUUID())) {
            for (var level:server.getAllLevels())
                if (level.getEntity(old) instanceof CorpseEntity previous) previous.discard();
        }
    }
    private static CorpseIndex load(CompoundTag tag) {
        var index=new CorpseIndex();
        for (Tag entry:tag.getList("Latest",Tag.TAG_COMPOUND)) {
            var pair=(CompoundTag)entry;
            if (pair.hasUUID("Owner") && pair.hasUUID("Body")) index.latest.put(pair.getUUID("Owner"),pair.getUUID("Body"));
        }
        return index;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var entries=new ListTag();
        latest.forEach((owner,body)->{
            var pair=new CompoundTag();pair.putUUID("Owner",owner);pair.putUUID("Body",body);entries.add(pair);
        });
        tag.put("Latest",entries);
        return tag;
    }
}
