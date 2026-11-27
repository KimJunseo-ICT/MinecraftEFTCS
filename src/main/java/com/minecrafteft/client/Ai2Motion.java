package com.minecrafteft.client;

import com.minecrafteft.medical.Ai2TreatmentProgress;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The entire performance is sampled from use time, including on the first frame after a hand switch. */
public final class Ai2Motion {
    static final float HINGE_Y = .173F, HINGE_Z = .057F;
    private static final float OPEN_DEGREES = -132;
    static final float LATCH_Y = -.168F;
    static final float NEEDLE_LENGTH = .137F;
    static final float TRAY_X = .012F, TRAY_Y = .039F, TRAY_Z = .040F;
    static final float ARM_SCALE = .65F;
    private static final float RAD = (float) (Math.PI / 180);

    public record Pose(Vector3f left, Vector3f right, Vector3f kit, Quaternionf kitRotation,
                       float lid, Vector3f syringe, Quaternionf syringeRotation,
                       float plunger, boolean syringeVisible) {}

    // Seconds, case position, then yaw, pitch and roll in degrees. Slopes are limited
    // at reversals and holds so the shell settles without drifting through its grip.
    private static final float[][] CASE = {
            {0,     .27F,-.175F,-.70F, -14,-10, 11},
            {.22F,  .22F,-.164F,-.71F, -18,-13,  7},
            {.56F, -.09F,-.14F,-.78F, -12,-10, -6},
            {.73F, -.13F,-.12F,-.79F,  -9, -8, -9},
            {.98F, -.15F,-.14F,-.81F,   1,-13,-11},
            {1.26F,-.16F,-.12F,-.82F,   9,-10, -6},
            {1.52F,-.15F,-.12F,-.81F,   7, -8, -5},
            {1.90F,-.14F,-.13F,-.81F,   4,-11, -7},
            {2.13F,-.10F,-.12F,-.81F,  13,-15, -9},
            {2.43F, .03F,-.04F,-.81F,  34, -8,-16},
            {2.78F, .20F, .09F,-.84F,  54, -5,-23},
            {3,     .22F, .10F,-.85F,  57, -4,-24},
            {5,     .22F, .10F,-.85F,  57, -4,-24},
            {5.24F, .19F, .07F,-.84F,  49, -8,-18},
            {5.55F, .09F,-.10F,-.77F,  25,-12, -6},
            {5.73F, .245F,-.16F,-.70F,-10,-12,  8},
            {5.78F, .26F,-.175F,-.69F,-13,-10, 10},
            {5.98F, .32F,-.91F,-.57F, -9, 10, 16},
            {6.04F, .32F,-.91F,-.57F, -9, 10, 16},
            {Ai2TreatmentProgress.READY_SECONDS, .27F,-.175F,-.70F,-14,-10,11}
    };

    private Ai2Motion() {}

    public static Pose sample(float seconds) { return sample(seconds, false, true); }

    static Pose sample(float seconds, boolean slim, boolean sleeve) {
        return sample(seconds, skinRadius(slim, sleeve), CASE);
    }

    static float skinRadius(boolean slim, boolean sleeve) {
        return ((slim ? 1.5F : 2F) + (sleeve ? .25F : 0)) / 16F * ARM_SCALE;
    }

