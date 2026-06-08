package com.dexer.aquanaut.client.model;

public final class GasFlowMeterPoseHelperTest {
    public static void main(String[] args) {
        GasFlowMeterPoseHelperTest test = new GasFlowMeterPoseHelperTest();
        test.rightHandPoseKeepsMainArmRaisedAndSupportArmTucked();
        test.leftHandPoseMirrorsTheRightHandPose();
    }

    private void rightHandPoseKeepsMainArmRaisedAndSupportArmTucked() {
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(true);

        assertClose(-0.08F, pose.bodyYRot(), "right body yaw");
        assertClose(-1.35F, pose.gunArmXRot(), "right gun arm xRot");
        assertClose(-0.28F, pose.gunArmYRot(), "right gun arm yRot");
        assertClose(-0.08F, pose.gunArmZRot(), "right gun arm zRot");
        assertClose(-0.25F, pose.supportArmXRot(), "right support arm xRot");
        assertClose(0.35F, pose.supportArmYRot(), "right support arm yRot");
        assertClose(0.12F, pose.supportArmZRot(), "right support arm zRot");
    }

    private void leftHandPoseMirrorsTheRightHandPose() {
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(false);

        assertClose(0.08F, pose.bodyYRot(), "left body yaw");
        assertClose(-1.35F, pose.gunArmXRot(), "left gun arm xRot");
        assertClose(0.28F, pose.gunArmYRot(), "left gun arm yRot");
        assertClose(0.08F, pose.gunArmZRot(), "left gun arm zRot");
        assertClose(-0.25F, pose.supportArmXRot(), "left support arm xRot");
        assertClose(-0.35F, pose.supportArmYRot(), "left support arm yRot");
        assertClose(-0.12F, pose.supportArmZRot(), "left support arm zRot");
    }

    private void assertClose(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001F) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
