package com.dexer.aquanaut.client.model;

public final class GasFlowMeterTargetingHelperTest {
    public static void main(String[] args) {
        GasFlowMeterTargetingHelperTest test = new GasFlowMeterTargetingHelperTest();
        test.rightHandTargetingStaysNearCenterWithForwardBias();
        test.leftHandTargetingMirrorsTheRightHandPose();
    }

    private void rightHandTargetingStaysNearCenterWithForwardBias() {
        GasFlowMeterTargetingHelper.Transform transform = GasFlowMeterTargetingHelper.targeting(true);

        assertClose(0.14F, transform.translateX(), "right translate x");
        assertClose(-0.18F, transform.translateY(), "right translate y");
        assertClose(-0.58F, transform.translateZ(), "right translate z");
        assertClose(18.0F, transform.yawDegrees(), "right yaw");
        assertClose(-4.0F, transform.pitchDegrees(), "right pitch");
        assertClose(-3.0F, transform.rollDegrees(), "right roll");
        assertClose(0.90F, transform.scale(), "right scale");
    }

    private void leftHandTargetingMirrorsTheRightHandPose() {
        GasFlowMeterTargetingHelper.Transform transform = GasFlowMeterTargetingHelper.targeting(false);

        assertClose(-0.14F, transform.translateX(), "left translate x");
        assertClose(-0.18F, transform.translateY(), "left translate y");
        assertClose(-0.58F, transform.translateZ(), "left translate z");
        assertClose(-18.0F, transform.yawDegrees(), "left yaw");
        assertClose(-4.0F, transform.pitchDegrees(), "left pitch");
        assertClose(3.0F, transform.rollDegrees(), "left roll");
        assertClose(0.90F, transform.scale(), "left scale");
    }

    private void assertClose(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001F) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
