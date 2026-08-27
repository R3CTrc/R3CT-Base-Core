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
import net.minecraft.core.BlockPos;
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

	public static final Item BASE_CORE_ITEM = new com.r3ct.base_core.item.BaseCoreBlockItem(ModBlocks.BASE_CORE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"))));
	public static final Item BLUEPRINT_ITEM = new BlueprintItem(new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint")))
			.stacksTo(16)
	);

	public static final Item ARCANE_LECTERN_ITEM = new BlockItem(ModBlocks.ARCANE_LECTERN, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":arcane_lectern")))) {
		@Override
		public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> builder, net.minecraft.world.item.TooltipFlag tooltipFlag) {
			super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
			builder.accept(Component.translatable("item.r3ct_base_core.arcane_lectern.desc.1").withStyle(net.minecraft.ChatFormatting.GRAY));
			builder.accept(Component.translatable("item.r3ct_base_core.arcane_lectern.desc.2").withStyle(net.minecraft.ChatFormatting.GRAY));
		}
	};

	public static final Item MAGIC_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":magic_tome"))));
	public static final Item DARK_MAGIC_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":dark_magic_tome"))));
	public static final Item ALCHEMY_TOME = new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":alchemy_tome"))));
	public static final Item EMPOWERED_TOME = new EmpoweredTomeItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":empowered_tome"))));

	@Override
	public void onInitialize() {
		Constants.LOG.info("Starting Base Core system on Fabric!");

		ModDataComponents.init();
		BaseCoreServerConfig.load();

		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":pvp_protection"), com.r3ct.base_core.registry.ModEffects.PVP_PROTECTION);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":fall_resistance"), com.r3ct.base_core.registry.ModEffects.FALL_RESISTANCE);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":satiation"), com.r3ct.base_core.registry.ModEffects.SATIATION);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":fire_immunity"), com.r3ct.base_core.registry.ModEffects.FIRE_IMMUNITY);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":night_vision"), com.r3ct.base_core.registry.ModEffects.NIGHT_VISION);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":extended_reach"), com.r3ct.base_core.registry.ModEffects.EXTENDED_REACH);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":mending_pulse"), com.r3ct.base_core.registry.ModEffects.MENDING_PULSE);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":pet_protection"), com.r3ct.base_core.registry.ModEffects.PET_PROTECTION);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":hostile_slowness"), com.r3ct.base_core.registry.ModEffects.HOSTILE_SLOWNESS);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":livestock_boost"), com.r3ct.base_core.registry.ModEffects.LIVESTOCK_BOOST);
		Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(Constants.MOD_ID + ":twin_breeding"), com.r3ct.base_core.registry.ModEffects.TWIN_BREEDING);

		Registry.register(BuiltInRegistries.BLOCK, ModBlocks.BASE_CORE_KEY, ModBlocks.BASE_CORE);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core"), BASE_CORE_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.BASE_CORE_BE_KEY, ModBlocks.BASE_CORE_BE_TYPE);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint"), BLUEPRINT_ITEM);

		Registry.register(BuiltInRegistries.MENU, Identifier.parse(Constants.MOD_ID + ":base_core_menu"), ModMenuTypes.BASE_CORE_MENU);
		Registry.register(BuiltInRegistries.MENU, Identifier.parse(Constants.MOD_ID + ":base_core_visitor_menu"), ModMenuTypes.BASE_CORE_VISITOR_MENU);
		Registry.register(BuiltInRegistries.MENU, Identifier.parse(Constants.MOD_ID + ":arcane_lectern_menu"), ModMenuTypes.ARCANE_LECTERN_MENU);

		Registry.register(BuiltInRegistries.BLOCK, ModBlocks.ARCANE_LECTERN_KEY, ModBlocks.ARCANE_LECTERN);
		Registry.register(BuiltInRegistries.ITEM, Identifier.parse(Constants.MOD_ID + ":arcane_lectern"), ARCANE_LECTERN_ITEM);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.ARCANE_LECTERN_BE_KEY, ModBlocks.ARCANE_LECTERN_BE_TYPE);

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

					for (com.r3ct.base_core.config.LecternRecipeDef recipe : com.r3ct.base_core.logic.LecternRecipes.getRecipes()) {
						if (recipe.effectId() != null && !recipe.effectId().isEmpty()) {
							output.accept(com.r3ct.base_core.item.EmpoweredTomeItem.createFromRecipe(EMPOWERED_TOME, recipe));
						}
					}
				})
				.build()
		);

		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncCoreStatePayload.TYPE, SyncCoreStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncAllCoresPayload.TYPE, SyncAllCoresPayload.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(UpgradeBaseCorePayload.TYPE, UpgradeBaseCorePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ToggleBorderPayload.TYPE, ToggleBorderPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LecternAutoFillPayload.TYPE, LecternAutoFillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ApplyEffectsPayload.TYPE, ApplyEffectsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RemoveEffectPayload.TYPE, RemoveEffectPayload.CODEC);

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

		ServerPlayNetworking.registerGlobalReceiver(ApplyEffectsPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleApplyEffectsRequest(context.player(), payload);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(RemoveEffectPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				BaseCoreServerLogic.handleRemoveEffectRequest(context.player(), payload);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(LecternAutoFillPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> BaseCoreServerLogic.handleLecternAutoFill(context.player(), payload));
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			String serverJson = BaseCoreServerConfig.getServerConfigString();
			ServerPlayNetworking.send(player, new ConfigSyncPayload(serverJson));
			com.r3ct.base_core.data.PlayerData data = com.r3ct.base_core.data.ModState.getPlayerData(server, player.getUUID());

			BlockPos pos = data.hasPlacedCore ? new BlockPos(data.coreX, data.coreY, data.coreZ) : BlockPos.ZERO;
			String dim = data.hasPlacedCore ? data.coreDimension : "";
			ServerPlayNetworking.send(player, new SyncCoreStatePayload(data.hasPlacedCore, pos, dim));
			ServerPlayNetworking.send(player, BaseCoreServerLogic.getAllCoresPayload(server));
		});
	}
}