package com.r3ct.base_core.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.r3ct.base_core.item.BlueprintItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class BlueprintHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private ItemStack offHandItem;

    @Shadow protected abstract float calculateMapTilt(float xRot);
    @Shadow protected abstract void renderMapHand(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm);
    @Shadow protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm);

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void renderBlueprintAsMap(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {

        if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlueprintItem) {

            poseStack.pushPose();

            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();

            if (isMainHand && this.offHandItem.isEmpty()) {
                float sqrtAttackValue = Mth.sqrt(attack);
                float ySwingPosition = -0.2F * Mth.sin(attack * (float) Math.PI);
                float zSwingPosition = -0.4F * Mth.sin(sqrtAttackValue * (float) Math.PI);
                poseStack.translate(0.0F, -ySwingPosition / 2.0F, zSwingPosition);

                float mapTilt = this.calculateMapTilt(xRot);
                poseStack.translate(0.0F, 0.04F + inverseArmHeight * -1.2F + mapTilt * -0.5F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(mapTilt * -85.0F));

                if (!this.minecraft.player.isInvisible()) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                    this.renderMapHand(poseStack, submitNodeCollector, lightCoords, HumanoidArm.RIGHT);
                    this.renderMapHand(poseStack, submitNodeCollector, lightCoords, HumanoidArm.LEFT);
                    poseStack.popPose();
                }

                float xzSwingRotation = Mth.sin(sqrtAttackValue * (float) Math.PI);
                poseStack.mulPose(Axis.XP.rotationDegrees(xzSwingRotation * 20.0F));
                poseStack.scale(2.0F, 2.0F, 2.0F);

                renderBlueprintTexture(poseStack, submitNodeCollector, lightCoords);

            } else {
                float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

                poseStack.translate(invert * 0.125F, -0.125F, 0.0F);

                if (!this.minecraft.player.isInvisible()) {
                    poseStack.pushPose();
                    poseStack.mulPose(Axis.ZP.rotationDegrees(invert * 10.0F));
                    this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight, attack, arm);
                    poseStack.popPose();
                }

                poseStack.pushPose();
                poseStack.translate(invert * 0.51F, -0.08F + inverseArmHeight * -1.2F, -0.75F);
                float sqrtAttackValue = Mth.sqrt(attack);
                float xSwing = Mth.sin(sqrtAttackValue * (float) Math.PI);
                float xSwingPosition = -0.5F * xSwing;
                float ySwingPosition = 0.4F * Mth.sin(sqrtAttackValue * (float) (Math.PI * 2));
                float zSwingPosition = -0.3F * Mth.sin(attack * (float) Math.PI);
                poseStack.translate(invert * xSwingPosition, ySwingPosition - 0.3F * xSwing, zSwingPosition);
                poseStack.mulPose(Axis.XP.rotationDegrees(xSwing * -45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(invert * xSwing * -30.0F));

                renderBlueprintTexture(poseStack, submitNodeCollector, lightCoords);
                poseStack.popPose();
            }
            poseStack.popPose();

            ci.cancel();
        }
    }

    private void renderBlueprintTexture(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(0.38F, 0.38F, 0.38F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);

        RenderType renderType = RenderTypes.text(Identifier.parse("r3ct_base_core:textures/item/r3ct_base_core_blueprint_background.png"));
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(lightCoords);
            buffer.addVertex(pose, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(lightCoords);
            buffer.addVertex(pose, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(lightCoords);
            buffer.addVertex(pose, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(lightCoords);
        });
    }
}