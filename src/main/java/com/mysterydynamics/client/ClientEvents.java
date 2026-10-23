package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import thaumcraft.api.research.ResearchBookApi;

/** 客户端注册（专用服务器不会加载本类）。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MysteryDynamics.REDSTONE_ENGINE_BE.get(), RedstoneEngineRenderer::new);
        event.registerBlockEntityRenderer(MysteryDynamics.INFERNAL_BOILER_BE.get(), FluidTankRenderer::new);

        // 让 (1,3) 的引用节点直接显示炼金术“要素蒸馏”的原始页面，
        // (1,5) 的引用节点显示“炼狱锅炉”原始页面。
        ResearchBookApi.addPageAlias(key ->
                "ESSENTIADISTILLATION_LINK".equals(key) ? "DISTILESSENTIA"
                        : "INFERNAL_BOILER_LINK".equals(key) ? "INFERNAL_BOILER"
                        : key);
    }
}
