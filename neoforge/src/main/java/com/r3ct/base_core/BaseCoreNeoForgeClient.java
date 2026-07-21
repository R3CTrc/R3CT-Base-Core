package com.r3ct.base_core;

import com.r3ct.base_core.client.screen.ConfigMainScreen;
import com.r3ct.base_core.config.BaseCoreClientConfig;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class BaseCoreNeoForgeClient {

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> new ConfigMainScreen(parent));
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            BaseCoreClientConfig.load();
        }
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientGameEvents {
        @SubscribeEvent
        public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            BaseCoreServerConfig.load();
        }
    }

    public static class ClientPayloadHandlers {
        public static void handleConfigSync(ConfigSyncPayload payload) {
            BaseCoreServerConfig.syncFromServer(payload.serverJson());
        }

        public static void handleOpenGui(OpenBaseCoreGuiPayload payload) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.r3ct.base_core.client.screen.BaseCoreScreen(payload));
        }
    }
}