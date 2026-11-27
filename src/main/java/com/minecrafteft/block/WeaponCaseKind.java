package com.minecrafteft.block;

import net.minecraft.world.item.ItemStack;

/** Dimensions and identity stay attached to the block/item, never to mutable contents NBT. */
public enum WeaponCaseKind {
    LARGE("large_weapon_case", 15, 3, ""),
    STANDARD("weapon_case", 7, 2, ""),
    AR_TEAM("weapon_case_ar_team", 7, 2, "ar_team"),
    ELMO("weapon_case_elmo", 7, 2, "elmo"),
    HIDE404("weapon_case_hide404", 7, 2, "hide404");

    private final String id, marking;
    private final int rows, footprintHeight;
    WeaponCaseKind(String id, int rows, int footprintHeight, String marking) {
        this.id=id; this.rows=rows; this.footprintHeight=footprintHeight; this.marking=marking;
    }
    public String id() { return id; }
    public String marking() { return marking; }
    public int rows() { return rows; }
    public int slots() { return rows * 10; }
    public int footprintHeight() { return footprintHeight; }
    public boolean small() { return this != LARGE; }
    public static WeaponCaseKind of(ItemStack stack) {
        return stack.getItem() instanceof WeaponCaseItem item ? item.kind() : LARGE;
    }
}
