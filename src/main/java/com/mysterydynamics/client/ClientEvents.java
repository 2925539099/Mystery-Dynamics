package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

import thaumcraft.api.research.ResearchBookApi;

/** 客户端注册（专用服务器不会加载本类）。 */
@EventBusSubscriber(modid = MysteryDynamics.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    static {
        // 尽早注册别名，确保研究书构建页面时已经生效。
        ResearchBookApi.addPageAlias(key -> switch (key) {
            case "ESSENTIADISTILLATION_LINK" -> "DISTILESSENTIA";
            case "INFERNALFURNACE_LINK", "INFERNAL_BOILER_LINK" -> "INFERNALFURNACE";
            default -> key;
        });
    }

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MysteryDynamics.REDSTONE_ENGINE_BE.get(), RedstoneEngineRenderer::new);
        event.registerBlockEntityRenderer(MysteryDynamics.INFERNAL_BOILER_BE.get(),
                InfernalBoilerRenderer::new);
        NeoForge.EVENT_BUS.addListener(InfernalBoilerWorldRenderer::onRenderLevel);
    }
}
