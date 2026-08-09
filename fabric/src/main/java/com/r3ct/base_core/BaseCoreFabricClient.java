package com.r3ct.base_core;

import com.r3ct.base_core.client.screen.ArcaneLecternScreen;
import com.r3ct.base_core.client.screen.BaseCoreScreen;
import com.r3ct.base_core.client.screen.BaseCoreVisitorScreen;
import com.r3ct.base_core.client.screen.ModMenuTypes;
import com.r3ct.base_core.config.BaseCoreClientConfig;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.logic.BaseCoreClientLogic;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.SyncCoreStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.gui.screens.MenuScreens;

public class BaseCoreFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		BaseCoreClientConfig.load();

		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				BaseCoreServerConfig.syncFromServer(payload.serverJson());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(SyncCoreStatePayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				BaseCoreClientLogic.handleCoreStateSync(payload);
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			BaseCoreServerConfig.load();
			BaseCoreClientLogic.clientHasCore = false;
		});

		LevelRenderEvents.END_MAIN.register(context -> {
			BaseCoreClientLogic.renderBorders(context.poseStack(), context.levelState().cameraRenderState);
		});

		MenuScreens.register(ModMenuTypes.BASE_CORE_MENU, BaseCoreScreen::new);
		MenuScreens.register(ModMenuTypes.BASE_CORE_VISITOR_MENU, BaseCoreVisitorScreen::new);
		MenuScreens.register(ModMenuTypes.ARCANE_LECTERN_MENU, ArcaneLecternScreen::new);
	}
}