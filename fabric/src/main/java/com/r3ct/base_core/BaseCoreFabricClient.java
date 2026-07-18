package com.r3ct.base_core;

import com.r3ct.base_core.config.BaseCoreClientConfig;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class BaseCoreFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		BaseCoreClientConfig.load();

		ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				BaseCoreServerConfig.syncFromServer(payload.serverJson());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenBaseCoreGuiPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				net.minecraft.client.Minecraft.getInstance().setScreen(new com.r3ct.base_core.client.screen.BaseCoreScreen(payload));
			});
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			BaseCoreServerConfig.load();
		});
	}
}