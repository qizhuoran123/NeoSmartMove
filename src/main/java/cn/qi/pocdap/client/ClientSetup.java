package cn.qi.pocdap.client;

import cn.qi.pocdap.client.key.ClientKeyConfig;
import cn.qi.pocdap.network.payload.ActionKeyPayload;
import cn.qi.pocdap.registers.NetworkRegister;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ClientSetup {
    public static void init(IEventBus modEventBus, ModContainer container) {
        // 模组事件
        modEventBus.addListener(ClientSetup::onKeyRegister);
        modEventBus.addListener(ClientSetup::onClientSetup); // 👈 移植过来的 Client Setup

        // 游戏事件
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);

        // 配置界面注册
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // 未来的客户端初始化逻辑（比如注册实体渲染器）可以写在这里
        System.out.println("HELLO FROM CLIENT SETUP in our clean architecture!");
    }

    private static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(ClientKeyConfig.GRAB_KEY);
        event.register(ClientKeyConfig.VAULT_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) return;

        while (ClientKeyConfig.GRAB_KEY.consumeClick()) {
            Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("抓取键被按下了！"), true);
            PacketDistributor.sendToServer(new ActionKeyPayload(0));
        }

        while (ClientKeyConfig.VAULT_KEY.consumeClick()) {
            Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("翻越键被按下了！"), true);
            PacketDistributor.sendToServer(new ActionKeyPayload(1));
        }
    }
}