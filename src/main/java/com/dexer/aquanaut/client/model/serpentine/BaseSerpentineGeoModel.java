package com.dexer.aquanaut.client.model.serpentine;

import com.dexer.aquanaut.common.entity.serpentine.AbstractSerpentineEntity;
import com.dexer.aquanaut.common.entity.serpentine.SerpentineSegment;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public abstract class BaseSerpentineGeoModel<T extends AbstractSerpentineEntity & GeoEntity>
        extends GeoModel<T> {

    protected String segmentBonePrefix() {
        return "segment_";
    }

    @Override
    public void setCustomAnimations(T entity, long uniqueId, AnimationState<T> animationState) {
        super.setCustomAnimations(entity, uniqueId, animationState);
        driveSegmentBones(entity, animationState.getPartialTick());
    }

    protected void driveSegmentBones(T entity, float partialTick) {
        SerpentineSegment[] segs = entity.getSegments();
        if (segs.length == 0)
            return;

        float yawRad = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) * Mth.DEG_TO_RAD;
        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);

        double headX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double headY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double headZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        float prevLX = 0f, prevLY = 0f, prevLZ = 0f;

        for (int i = 0; i < segs.length; i++) {
            SerpentineSegment seg = segs[i];

            double segX = Mth.lerp(partialTick, seg.xo, seg.getX());
            double segY = Mth.lerp(partialTick, seg.yo, seg.getY()) + seg.getBbHeight() * 0.5D;
            double segZ = Mth.lerp(partialTick, seg.zo, seg.getZ());

            double relX = segX - headX;
            double relY = segY - headY;
            double relZ = segZ - headZ;

            float lX = (float) ((-cosYaw * relX - sinYaw * relZ) * 16.0);
            float lY = (float) (relY * 16.0);
            float lZ = (float) ((sinYaw * relX - cosYaw * relZ) * 16.0);

            float dX = lX - prevLX;
            float dY = lY - prevLY;
            float dZ = lZ - prevLZ;
            float hDist = (float) Math.sqrt(dX * dX + dZ * dZ);

            final float posX = -lX;
            final float posY = lY;
            final float posZ = lZ;

            final float rotY = (float) Math.atan2(dX, dZ);
            final float rotX = -(float) Math.atan2(dY, hDist);

            getBone(segmentBonePrefix() + i).ifPresent(bone -> {
                bone.setPosX(posX);
                bone.setPosY(posY);
                bone.setPosZ(posZ);
                bone.setRotY(rotY);
                bone.setRotX(rotX);
            });

            prevLX = lX;
            prevLY = lY;
            prevLZ = lZ;
        }
    }
}
