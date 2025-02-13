package me.suff.mc.regen.client.rendering.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import me.suff.mc.regen.client.rendering.types.RenderTypes;
import me.suff.mc.regen.common.regen.RegenCap;
import me.suff.mc.regen.common.regen.transitions.TransitionTypeRenderers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class HandLayer extends RenderLayer {

    public HandLayer(RenderLayerParent entityRendererIn) {
        super(entityRendererIn);
    }

    public static void renderGlowingHands(LivingEntity livingEntity, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, HumanoidArm handSide) {
        RegenCap.get(livingEntity).ifPresent(iRegen -> {
            if (iRegen.glowing()) {
                Vec3 primaryColors = iRegen.getPrimaryColors();
                Vec3 secondaryColors = iRegen.getSecondaryColors();
                RenderRegenLayer.renderColorCone(matrixStackIn, bufferIn.getBuffer(RenderTypes.REGEN_FLAMES), packedLightIn, livingEntity, 0.5F, 0.5F, primaryColors);
                RenderRegenLayer.renderColorCone(matrixStackIn, bufferIn.getBuffer(RenderTypes.REGEN_FLAMES), packedLightIn, livingEntity, 0.7F, 0.7F, secondaryColors);
            }
        });
    }

    @Override
    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, Entity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        EntityModel<?> model = getParentModel();
        if (entitylivingbaseIn instanceof LivingEntity livingEntity) {
            RegenCap.get(livingEntity).ifPresent(iRegen -> {

                if (entitylivingbaseIn.isShiftKeyDown()) {
                    matrixStackIn.translate(0.0F, 0.2F, 0.0F);
                }

                HumanoidModel<?> bipedModel = (HumanoidModel) model;

                //For Regen Layers
                for (HumanoidArm handSide : HumanoidArm.values()) {
                    matrixStackIn.pushPose();
                    bipedModel.translateToHand(handSide, matrixStackIn);

                    renderGlowingHands((LivingEntity) entitylivingbaseIn, matrixStackIn, bufferIn, packedLightIn, handSide);
                    TransitionTypeRenderers.get(iRegen.transitionType()).thirdPersonHand(handSide, matrixStackIn, bufferIn, packedLightIn, livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
                    matrixStackIn.popPose();
                }
            });
        }
    }
}
