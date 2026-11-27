package com.minecrafteft.status;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Damage identity travels through vanilla death handling; unrelated hits never inherit an injury cause. */
public enum StatusDamage {
    HEAVY_BLEEDING("heavy_bleeding"),LIGHT_BLEEDING("light_bleeding"),FRACTURE("fracture"),
    DEHYDRATION("dehydration"),MALNUTRITION("malnutrition");
    private final String id;
    private final ResourceKey<DamageType> key;
    StatusDamage(String id) {
        this.id=id;
        key=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("minecrafteft",id));
    }
    public String translation() { return "corpse.minecrafteft.cause_"+id; }
    public DamageSource source(LivingEntity victim) {
        return new DamageSource(victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
    public static StatusDamage from(DamageSource source) {
        for(var cause:values()) if(source.is(cause.key)) return cause;
        return null;
    }
}
