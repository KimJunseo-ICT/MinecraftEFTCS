package com.minecrafteft.client;

import com.minecrafteft.medical.Ai2TreatmentProgress;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Ai2YsmMotion {
    // Move the open tray farther across the body to clear the working sleeve, then
    // turn the support forearm back towards the needle. Both tracks use the same use ticks.
    private static final float[][] CASE = {
            {0,      .245F,-.205F,-.73F, -18,-12,  8},
            {.22F,   .20F, -.18F, -.75F, -21,-16,  4},
            {.56F,  -.11F, -.145F,-.82F, -14,-12, -9},
            {.73F,  -.16F, -.125F,-.83F,  -9, -9,-12},
            {1.23F, -.195F,-.12F, -.85F,   7,-11, -9},
            {1.62F, -.18F, -.12F, -.84F,   8, -9, -7},
            {2.06F, -.145F,-.14F, -.83F,   7,-12,-10},
            {2.43F, -.005F,-.065F,-.84F,  29,-10,-19},
            {2.83F,  .175F, .06F, -.87F,  51, -8,-27},
            {3,      .19F,  .075F,-.87F,  54, -7,-28},
            {5,      .19F,  .075F,-.87F,  54, -7,-28},
            {5.24F,  .165F, .045F,-.86F,  48,-10,-21},
            {5.55F,  .07F, -.12F, -.80F,  21,-14, -8},
            {5.76F,  .24F, -.20F, -.73F, -17,-12,  8},
            {5.98F,  .30F,-1.02F, -.59F, -11, 13, 18},
            {6.04F,  .30F,-1.02F, -.59F, -11, 13, 18},
            {Ai2TreatmentProgress.READY_SECONDS, .245F,-.205F,-.73F, -18,-12,8}
    };

    private Ai2YsmMotion() {}

    static Ai2Motion.Pose sample(float seconds, float supportRadius) {
        return Ai2Motion.sample(seconds, supportRadius, CASE);
    }

    // YSM 2.6.5's Forge arm renderer translates by (+/- .25, 1.8, 0)
    // and flips X and Y before drawing its model in sixteenths of a block.
    private static Vector3f hookPoint(boolean left, Vector3f pivot) {
        return new Vector3f((left ? .25F : -.25F) - pivot.x / 16F, 1.8F - pivot.y / 16F, pivot.z / 16F);
    }

    record Arm(Vector3f palm, Quaternionf toVertical, float length, float widthScale) {
        static Arm fromPivots(boolean left, Vector3f shoulder, Vector3f hand) {
            if (shoulder == null || hand == null) return null;
            Vector3f palm = hookPoint(left, hand);
            Vector3f axis = new Vector3f(palm).sub(hookPoint(left, shoulder));
            float length = axis.length();
            if (!Float.isFinite(length) || length < .01F) return null;
            Quaternionf toVertical = new Quaternionf().rotationTo(axis.div(length), new Vector3f(0, 1, 0));
            return new Arm(palm, toVertical, length, Math.max(.45F, Math.min(1.15F, .45F / length)));
        }
    }

    record Arms(Arm left, Arm right) {
        Arm side(boolean physicalLeft) { return physicalLeft ? left : right; }
    }
}
