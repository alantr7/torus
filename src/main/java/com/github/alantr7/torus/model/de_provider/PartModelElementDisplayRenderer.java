package com.github.alantr7.torus.model.de_provider;

import org.bukkit.entity.Display;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class PartModelElementDisplayRenderer {

    public final Class<? extends Display> entityType;
    public final float[] offset;
    public final float[] scale;
    public final Quaternionf rotation;
    public final float viewRange;

    public PartModelElementDisplayRenderer(Class<? extends Display> entityType, Vector3f offset, Vector3f scale, float rotH, float rotV) {
        this.entityType = entityType;
        this.offset = new float[] { offset.x, offset.y, offset.z };
        this.scale = new float[] { scale.x, scale.y, scale.z };
        this.rotation = new Quaternionf()
          .rotateXYZ((float) Math.toRadians(rotV), (float) Math.toRadians(rotH), 0f);
        this.viewRange = 1f;
    }

    public PartModelElementDisplayRenderer(Class<? extends Display> entityType, float... data) {
        this.entityType = entityType;
        this.offset = new float[] { data[0], data[1], data[2] };
        this.scale = new float[] { data[3], data[4], data[5] };
        if (data.length < 8) {
            rotation = new Quaternionf();
            viewRange = 1f;
        } else {
            rotation = new Quaternionf()
              .rotateXYZ((float) Math.toRadians(data[6]), (float) Math.toRadians(data[7]), (float) Math.toRadians(data[8]));
            viewRange = data.length < 10 ? 1f : data[9];
        }
    }

}