    static Pose sample(float seconds, float forearmRadius, float[][] caseTrack) {
        float t = Float.isFinite(seconds) ? Math.max(0, Math.min(Ai2TreatmentProgress.READY_SECONDS, seconds)) : 0;
        int i = 0;
        while (i < caseTrack.length - 2 && t > caseTrack[i + 1][0]) i++;
        Vector3f kit = new Vector3f(channel(caseTrack, i, 1, t), channel(caseTrack, i, 2, t), channel(caseTrack, i, 3, t));
        float breath = (float) Math.sin((t - 3) * Math.PI) * .004F
                * ease(2.9F, 3.3F, t) * (1 - ease(4.7F, 5, t));
        kit.y += breath;
        Quaternionf rotation = new Quaternionf().rotationYXZ(
                channel(caseTrack, i, 4, t) * RAD, channel(caseTrack, i, 5, t) * RAD, channel(caseTrack, i, 6, t) * RAD);

        float lid = ease(.68F, 1.20F, t) * (1 - ease(2.05F, 2.47F, t));
        Vector3f support = local(kit, rotation, -.068F, -.097F, -.030F);
        Vector3f parked = new Vector3f(-.46F, -.68F, -.43F);
        support.lerp(parked, 1 - ease(.03F, .53F, t) + ease(5.64F, 5.85F, t));

        Vector3f idleGrip = local(kit, rotation, .149F, -.115F, .006F);
        Vector3f latch = lidPoint(lid, .022F, LATCH_Y, .072F);
        rotation.transform(latch).add(kit);
        // Positive local X runs from needle to thumb. The red barrel rises clear
        // of the rim before the wrist turns or the supporting hand closes the case.
        float lift = ease(1.64F, 1.87F, t);
        float draw = ease(1.76F, 2.30F, t);
        Vector3f free = local(kit, rotation, TRAY_X, TRAY_Y, TRAY_Z + .096F * lift);
        free.lerp(new Vector3f(.247F, -.109F, -.56F), draw);
        Quaternionf freeRotation = new Quaternionf(rotation).slerp(
                new Quaternionf().rotationYXZ(-.28F, .14F, .30F), draw);

        // The target is a point on the working-hand-facing side of the actual skin arm.
        // Its surface, shaft and hand share the arm's roll, scale and breathing motion.
        Vector3f shoulder = shoulder(true);
        Quaternionf supportRotation = armRotation(support, true, false);
        Vector3f outward = supportRotation.transform(new Vector3f(1, 0, 0));
        Vector3f surface = shoulder.lerp(support, .76F).fma(forearmRadius, outward);
        float gap = .104F * (1 - ease(2.73F, 2.97F, t)) - .012F * ease(2.95F, 3, t)
                + .145F * ease(5, 5.23F, t);
        Vector3f aimed = surface.fma(NEEDLE_LENGTH + gap, outward);
        float aim = ease(2.23F, 2.72F, t);
        Vector3f syringe = free.lerp(aimed, aim);
        Quaternionf syringeRotation = freeRotation.slerp(supportRotation, aim);

        float flick = ease(5.23F, 5.52F, t);
        syringe.lerp(new Vector3f(.31F, -.12F, -.53F), flick);
        Quaternionf releasedRotation = new Quaternionf().rotationYXZ(-.22F,.18F,.63F);
        syringeRotation.slerp(releasedRotation, flick);
        float flight = Math.max(0, t - 5.52F);
        syringe.add(2.7F * flight, .8F * flight - 3.5F * flight * flight, -.7F * flight);
        syringeRotation.rotateZ(11 * flight);

        Vector3f right = new Vector3f(idleGrip).lerp(latch, ease(.32F, .68F, t));
        // Release the rising lower edge, then bring the hand around the side of the open tray.
        right.lerp(local(kit, rotation, .195F, .125F, .150F), ease(1.03F, 1.31F, t));
        Vector3f grip = syringeGrip(syringe, syringeRotation, draw);
        right.lerp(grip, ease(1.22F, 1.61F, t));
        if (t > 5.52F) {
            Vector3f releasedHand = syringeGrip(new Vector3f(.31F, -.12F, -.53F), releasedRotation, 1);
            releasedHand.add(.036F * (float) Math.sin(Math.PI * ease(5.52F, 5.78F, t)), 0, 0);
            right = releasedHand.lerp(idleGrip, ease(5.55F, 5.76F, t));
        }
        return new Pose(support, right, kit, rotation, lid, syringe, syringeRotation,
                ease(3, 5, t), t < 5.95F && (lid > 0 || lift > 0));
    }

    private static Vector3f syringeGrip(Vector3f syringe, Quaternionf rotation, float draw) {
        Vector3f grip = rotation.transform(new Vector3f(.040F * draw, 0, 0)).add(syringe);
        Vector3f front = armRotation(grip, false, false).transform(new Vector3f(0,0,1));
        // Let the barrel sit along the camera-facing edge of the fist as it turns out of the cradle.
        grip.fma(-.083F * draw, front);
        return grip.fma(-.012F, new Vector3f(grip).sub(shoulder(false)).normalize());
    }

    static Vector3f shoulder(boolean support) {
        return new Vector3f(support ? -.68F : .68F, -.55F, .045F);
    }

    static Quaternionf armRotation(Vector3f palm, boolean support, boolean mirror) {
        Vector3f origin = shoulder(support);
        if (mirror) origin.x = -origin.x;
        Vector3f direction = new Vector3f(palm).sub(origin).normalize();
        float roll = (support ? -17 : 14) * (mirror ? -1 : 1) * RAD;
        return new Quaternionf().rotationTo(0, 1, 0, direction.x, direction.y, direction.z).rotateY(roll);
    }

    static Quaternionf lidRotation(float open) {
        return new Quaternionf().rotationX(OPEN_DEGREES * RAD * open);
    }

    static Vector3f lidPoint(float open, float x, float y, float z) {
        Vector3f offset = new Vector3f(x, y - HINGE_Y, z - HINGE_Z);
        return lidRotation(open).transform(offset).add(0, HINGE_Y, HINGE_Z);
    }

    private static Vector3f local(Vector3f origin, Quaternionf rotation, float x, float y, float z) {
        return rotation.transform(new Vector3f(x, y, z)).add(origin);
    }

    private static float ease(float start, float end, float time) {
        float u = Math.max(0, Math.min(1, (time - start) / (end - start)));
        return u * u * (3 - 2 * u);
    }

    static float channel(float[][] track, int i, int c, float t) {
        float[] a = track[i], b = track[i + 1];
        float dt = b[0] - a[0], u = (t - a[0]) / dt, u2 = u * u;
        return a[c] + (b[c] - a[c]) * u2 * (3 - 2 * u)
                + dt * u * (1 - u) * ((1 - u) * tangent(track, i, c) - u * tangent(track, i + 1, c));
    }

    private static float tangent(float[][] track, int i, int c) {
        if (i == 0 || i == track.length - 1) return 0;
        float[] a = track[i - 1], b = track[i], d = track[i + 1];
        float before = (b[c] - a[c]) / (b[0] - a[0]);
        float after = (d[c] - b[c]) / (d[0] - b[0]);
        return before * after <= 0 ? 0 : 2 * before * after / (before + after);
    }
}
