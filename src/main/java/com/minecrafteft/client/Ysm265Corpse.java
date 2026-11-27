package com.minecrafteft.client;

import com.elfmcys.yesstevemodel.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;

/** Isolated YSM 2.6.5 world rig. The main player timeline supplies the frozen corpse pose. */
final class Ysm265Corpse extends oo0OooOO0oOoOoOoo00oO000 implements CorpseRenderer.WorldPose {
    private final CorpseRenderer.Avatar avatar;
    private final String modelId;
    private OO00O0o0OooOOOo00OO00o00<?> frozen;
    private static final String MAIN_CONTROLLER="player.main";
    private boolean lieFlat;
    private String poseClip;
    private boolean poseRegistered;

    static CorpseRenderer.WorldPose create(CorpseRenderer.Avatar avatar,String model,String texture) {
        return YesSteveModel.isAvailable()?new Ysm265Corpse(avatar,model,texture):null;
    }

    private Ysm265Corpse(CorpseRenderer.Avatar avatar,String model,String texture) {
        super(avatar,false,false);
        this.avatar=avatar;
        this.modelId=model;
        Oo0Oo0o00O00Oo0OOoOOoooo(model,texture);
        o0OOooo0o0OO00OoOOOo0o0O(false);
    }

    static void select(CorpseRenderer.Avatar avatar,String model,String texture) {
        if (!YesSteveModel.isAvailable()) return;
        avatar.getCapability(com.elfmcys.yesstevemodel.O0OooOo0oOOoOoOoOooO000o.Oo0Oo0o00O00Oo0OOoOOoooo).ifPresent(state->{
            if (!model.isEmpty()) state.Oo0Oo0o00O00Oo0OOoOOoooo(model,texture);
            state.o0OOooo0o0OO00OoOOOo0o0O(model.isEmpty());
        });
    }

    // Use the same model and texture loader as the YSM preview for an unregistered player.
    @Override protected o0O0oOooOo0OoOo0oOo00O00<Player>.Oo0Oo0o00O00Oo0OOoOOoooo o0OOooo0o0OO00OoOOOo0o0O(
            oOo0oO0OOo0ooOoo0oOo0oOo model,boolean flag) {
        return new Oo0Oo0o00O00Oo0OOoOOoooo(model,flag,false,true,300);
    }

    @Override protected void a_() {
        frozen=null;
        lieFlat=OOOOo0O0oO0OOo0O0O0Oo0O0("death")==null;
        poseClip=lieFlat?"idle":"death";
        if (OOOOo0O0oO0OOo0O0O0Oo0O0(poseClip)==null) poseClip=null;
        poseRegistered=false;
        // Let the pack register its world controllers, including parallel visibility and accessory tracks.
        super.a_();
        if (poseClip!=null && !poseRegistered) registerPose();
    }

    @Override public <T extends o0000OoOooO0oo0o0oooo0Oo<Player>> void Oo0Oo0o00O00Oo0OOoOOoooo(
            OoOoOO0O00oOoO0o0ooOO0oO controller) {
        if (poseClip!=null && MAIN_CONTROLLER.equals(controller.Oo0Oo0o00O00Oo0OOoOOoooo())) registerPose();
        else super.Oo0Oo0o00O00Oo0OOoOOoooo(controller);
    }

    private void registerPose() {
        var controller=new oo000oooo0OOoo00O0o0OOOO<Ysm265Corpse>(this,MAIN_CONTROLLER,0,
                (frame,context)->oOOO0Oo0O00oo0O0oO0OooO0.Oo0Oo0o00O00Oo0OOoOOoooo(
                        frame,poseClip,OooOO0OOOoO0oOO0OOOO0Ooo.O00OOOooOoooOoo0o0o0oO0O));
        super.Oo0Oo0o00O00Oo0OOoOOoooo(controller);
        poseRegistered=true;
    }

    @Override protected boolean Oo0Oo0o00O00Oo0OOoOOoooo(OO00O0o0OooOOOo00OO00o00<?> frame) { return true; }
    @Override public int oOo0oOOOO0ooOOO0oOoOO0oo() { return 60; }

    @Override public OO00O0o0OooOOOo00OO00o00<?> o0OOooo0o0OO00OoOOOo0o0O(float partial,boolean asynchronous) {
        return frozen!=null?frozen:super.o0OOooo0o0OO00OoOOOo0o0O(partial,false);
    }

    private float headHeight() {
        var head=OOOoOO000000o0o0oOooo0o0().oOOOooO00oo0oOooooOO0Oo0();
        if (head.isEmpty()) return Float.NaN;
        PoseStack attachment=new PoseStack();
        o000OO00oooOo00000ooo0Oo.Oo0Oo0o00O00Oo0OOoOOoooo(attachment,head);
        return attachment.last().pose().m31();
    }

    @Override public boolean draw(PoseStack pose,MultiBufferSource buffers,int light) {
        var renderer=OOoO00ooO00OOO00O0o0000O.Oo0Oo0o00O00Oo0OOoOOoooo();
        if (renderer==null) return false;
        var selected=com.elfmcys.yesstevemodel.o0OooO00ooo0OO000O0OoOoO.Oo0Oo0o00O00Oo0OOoOOoooo(modelId);
        if (selected.isEmpty()) return false;
        if (selected.get()!=O0ooooOO0oOo000O0Oo00OOO()) frozen=null;
        if (frozen==null) {
            avatar.tickCount++;
            oOo0o0000OOOO0OooooO00oo();
            if (!o0ooooOo0o000OOo0oO00OoO() || OOOoOO000000o0o0oOooo0o0()==null) return false;
            var clip=OOOOo0O0oO0OOo0O0O0Oo0O0("death");
            // Native clip length and controller clock both use ticks; the Molang time query uses seconds.
            if (super.o0OOooo0o0OO00OoOOOo0o0O(0,false)==null) return false;
            float initialHeadHeight=headHeight();
            avatar.tickCount+=2;
            super.o0OOooo0o0OO00OoOOOo0o0O(0,false);
            avatar.tickCount+=clip==null?1:Math.max(1,(int)Math.ceil(clip.o0OOooo0o0OO00OoOOOo0o0O)+1);
            frozen=super.o0OOooo0o0OO00OoOOOo0o0O(0,false);
            if (frozen==null) return false;
            // A pack may supply an upright death clip. Lay that result down once as well.
            if (initialHeadHeight>.8F && headHeight()>initialHeadHeight*.8F) lieFlat=true;
        }
        if (lieFlat) {
            pose.translate(0,.26,-.9);
            pose.mulPose(Axis.XP.rotationDegrees(90));
        }
        Ysm265BackpackLayer.install();
        renderer.Oo0Oo0o00O00Oo0OOoOOoooo(this,0,0,pose,buffers,light);
        return true;
    }
}
