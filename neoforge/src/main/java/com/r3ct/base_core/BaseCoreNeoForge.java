package com.r3ct.base_core;

import com.r3ct.base_core.block.ModBlocks;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ConfigSyncPayload;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.ToggleBorderPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import com.r3ct.base_core.item.BlueprintItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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

    public BaseCoreNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        Constants.LOG.info("Starting Base Core system on NeoForge!");
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

        registrar.playToServer(UnlockEffectPayload.TYPE, UnlockEffectPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    BaseCoreServerLogic.handleUnlockRequest(player, payload);
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

        registrar.playToClient(OpenBaseCoreGuiPayload.TYPE, OpenBaseCoreGuiPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> BaseCoreNeoForgeClient.ClientPayloadHandlers.handleOpenGui(payload));
        });
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> helper.register(ModBlocks.BASE_CORE_KEY, ModBlocks.BASE_CORE));
        event.register(Registries.ITEM, helper -> {
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":base_core")), BASE_CORE_ITEM);
            helper.register(ResourceKey.create(Registries.ITEM, Identifier.parse(Constants.MOD_ID + ":blueprint")), BLUEPRINT_ITEM);
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> helper.register(ModBlocks.BASE_CORE_BE_KEY, ModBlocks.BASE_CORE_BE_TYPE));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(Identifier.parse(Constants.MOD_ID + ":main_tab"),
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.r3ct_base_core.main_tab"))
                            .icon(() -> new ItemStack(BASE_CORE_ITEM))
                            .displayItems((context, output) -> {
                                output.accept(BASE_CORE_ITEM);
                                output.accept(BLUEPRINT_ITEM);
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
        }
    }
}