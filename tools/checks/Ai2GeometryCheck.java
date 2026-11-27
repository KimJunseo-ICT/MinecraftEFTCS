package com.minecrafteft.client;

import org.joml.Vector3f;

/** Sweeps the hinge and tests needle contact in the skin mesh's local frame. */
public final class Ai2GeometryCheck {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (int frame = 0; frame <= 300; frame++) {
            float open = frame / 300F;
            for (float x : new float[]{-.146F,0,.146F}) {
                var hinge = Ai2Motion.lidPoint(open,x,Ai2Motion.HINGE_Y,Ai2Motion.HINGE_Z);
                check(hinge.distance(x,Ai2Motion.HINGE_Y,Ai2Motion.HINGE_Z) < .00001F, "top hinge drifts");
                for (float y : new float[]{-.168F,0,.168F}) {
                    for (float z : new float[]{.055F,.071F,.095F}) {
                        var point = Ai2Motion.lidPoint(open,x,y,z);
                        check(Math.abs(point.x-x) < .00001F, "lid swings sideways");
                        check(point.z >= .054F || point.y > .166F, "lid enters the tray");
                    }
                }
            }
            var edge = Ai2Motion.lidPoint(open,0,Ai2Motion.LATCH_Y,.071F);
            if (open == 1) check(edge.y > Ai2Motion.HINGE_Y+.15F, "open lid must sit above the case");
            if (open > 0 && open < 1) check(edge.z > .071F, "lower lip swings through the case");
        }
        for (int frame = 0; frame <= 42; frame++) {
            var p = Ai2Motion.sample(.68F + frame / 120F);
            var latch = Ai2Motion.lidPoint(p.lid(),.022F,Ai2Motion.LATCH_Y,.072F);
            p.kitRotation().transform(latch).add(p.kit());
            check(latch.distance(p.right()) < .00001F, "fingertips slip off the lower latch");
        }
        for (boolean slim : new boolean[]{false,true}) {
            for (boolean sleeve : new boolean[]{false,true}) {
                var stored = Ai2Motion.sample(1.55F,slim,sleeve);
                var local = stored.kitRotation().transformInverse(new Vector3f(stored.syringe()).sub(stored.kit()));
                check(local.distance(Ai2Motion.TRAY_X,Ai2Motion.TRAY_Y,Ai2Motion.TRAY_Z) < .00001F, "injector misses cradle");
                float radius = ((slim ? 1.5F : 2F) + (sleeve ? .25F : 0)) / 16F * Ai2Motion.ARM_SCALE;
                for (int frame = 0; frame <= 240; frame++) {
                    float t = 3 + frame / 120F;
                    var p = Ai2Motion.sample(t,slim,sleeve);
                    var tip = p.syringeRotation().transform(new Vector3f(-Ai2Motion.NEEDLE_LENGTH,0,0)).add(p.syringe());
                    var arm = Ai2Motion.armRotation(p.left(),true,false);
                    var armCenter = Ai2Motion.shoulder(true).lerp(p.left(),.76F);
                    var relative = arm.transformInverse(tip.sub(armCenter));
                    check(relative.distance(radius-.012F,0,0) < .00001F, "tip slides on arm during treatment");
                    var hub = p.syringeRotation().transform(new Vector3f(-.059F,0,0)).add(p.syringe());
                    check(arm.transformInverse(hub.sub(armCenter)).x > radius, "barrel penetrates arm");

                    var mirroredPalm = new Vector3f(-p.left().x,p.left().y,p.left().z);
                    var otherSide = Ai2Motion.armRotation(mirroredPalm,true,true).transform(new Vector3f(-1,0,0));
                    var axis = p.syringeRotation().transform(new Vector3f(1,0,0));
                    check(otherSide.distance(-axis.x,axis.y,axis.z)<.00001F, "offhand contact normal");
                }
                for (float t : new float[]{2.74F,5.21F}) {
                    var p = Ai2Motion.sample(t,slim,sleeve);
                    var tip = p.syringeRotation().transform(new Vector3f(-Ai2Motion.NEEDLE_LENGTH,0,0)).add(p.syringe());
                    var center = Ai2Motion.shoulder(true).lerp(p.left(),.76F);
                    check(Ai2Motion.armRotation(p.left(),true,false).transformInverse(tip.sub(center)).x > radius+.08F,
                            "approach or withdrawal clips forearm");
                }
            }
        }
        System.out.println("AI-2 hinge, extraction, approach, contact and withdrawal passed for both arms and skin widths.");
    }
}
