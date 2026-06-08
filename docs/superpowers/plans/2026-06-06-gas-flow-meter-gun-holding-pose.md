# Gas Flow Meter Gun Holding Pose Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the broken vanilla custom arm-pose attempt with a real gas-flow-meter gun-holding pose that works in first person and third person.

**Architecture:** Keep first-person behavior on the item through `IClientItemExtensions.applyForgeHandTransform(...)`, because NeoForge still supports that hook cleanly. Move third-person posing into a client mixin on `HumanoidModel.setupAnim(...)` so the pose is applied after vanilla arm animation instead of being overwritten by it.

**Tech Stack:** NeoForge 21.0.167, Sponge Mixin, Java 21, direct Java `main(...)` regression tests

---

### Task 1: Add a Regression Test for the Pose Math

**Files:**
- Create: `src/test/java/com/dexer/aquanaut/client/model/GasFlowMeterPoseHelperTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.dexer.aquanaut.client.model;

import net.minecraft.world.entity.HumanoidArm;

public final class GasFlowMeterPoseHelperTest {
    public static void main(String[] args) {
        GasFlowMeterPoseHelperTest test = new GasFlowMeterPoseHelperTest();
        test.rightHandPoseKeepsMainArmRaisedAndSupportArmTucked();
        test.leftHandPoseMirrorsTheRightHandPose();
    }

    private void rightHandPoseKeepsMainArmRaisedAndSupportArmTucked() {
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(HumanoidArm.RIGHT);

        assertClose(-0.08F, pose.bodyYRot(), "right body yaw");
        assertClose(-1.35F, pose.gunArmXRot(), "right gun arm xRot");
        assertClose(-0.28F, pose.gunArmYRot(), "right gun arm yRot");
        assertClose(-0.08F, pose.gunArmZRot(), "right gun arm zRot");
        assertClose(-0.25F, pose.supportArmXRot(), "right support arm xRot");
        assertClose(0.35F, pose.supportArmYRot(), "right support arm yRot");
        assertClose(0.12F, pose.supportArmZRot(), "right support arm zRot");
    }

    private void leftHandPoseMirrorsTheRightHandPose() {
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(HumanoidArm.LEFT);

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
```

- [ ] **Step 2: Run test compile to verify it fails**

Run: `./gradlew compileTestJava`
Expected: FAIL because `GasFlowMeterPoseHelper` does not exist yet

### Task 2: Implement the Pose Helper and Item First-Person Transform

**Files:**
- Create: `src/main/java/com/dexer/aquanaut/client/model/GasFlowMeterPoseHelper.java`
- Modify: `src/main/java/com/dexer/aquanaut/common/item/GasFlowMeterItem.java`

- [ ] **Step 1: Write minimal implementation for the tested pose math**

```java
package com.dexer.aquanaut.client.model;

import net.minecraft.world.entity.HumanoidArm;

public final class GasFlowMeterPoseHelper {
    private GasFlowMeterPoseHelper() {
    }

    public static Pose pose(HumanoidArm arm) {
        float bodyYaw = arm == HumanoidArm.RIGHT ? -0.08F : 0.08F;
        return new Pose(
                bodyYaw,
                -1.35F,
                bodyYaw + (arm == HumanoidArm.RIGHT ? -0.20F : 0.20F),
                arm == HumanoidArm.RIGHT ? -0.08F : 0.08F,
                -0.25F,
                arm == HumanoidArm.RIGHT ? 0.35F : -0.35F,
                arm == HumanoidArm.RIGHT ? 0.12F : -0.12F);
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
```

- [ ] **Step 2: Replace the broken `ArmPose.create(...)` path**

```java
@Override
public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
            return null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm,
                ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
            if (!isUsingInArm(player, arm, itemInHand)) {
                return false;
            }

            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            poseStack.translate(side * 0.02F, -0.10F, -0.08F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 7.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * -2.5F));
            return false;
        }
    });
}
```

- [ ] **Step 3: Re-run the test compile**

Run: `./gradlew compileTestJava`
Expected: PASS

### Task 3: Apply the Third-Person Gun Holding Pose After Vanilla Setup

**Files:**
- Create: `src/main/java/com/dexer/aquanaut/mixin/HumanoidModelMixin.java`
- Modify: `src/main/resources/aquanaut.mixins.json`

- [ ] **Step 1: Add a tail mixin on `HumanoidModel.setupAnim(...)`**

```java
@Mixin(value = HumanoidModel.class, remap = false)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Shadow public ModelPart body;
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void aquanaut$applyGasFlowMeterGunHoldingPose(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!entity.isUsingItem()) {
            return;
        }

        ItemStack stack = entity.getUseItem();
        if (!stack.is(ItemRegistry.GAS_FLOW_METER.get())) {
            return;
        }

        HumanoidArm arm = entity.getUsedItemHand() == InteractionHand.MAIN_HAND
                ? entity.getMainArm()
                : entity.getMainArm().getOpposite();
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(arm);
        this.body.yRot = pose.bodyYRot();
        // assign gun arm and support arm rotations here
    }
}
```

- [ ] **Step 2: Register the new client mixin**

```json
"client": [
  "CreativeModeInventoryScreenMixin",
  "ItemRendererMixin",
  "BufferSourceMixin",
  "HarpoonRendererMixin",
  "HumanoidModelMixin"
]
```

### Task 4: Verify the Build and the New Regression Test

**Files:**
- Verify only

- [ ] **Step 1: Run the pose regression test**

Run: `java -cp build/classes/java/main:build/classes/java/test com.dexer.aquanaut.client.model.GasFlowMeterPoseHelperTest`
Expected: no output, exit code 0

- [ ] **Step 2: Run full Java compile verification**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`
