package com.r3ct.base_core;

import com.r3ct.base_core.client.screen.*;
import com.r3ct.base_core.config.BaseCoreClientConfig;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.logic.BaseCoreClientLogic;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.SyncCoreStatePayload;
import com.r3ct.base_core.network.SyncMailboxStatePayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
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

        @SubscribeEvent
        public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.BASE_CORE_MENU, BaseCoreScreen::new);
            event.register(ModMenuTypes.BASE_CORE_VISITOR_MENU, BaseCoreVisitorScreen::new);
            event.register(ModMenuTypes.ARCANE_LECTERN_MENU, ArcaneLecternScreen::new);

            event.register(ModMenuTypes.MAILBOX_VISITOR_MENU, MailboxVisitorScreen::new);
            event.register(ModMenuTypes.MAILBOX_OWNER_MENU, MailboxOwnerScreen::new);
        }
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientGameEvents {

        @SubscribeEvent
        public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
            BaseCoreServerConfig.load();
            BaseCoreClientLogic.clientHasCore = false;
            BaseCoreClientLogic.clientHasMailbox = false;
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentParticles event) {
            BaseCoreClientLogic.renderBorders(event.getPoseStack(), event.getLevelRenderState().cameraRenderState);
        }
    }

    public static class ClientPayloadHandlers {
        public static void handleConfigSync(ConfigSyncPayload payload) {
            BaseCoreServerConfig.syncFromServer(payload.serverJson());
        }

        public static void handleCoreStateSync(SyncCoreStatePayload payload) {
            BaseCoreClientLogic.handleCoreStateSync(payload);
        }

        public static void handleMailboxStateSync(SyncMailboxStatePayload payload) {
            BaseCoreClientLogic.handleMailboxStateSync(payload);
        }
    }
}