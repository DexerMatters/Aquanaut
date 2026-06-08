package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.client.model.GasFlowMeterPoseHelper;
import com.dexer.aquanaut.core.ItemRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidModel.class, remap = false)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"), remap = false)
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
        GasFlowMeterPoseHelper.Pose pose = GasFlowMeterPoseHelper.pose(arm == HumanoidArm.RIGHT);
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

        model.body.yRot = pose.bodyYRot();

        ModelPart gunArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        ModelPart supportArm = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;

        gunArm.xRot = pose.gunArmXRot();
        gunArm.yRot = pose.gunArmYRot();
        gunArm.zRot = pose.gunArmZRot();

        supportArm.xRot = pose.supportArmXRot();
        supportArm.yRot = pose.supportArmYRot();
        supportArm.zRot = pose.supportArmZRot();
    }
}
