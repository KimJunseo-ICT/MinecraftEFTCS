package com.minecrafteft.client;

import com.elfmcys.yesstevemodel.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

/** Attach to the animated YSM skeleton in the same rendering pass as its built-in accessories. */
final class Ysm265BackpackLayer extends Ooo0O0OOOoOo000OO000oooo<oo0OooOO0oOoOoOoo00oO000> {
    private static o0ooO0o0ooo000OOO0o0oo0o installed;

    static void install() {
        if (!YesSteveModel.isAvailable()) return;
        var renderer = OOoO00ooO00OOO00O0o0000O.Oo0Oo0o00O00Oo0OOoOOoooo();
        if (renderer != null && renderer != installed) {
            renderer.Oo0Oo0o00O00Oo0OOoOOoooo(new Ysm265BackpackLayer());
            installed = renderer;
        }
    }
    @Override public void Oo0Oo0o00O00Oo0OOoOOoooo(PoseStack pose, MultiBufferSource buffers, int light,
            oo0OooOO0oOoOoOoo00oO000 model, float swing, float amount, float partial, float age, float yaw, float pitch) {
        Player player = model.OO00OOOOo0Ooo0oo0o0Oo0OO();
        var skeleton = model.OOOoOO000000o0o0oOooo0o0();
        if (skeleton == null || !BackpackLayer.visible(player)) return;
        // YSM's backpack path, then its general back attachment for older models.
        var bones = skeleton.O0OooOo0oOOoOoOoOooO000o();
        if (bones.isEmpty()) bones = skeleton.o0OOO0o0o0OOo000oO00o00O();
        pose.pushPose();
        try {
            if (!bones.isEmpty()) o000OO00oooOo00000ooo0Oo.Oo0Oo0o00O00Oo0OOoOOoooo(pose,bones);
            else {
                var geometry = skeleton.oOOOo0OOO0ooooo0O00OO0o0().Oo0Oo0o00O00Oo0OOoOOoooo;
                var shoulder = Ysm265Ai2Arms.pivot(geometry,"LeftArm");
                if (shoulder == null) shoulder = Ysm265Ai2Arms.pivot(geometry,"LeftUpperArm");
                pose.translate(0,shoulder == null ? 1.42 : shoulder.y/16.0,shoulder == null ? 0 : shoulder.z/16.0);
            }
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            BackpackLayer.draw(pose,buffers,light,player);
        } finally { pose.popPose(); }
    }
}
