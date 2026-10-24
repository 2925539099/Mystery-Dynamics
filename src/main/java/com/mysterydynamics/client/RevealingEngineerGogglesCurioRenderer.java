package com.mysterydynamics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** 饰品栏形态：只渲染头部，使用自定义的 128×64 护目镜贴图。 */
public final class RevealingEngineerGogglesCurioRenderer implements ICurioRenderer {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "mystery_dynamics", "textures/models/armor/revealing_engineer_goggles_layer_1.png");

    private HumanoidModel<LivingEntity> model;

    private HumanoidModel<LivingEntity> model() {
        if (model == null) {
            model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
                    .bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
            model.setAllVisible(false);
            model.head.visible = true;
            model.hat.visible = true;
        }
        return model;
    }

    @Override
    public void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
                       RenderLayerParent renderLayerParent, MultiBufferSource bufferSource,
                       int packedLight, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        HumanoidModel<LivingEntity> model = model();
        EntityModel<?> parentModel = renderLayerParent.getModel();
        if (parentModel instanceof HumanoidModel<?> parent) {
            copyProperties(parent, model);
        } else {
            ICurioRenderer.followHeadRotations(slotContext.entity(), model.head, model.hat);
        }
        model.setAllVisible(false);
        model.head.visible = true;
        model.hat.visible = true;

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.armorCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @SuppressWarnings("unchecked")
    private static void copyProperties(HumanoidModel<?> parent, HumanoidModel<LivingEntity> target) {
        ((HumanoidModel<LivingEntity>) parent).copyPropertiesTo(target);
    }
}
