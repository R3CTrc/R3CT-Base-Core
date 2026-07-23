package com.r3ct.base_core;

import com.r3ct.base_core.block.ModBlocks;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BaseCoreFabric implements ModInitializer {

	public static final Item BASE_CORE_ITEM = new BlockItem(ModBlocks.BASE_CORE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"))));

	@Override
	public void onInitialize() {
		Constants.LOG.info("Starting Base Core system on Fabric!");

		BaseCoreServerConfig.load();

		Registry.register(BuiltInRegistries.BLOCK, ModBlocks.BASE_CORE_KEY, ModBlocks.BASE_CORE);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"), BASE_CORE_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.BASE_CORE_BE_KEY, ModBlocks.BASE_CORE_BE_TYPE);

		ResourceKey<CreativeModeTab> R3CT_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.parse(Constants.MOD_ID + ":main_tab"));

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, R3CT_TAB_KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.r3ct_base_core.main_tab"))
				.icon(() -> new ItemStack(BASE_CORE_ITEM))
				.displayItems((context, output) -> output.accept(BASE_CORE_ITEM))
				.build()
		);

		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpgradeBaseCorePayload.TYPE, UpgradeBaseCorePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UnlockEffectPayload.TYPE, UnlockEffectPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenBaseCoreGuiPayload.TYPE, OpenBaseCoreGuiPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(UpgradeBaseCorePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleUpgradeRequest(context.player(), payload);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(UnlockEffectPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleUnlockRequest(context.player(), payload);
			});
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			String serverJson = BaseCoreServerConfig.getServerConfigString();
			ServerPlayNetworking.send(player, new ConfigSyncPayload(serverJson));
		});
	}
}