package com.r3ct.base_core;

import com.r3ct.base_core.block.ModBlocks;
import com.r3ct.base_core.client.screen.ModMenuTypes;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.item.EmpoweredTomeItem;
import com.r3ct.base_core.network.*;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import com.r3ct.base_core.item.BlueprintItem;
import com.r3ct.base_core.registry.ModDataComponents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class BaseCoreFabric implements ModInitializer {

	public static final Item BASE_CORE_ITEM = new com.r3ct.base_core.item.BaseCoreBlockItem(ModBlocks.BASE_CORE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"))));
	public static final Item BLUEPRINT_ITEM = new BlueprintItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint")))
			.stacksTo(16)
			.component(DataComponents.LORE, new ItemLore(List.of(
					Component.translatable("item.r3ct_base_core.blueprint.desc").withStyle(ChatFormatting.GRAY)
			)))
	);

	public static final Item ARCANE_LECTERN_ITEM = new BlockItem(ModBlocks.ARCANE_LECTERN, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":arcane_lectern"))));
	public static final Item MAGIC_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":magic_tome"))));
	public static final Item DARK_MAGIC_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":dark_magic_tome"))));
	public static final Item ALCHEMY_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":alchemy_tome"))));
	public static final Item EMPOWERED_TOME = new EmpoweredTomeItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":empowered_tome"))));

	@Override
	public void onInitialize() {
		Constants.LOG.info("Starting Base Core system on Fabric!");

		ModDataComponents.init();
		BaseCoreServerConfig.load();

		Registry.register(BuiltInRegistries.BLOCK, ModBlocks.BASE_CORE_KEY, ModBlocks.BASE_CORE);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"), BASE_CORE_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.BASE_CORE_BE_KEY, ModBlocks.BASE_CORE_BE_TYPE);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint"), BLUEPRINT_ITEM);
		Registry.register(BuiltInRegistries.MENU, Identifier.parse(Constants.MOD_ID + ":base_core_menu"), ModMenuTypes.BASE_CORE_MENU);

		Registry.register(BuiltInRegistries.BLOCK, ModBlocks.ARCANE_LECTERN_KEY, ModBlocks.ARCANE_LECTERN);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":arcane_lectern"), ARCANE_LECTERN_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.ARCANE_LECTERN_BE_KEY, ModBlocks.ARCANE_LECTERN_BE_TYPE);
		Registry.register(BuiltInRegistries.MENU, Identifier.parse(Constants.MOD_ID + ":arcane_lectern_menu"), ModMenuTypes.ARCANE_LECTERN_MENU);

		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":magic_tome"), MAGIC_TOME);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":dark_magic_tome"), DARK_MAGIC_TOME);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":alchemy_tome"), ALCHEMY_TOME);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":empowered_tome"), EMPOWERED_TOME);

		ResourceKey<CreativeModeTab> R3CT_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.parse(Constants.MOD_ID + ":main_tab"));

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, R3CT_TAB_KEY, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.r3ct_base_core.main_tab"))
				.icon(() -> new ItemStack(BASE_CORE_ITEM))
				.displayItems((context, output) -> {
					output.accept(BASE_CORE_ITEM);
					output.accept(BLUEPRINT_ITEM);
					output.accept(ARCANE_LECTERN_ITEM);
					output.accept(MAGIC_TOME);
					output.accept(DARK_MAGIC_TOME);
					output.accept(ALCHEMY_TOME);
					output.accept(EMPOWERED_TOME);
				})
				.build()
		);

		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(UpgradeBaseCorePayload.TYPE, UpgradeBaseCorePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ToggleBorderPayload.TYPE, ToggleBorderPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LecternAutoFillPayload.TYPE, LecternAutoFillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LecternCraftPayload.TYPE, LecternCraftPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(UpgradeBaseCorePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleUpgradeRequest(context.player(), payload);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(ToggleBorderPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleToggleBorderRequest(context.player(), payload);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(LecternAutoFillPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> BaseCoreServerLogic.handleLecternAutoFill(context.player(), payload));
		});

		ServerPlayNetworking.registerGlobalReceiver(LecternCraftPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> BaseCoreServerLogic.handleLecternCraft(context.player(), payload));
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			String serverJson = BaseCoreServerConfig.getServerConfigString();
			ServerPlayNetworking.send(player, new ConfigSyncPayload(serverJson));
		});
	}
}