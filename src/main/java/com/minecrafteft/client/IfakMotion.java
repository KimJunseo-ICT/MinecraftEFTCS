package com.minecrafteft.client;

import com.minecrafteft.medical.MedikitType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class IfakMotion {
    enum Variant { WRAP, INJECT }
    record Pose(Vector3f left, Vector3f right, Vector3f pouch, Quaternionf rotation, float open,
                Vector3f tool, Quaternionf toolRotation, boolean toolVisible, Vector3f contact,
                Quaternionf armRotation, float treatment, float wrapCoverage) {}

    static final float NEEDLE = .105F;
    static final float POUCH_HINGE_Y = -.151F;
    private static final float RAD = (float)Math.PI / 180F;
    private static final float START = MedikitType.IFAK.prepareTicks / 20F;
    private static final float END = START + MedikitType.IFAK.healTicks / 20F;
    // Seconds, case centre, yaw, pitch and roll. The open face follows the support hand.
    private static final float[][] CARRIER = {
            {0, .25F,-.19F,-.70F, -18,-12,9}, {.36F,.12F,-.18F,-.73F, -14,-15,4},
            {.68F,-.07F,-.10F,-.78F, -10,-17,-6}, {1.24F,-.14F,-.12F,-.79F, 4,-23,-10},
            {1.6F,-.12F,-.12F,-.79F, 8,-18,-5}, {1.92F,-.08F,-.15F,-.79F, 12,-14,-8},
            {START,.18F,.065F,-.84F, 29,-9,-21}, {END,.18F,.065F,-.84F, 29,-9,-21},
            {5.72F,-.08F,-.15F,-.78F, 1,-14,-8}, {5.99F,.22F,-.21F,-.72F, -16,-12,8},
            {6.18F,.31F,-.99F,-.60F, -8,12,15}, {6.24F,.31F,-.99F,-.60F, -8,12,15},
            {MedikitType.IFAK.readySeconds(),.25F,-.19F,-.70F, -18,-12,9}
    };

    private IfakMotion() {}

    static Pose sample(float seconds, Variant variant, float radius, boolean ysm) {
        float t = Math.max(0, Math.min(MedikitType.IFAK.readySeconds(), seconds));
        int i = 0;
        while (i < CARRIER.length - 2 && t > CARRIER[i+1][0]) i++;
        Vector3f pouch = new Vector3f(channel(i,1,t), channel(i,2,t), channel(i,3,t));
        boolean wrap = variant == Variant.WRAP;
        Quaternionf rotation = new Quaternionf().rotationYXZ(channel(i,4,t)*RAD,channel(i,5,t)*RAD,channel(i,6,t)*RAD);
        Vector3f supportGrip = rotation.transform(new Vector3f(-.115F,-.045F,.005F));
        if (wrap) {
            // Carry the pouch into the wrapping pose with the same point in the palm.
            Vector3f heldPouch = new Vector3f(.19F,-.19F,-.65F).sub(supportGrip);
            pouch.lerp(heldPouch,ease(1.99F,START,t)*(1-ease(END,5.79F,t)));
        }
        if (ysm) pouch.add(-.022F,-.018F,-.035F);
        float breathing = (float)Math.sin((t-START)*Math.PI) * .005F * ease(2.4F,2.7F,t) * (1-ease(5.1F,END,t));
        pouch.y += breathing;
        float open = ease(.58F,1.22F,t) * (1-ease(5.64F,5.96F,t));
        Vector3f left = new Vector3f(pouch).add(supportGrip);
        Vector3f parked = new Vector3f(-.48F,-.72F,-.40F);
        left.lerp(parked, 1-ease(.04F,.49F,t)+ease(5.84F,6.09F,t));
        Quaternionf arm = Ai2Motion.armRotation(left,true,false);
        float progress = Math.max(0,Math.min(1,(t-START)/(END-START)));
        Vector3f contact = Ai2Motion.shoulder(true).lerp(left, wrap ? .70F + .065F*progress : .73F);
        Vector3f grip = local(pouch,rotation,.115F,-.07F,.025F);
        var zip = new Vector3f(.107F-.214F*open,.148F-POUCH_HINGE_Y,.052F-.024F)
                .rotateX(24F*RAD*open).add(0,POUCH_HINGE_Y,.024F);
        rotation.transform(zip).add(pouch);
        Vector3f mouth = local(pouch,rotation,-.04F,.115F,.027F);
        Vector3f right = new Vector3f(grip).lerp(zip,ease(.27F,.60F,t));
        right.lerp(mouth,ease(1.16F,1.47F,t));

        Vector3f tool = new Vector3f(mouth).lerp(new Vector3f(.235F,-.055F,-.59F),ease(1.48F,1.90F,t));
        Quaternionf toolRotation = new Quaternionf(rotation).slerp(new Quaternionf().rotationZ(-1.28F),ease(1.51F,1.94F,t));
        float aim = ease(1.94F,START,t);
        if (wrap) {
            float angle = (float)(Math.PI*6) * progress;
            float x = (float)Math.cos(angle), z = (float)Math.sin(angle);
            float distance = radius / Math.max(Math.abs(x),Math.abs(z)) + .032F;
            Vector3f orbit = new Vector3f(x*distance,0,z*distance);
            tool.lerp(arm.transform(orbit).add(contact),aim);
            toolRotation.slerp(new Quaternionf(arm).rotateY(-angle),aim);
        } else {
            Vector3f outward = arm.transform(new Vector3f(1,0,0));
            tool.lerp(new Vector3f(contact).fma(radius+NEEDLE-.008F, outward),aim);
            toolRotation.slerp(arm,aim);
        }
        float withdraw = ease(END,5.66F,t);
        tool.lerp(new Vector3f(.34F,-.19F,-.55F),withdraw);
        toolRotation.slerp(new Quaternionf().rotationZ(-.62F),withdraw);
        Vector3f fingers = local(tool,toolRotation,wrap ? .045F : .042F,-.018F,-.024F);
        right.lerp(fingers,ease(1.38F,1.55F,t));
        // Drop the spent supply below frame, then return the hand to the zipper and grip.
        tool.y -= .58F*ease(5.66F,5.9F,t);
        right.lerp(zip,ease(5.63F,5.81F,t)).lerp(grip,ease(5.83F,6.01F,t));
        return new Pose(left,right,pouch,rotation,open,tool,toolRotation,t>=1.46F && t<5.91F,
                contact,arm,progress,wrap ? ease(START,END,t)*(1-ease(5.55F,5.86F,t)) : 0);
    }

    private static float channel(int i,int c,float t) { return Ai2Motion.channel(CARRIER,i,c,t); }
    private static Vector3f local(Vector3f origin,Quaternionf rotation,float x,float y,float z) {
        return rotation.transform(new Vector3f(x,y,z)).add(origin);
    }
    private static float ease(float a,float b,float t) {
        float u=Math.max(0,Math.min(1,(t-a)/(b-a)));
        return u*u*(3-2*u);
    }
}
