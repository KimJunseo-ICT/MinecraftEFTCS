package com.minecrafteft.stash;

import java.util.Set;

/** Pure size rules shared by client rendering and server placement through StashMenu. */
public final class GunFootprintRules {
    private static final Set<String> MAGAZINE_SIZE_EXCLUDED = Set.of(
            "tacz:springfield1873", "tacz:kar98", "tacz:svd", "tacz:mrad", "tacz:m1866");
    private static final Set<String> SCOPE_SIZE_EXCLUDED = Set.of(
            "tacz:springfield1873", "tacz:kar98", "tacz:m1866");
    private static final Set<String> PISTOL_MAGAZINE_EXCLUDED = Set.of(
            "tacz:taurus943", "tacz:rhino357", "tacz:taurus500", "tacz:lonetrail");
    private static final Set<String> SMG_MAGAZINE_EXTENDS = Set.of(
            "tacz:hk_mp5a5", "tacz:uzi", "tacz:vector45", "tacz:ump45", "tacz:hk_mp7a1");
    private static final Set<String> STOCK_EXTENDS = Set.of(
            "tacz:hk_mp5a5", "tacz:uzi", "tacz:vector45", "tacz:ump45",
            "tacz:hk_mp7a1", "tacz:tec_9", "tacz:db_short", "tacz:spas_12");
    private static final Set<String> LONG_MUZZLES = Set.of(
            "tacz:muzzle_silencer_knight_qd", "tacz:muzzle_silencer_mirage",
            "tacz:muzzle_silencer_phantom_s1", "tacz:muzzle_silencer_ptilopsis",
            "tacz:muzzle_silencer_sg", "tacz:muzzle_silencer_socom_rc1",
            "tacz:muzzle_silencer_ursus", "tacz:muzzle_silencer_vulture",
            "tacz:muzzle_silencer_wraith", "tacz:bayonet_6h3", "tacz:bayonet_m9",
            "tacz:oem_m1897_bayonet");
    // TACZ keeps optical zoom in client display assets. Explicit IDs keep the size identical
    // on dedicated servers. Variable optics qualify by their maximum magnification.
    private static final Set<String> SCOPES_AT_LEAST_4X = Set.of(
            "tacz:scope_standard_8x", "tacz:scope_pso_1", "tacz:scope_lpvo_1_6",
            "tacz:scope_elcan_4x", "tacz:scope_aug_default", "tacz:scope_vudu",
            "tacz:scope_mk5hd", "tacz:scope_xm157", "tacz:scope_1873_6x",
            "tacz:scope_1866_4x", "tacz:scope_contender", "tacz:scope_98k",
            "tacz:scope_mk4ert", "tacz:scope_amg_10", "tacz:scope_reap_ir",
            "tacz:scope_xl50");

    private GunFootprintRules() { }

    public static Size apply(int baseWidth, int baseHeight, String gunId, String gunType,
                             String muzzleId, String stockId, boolean magazine, String scopeId) {
        int left = LONG_MUZZLES.contains(muzzleId)
                || ("tacz:deagle_golden".equals(gunId)
                && "tacz:deagle_golden_long_barrel".equals(muzzleId)) ? 1 : 0;
        int right = 0;
        if ("tacz:oem_stock_removed".equals(stockId)) {
            right = -1;
        } else if (STOCK_EXTENDS.contains(gunId) && !stockId.isEmpty()) {
            right = 1;
        }
        boolean magazineExtends = switch (gunType) {
            case "pistol" -> !PISTOL_MAGAZINE_EXCLUDED.contains(gunId);
            case "smg" -> SMG_MAGAZINE_EXTENDS.contains(gunId);
            case "shotgun" -> "tacz:aa12".equals(gunId);
            default -> true;
        };
        magazineExtends &= !MAGAZINE_SIZE_EXCLUDED.contains(gunId);
        boolean highPowerScope = !SCOPE_SIZE_EXCLUDED.contains(gunId) && SCOPES_AT_LEAST_4X.contains(scopeId);
        boolean hasScope = !SCOPE_SIZE_EXCLUDED.contains(gunId) && !scopeId.isEmpty();
        boolean deagleScope = ("tacz:deagle".equals(gunId) || "tacz:deagle_golden".equals(gunId)) && hasScope;
        // Rifle optics and magazines share one extra row; never add the two twice.
        int bottom = (magazine && magazineExtends)
                || ("rifle".equals(gunType) && highPowerScope) || deagleScope ? 1 : 0;
        int top = (("sniper".equals(gunType) || "tacz:fn_evolys".equals(gunId)) && highPowerScope)
                || (PISTOL_MAGAZINE_EXCLUDED.contains(gunId) && hasScope) ? 1 : 0;
        return new Size(Math.max(1, baseWidth + left + right), baseHeight + top + bottom);
    }

    /** Storage anchors are the top-left corner of the final occupied rectangle. */
    public record Size(int width, int height) { }
}
