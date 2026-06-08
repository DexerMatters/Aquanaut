package com.dexer.aquanaut.client.model;

public final class GasFlowMeterPoseHelper {
    private GasFlowMeterPoseHelper() {
    }

    public static Pose pose(boolean rightHanded) {
        float bodyYaw = rightHanded ? -0.08F : 0.08F;
        return new Pose(
                bodyYaw,
                -1.35F,
                bodyYaw + (rightHanded ? -0.20F : 0.20F),
                rightHanded ? -0.08F : 0.08F,
                -0.25F,
                rightHanded ? 0.35F : -0.35F,
                rightHanded ? 0.12F : -0.12F);
    }

    public record Pose(
            float bodyYRot,
            float gunArmXRot,
            float gunArmYRot,
            float gunArmZRot,
            float supportArmXRot,
            float supportArmYRot,
            float supportArmZRot) {
    }
}
