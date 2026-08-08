package com.r3ct.base_core;

import com.r3ct.base_core.block.ModBlocks;
import com.r3ct.base_core.client.screen.ModMenuTypes;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.item.EmpoweredTomeItem;
import com.r3ct.base_core.network.*;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import com.r3ct.base_core.item.BlueprintItem;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@Mod(Constants.MOD_ID)
public class BaseCoreNeoForge {

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

    public BaseCoreNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        Constants.LOG.info("Starting Base Core system on NeoForge!");

        ModDataComponents.init();
        BaseCoreServerConfig.load();

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onRegister);
        NeoForge.EVENT_BUS.register(this);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            BaseCoreNeoForgeClient.init(modContainer);
        }
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID);

        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> BaseCoreNeoForgeClient.ClientPayloadHandlers.handleConfigSync(payload));
        });

        registrar.playToServer(UpgradeBaseCorePayload.TYPE, UpgradeBaseCorePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleUpgradeRequest(player, payload);
                }
            });
        });

        registrar.playToServer(ToggleBorderPayload.TYPE, ToggleBorderPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleToggleBorderRequest(player, payload);
                }
            });
        });

        registrar.playToServer(ApplyEffectsPayload.TYPE, ApplyEffectsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleApplyEffectsRequest(player, payload);
                }
            });
        });

        registrar.playToServer(RemoveEffectPayload.TYPE, RemoveEffectPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleRemoveEffectRequest(player, payload);
                }
            });
        });

        registrar.playToServer(LecternAutoFillPayload.TYPE, LecternAutoFillPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleLecternAutoFill(player, payload);
                }
            });
        });

        registrar.playToClient(SyncCoreStatePayload.TYPE, SyncCoreStatePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> BaseCoreNeoForgeClient.ClientPayloadHandlers.handleCoreStateSync(payload));
        });
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.MOB_EFFECT, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":pvp_protection"), com.r3ct.base_core.registry.ModEffects.PVP_PROTECTION);
            helper.register(Identifier.parse(Constants.MOD_ID + ":fall_resistance"), com.r3ct.base_core.registry.ModEffects.FALL_RESISTANCE);
            helper.register(Identifier.parse(Constants.MOD_ID + ":satiation"), com.r3ct.base_core.registry.ModEffects.SATIATION);
            helper.register(Identifier.parse(Constants.MOD_ID + ":fire_immunity"), com.r3ct.base_core.registry.ModEffects.FIRE_IMMUNITY);
            helper.register(Identifier.parse(Constants.MOD_ID + ":night_vision"), com.r3ct.base_core.registry.ModEffects.NIGHT_VISION);
            helper.register(Identifier.parse(Constants.MOD_ID + ":extended_reach"), com.r3ct.base_core.registry.ModEffects.EXTENDED_REACH);
            helper.register(Identifier.parse(Constants.MOD_ID + ":mending_pulse"), com.r3ct.base_core.registry.ModEffects.MENDING_PULSE);
            helper.register(Identifier.parse(Constants.MOD_ID + ":pet_protection"), com.r3ct.base_core.registry.ModEffects.PET_PROTECTION);
            helper.register(Identifier.parse(Constants.MOD_ID + ":hostile_slowness"), com.r3ct.base_core.registry.ModEffects.HOSTILE_SLOWNESS);
            helper.register(Identifier.parse(Constants.MOD_ID + ":livestock_boost"), com.r3ct.base_core.registry.ModEffects.LIVESTOCK_BOOST);
            helper.register(Identifier.parse(Constants.MOD_ID + ":twin_breeding"), com.r3ct.base_core.registry.ModEffects.TWIN_BREEDING);
        });

        event.register(Registries.BLOCK, helper -> {
            helper.register(ModBlocks.BASE_CORE_KEY, ModBlocks.BASE_CORE);
            helper.register(ModBlocks.ARCANE_LECTERN_KEY, ModBlocks.ARCANE_LECTERN);
        });
        event.register(Registries.ITEM, helper -> {
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core")), BASE_CORE_ITEM);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint")), BLUEPRINT_ITEM);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":arcane_lectern")), ARCANE_LECTERN_ITEM);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":magic_tome")), MAGIC_TOME);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":dark_magic_tome")), DARK_MAGIC_TOME);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":alchemy_tome")), ALCHEMY_TOME);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":empowered_tome")), EMPOWERED_TOME);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            helper.register(ModBlocks.BASE_CORE_BE_KEY, ModBlocks.BASE_CORE_BE_TYPE);
            helper.register(ModBlocks.ARCANE_LECTERN_BE_KEY, ModBlocks.ARCANE_LECTERN_BE_TYPE);
        });
        event.register(Registries.MENU, helper -> {
            helper.register(ResourceKey.create(Registries.MENU, Identifier.parse(Constants.MOD_ID + ":base_core_menu")), ModMenuTypes.BASE_CORE_MENU);
            helper.register(ResourceKey.create(Registries.MENU, Identifier.parse(Constants.MOD_ID + ":arcane_lectern_menu")), ModMenuTypes.ARCANE_LECTERN_MENU);
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    CreativeModeTab.builder()
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
        });
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String serverJson = BaseCoreServerConfig.getServerConfigString();
            PacketDistributor.sendToPlayer(player, new ConfigSyncPayload(serverJson));
            com.r3ct.base_core.data.PlayerData data = com.r3ct.base_core.data.ModState.getPlayerData(player.level().getServer(), player.getUUID());
            BlockPos pos = data.hasPlacedCore ? new BlockPos(data.coreX, data.coreY, data.coreZ) : BlockPos.ZERO;
            String dim = data.hasPlacedCore ? data.coreDimension : "";
            PacketDistributor.sendToPlayer(player, new SyncCoreStatePayload(data.hasPlacedCore, pos, dim));
        }
    }
}