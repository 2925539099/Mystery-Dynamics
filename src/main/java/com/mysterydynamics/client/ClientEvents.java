package com.mysterydynamics.client;

import com.mysterydynamics.MysteryDynamics;

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

        // 让 (1,3) 的引用节点直接显示炼金术“要素蒸馏”的原始页面，
        // 相当于在神秘动力学页里开了一个分身，而不是单独写一套内容。
        ResearchBookApi.addPageAlias(key ->
                "ESSENTIADISTILLATION_LINK".equals(key) ? "DISTILESSENTIA" : key);
    }
}
