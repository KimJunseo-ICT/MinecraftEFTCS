package com.minecrafteft.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.StringJoiner;

/** Offline silhouette input, sampled directly from the in-game rig. */
public final class Ai2PreviewPoses {
    private static void add(StringJoiner row, Vector3f v) {
        row.add(Float.toString(v.x)).add(Float.toString(v.y)).add(Float.toString(v.z));
    }

    private static void axes(StringJoiner row, Quaternionf q) {
        add(row, q.transform(new Vector3f(1,0,0)));
        add(row, q.transform(new Vector3f(0,1,0)));
        add(row, q.transform(new Vector3f(0,0,1)));
    }

    public static void main(String[] args) {
        for (String arg : args) {
            var p = Ai2Motion.sample(Float.parseFloat(arg));
            var row = new StringJoiner(" ");
            add(row,p.left()); add(row,p.right());
            add(row,Ai2Motion.shoulder(true)); add(row,Ai2Motion.shoulder(false));
            axes(row,Ai2Motion.armRotation(p.left(),true,false));
            axes(row,Ai2Motion.armRotation(p.right(),false,false));
            add(row,p.kit()); axes(row,p.kitRotation());
            var lidCenter = Ai2Motion.lidPoint(p.lid(),0,0,.063F);
            add(row,p.kitRotation().transform(lidCenter).add(p.kit()));
            axes(row,new Quaternionf(p.kitRotation()).mul(Ai2Motion.lidRotation(p.lid())));
            add(row,p.syringe()); axes(row,p.syringeRotation());
            row.add(p.syringeVisible() ? "1" : "0");
            System.out.println(row);
        }
    }
}
