package com.minecrafteft.client;

import com.elfmcys.yesstevemodel.O0OooOo0oOOoOoOoOooO000o;
import com.elfmcys.yesstevemodel.OOoOoooOOooO0o0000o0O0o0;
import com.elfmcys.yesstevemodel.OoOoOOoOOoOo0OooOOoO00Oo;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.oO0OO0OooO0Oo0OoO0oOooo0;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/** Only loaded for the matching release. YSM publishes these model fields under obfuscated names. */
final class Ysm265Ai2Arms {
    private static WeakReference<OOoOoooOOooO0o0000o0O0o0> lastGeometry = new WeakReference<>(null);
    private static Ai2YsmMotion.Arms bindings;

    private Ysm265Ai2Arms() {}

    static Ai2YsmMotion.Arms active(LocalPlayer player) {
        // The same availability and first-person switches checked by YSM's RenderArmEvent listener.
        if (!YesSteveModel.isAvailable() || OoOoOOoOOoOo0OooOOoO00Oo.O00OOOooOoooOoo0o0o0oO0O.get()
                || OoOoOOoOOoOo0OooOOoO00Oo.OOOOo0O0oO0OOo0O0O0Oo0O0.get()) return null;
        var state = player.getCapability(O0OooOo0oOOoOoOoOooO000o.Oo0Oo0o00O00Oo0OOoOOoooo).resolve().orElse(null);
        if (state == null || !state.o0ooooOo0o000OOo0oO00OoO()) return null;
        var model = state.O0ooooOO0oOo000O0Oo00OOO();
        if (model == null) return null;
        var geometry = model.Oo0Oo0o00O00Oo0OOoOOoooo().o0OOooo0o0OO00OoOOOo0o0O();
        if (geometry == null) return null;
        if (lastGeometry.get() != geometry) {
            var bones = geometry.Oo0Oo0o00O00Oo0OOoOOoooo;
            var left = geometry.o0o0Oooo00OoOoOOOooo0000 ? bind(bones, true) : null;
            var right = geometry.ooooO0o00oO0Oo0OOo0O0O0o ? bind(bones, false) : null;
            bindings = left == null && right == null ? null : new Ai2YsmMotion.Arms(left, right);
            lastGeometry = new WeakReference<>(geometry);
        }
        return bindings;
    }

    static Ai2YsmMotion.Arm bind(List<oO0OO0OooO0Oo0OoO0oOooo0> bones, boolean left) {
        String side = left ? "Left" : "Right";
        Vector3f shoulder = pivot(bones, side + "Arm");
        if (shoulder == null) shoulder = pivot(bones, side + "UpperArm");
        Vector3f palm = pivot(bones, side + "HandLocator");
        if (palm == null) palm = pivot(bones, side + "Hand");
        if (palm == null && shoulder != null) {
            Vector3f forearm = pivot(bones, side + "ForeArm");
            if (forearm == null) forearm = pivot(bones, side + "LowerArm");
            if (forearm != null) palm = forearm.mul(2).sub(shoulder);
        }
        return Ai2YsmMotion.Arm.fromPivots(left, shoulder, palm);
    }

    static Vector3f pivot(List<oO0OO0OooO0Oo0OoO0oOooo0> bones, String name) {
        name = name.toLowerCase(Locale.ROOT);
        Vector3f numbered = null;
        for (var bone : bones) {
            String key = bone.Oo0Oo0o00O00Oo0OOoOOoooo().toLowerCase(Locale.ROOT)
                    .replace("_", "").replace("-", "").replace(" ", "");
            if (!key.startsWith(name)) continue;
            boolean exact = key.length() == name.length();
            if (!exact && (numbered != null || !key.substring(name.length()).chars().allMatch(Character::isDigit))) continue;
            var value = new Vector3f(bone.Ooooo0oooO0oooOOOoO0000O(), bone.oo0OoO00oOoo000O0000o0oo(),
                    bone.oooooooOOoOOoO00OooOo00O());
            if (exact) return value;
            numbered = value;
        }
        return numbered;
    }
}
