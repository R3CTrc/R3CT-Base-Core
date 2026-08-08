package com.r3ct.base_core.item;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.logic.BaseCoreClientLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

public class BaseCoreBlockItem extends BlockItem {

    public BaseCoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player != null) {
            if (level.isClientSide()) {
                if (BaseCoreClientLogic.clientHasCore) {
                    return InteractionResult.FAIL;
                }
            }
            else if (player instanceof ServerPlayer serverPlayer) {
                PlayerData data = ModState.getPlayerData(level.getServer(), serverPlayer.getUUID());

                if (data.hasPlacedCore) {
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(data.coreDimension));
                    ServerLevel targetLevel = level.getServer().getLevel(dimKey);
                    BlockPos targetPos = new BlockPos(data.coreX, data.coreY, data.coreZ);
                    boolean coreExists = false;

                    if (targetLevel != null) {
                        if (targetLevel.isLoaded(targetPos)) {
                            BlockEntity targetBE = targetLevel.getBlockEntity(targetPos);
                            if (targetBE instanceof com.r3ct.base_core.block.BaseCoreBlockEntity coreBE && serverPlayer.getUUID().toString().equals(coreBE.getOwnerUUID())) {
                                coreExists = true;
                            }
                        } else {
                            coreExists = true;
                        }
                    }

                    if (coreExists) {
                        serverPlayer.sendSystemMessage(Component.translatable("r3ct_base_core.message.core_exists", data.coreX, data.coreY, data.coreZ, data.coreDimension).withStyle(ChatFormatting.RED));
                        if (targetLevel != null && !targetLevel.isLoaded(targetPos)) {
                            serverPlayer.sendSystemMessage(Component.translatable("r3ct_base_core.message.core_exists_hint").withStyle(ChatFormatting.GRAY));
                        }
                        return InteractionResult.FAIL;
                    } else {
                        data.hasPlacedCore = false;
                        ModState.get(level.getServer()).setDirty();

                        serverPlayer.connection.send(new ClientboundCustomPayloadPacket(
                                new com.r3ct.base_core.network.SyncCoreStatePayload(false, BlockPos.ZERO, "")
                        ));
                    }
                }
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        int tier = 0;

        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            tier = tag.getInt("baseCoreTier").orElse(0);
        }

        int range = BaseCoreServerConfig.calculateRangeUpToTier(tier);
        int slots = BaseCoreServerConfig.calculateTotalSlots(tier);

        String diameterStr = range == 0 ? "0" : String.valueOf(range * 2 + 1);
        String displayTier = tier == 0 ? "0" : String.valueOf(tier);

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.tier")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(displayTier).withStyle(ChatFormatting.AQUA)));

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.area")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(range)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (" + diameterStr + ")").withStyle(ChatFormatting.RED)));

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.slots")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(slots)).withStyle(ChatFormatting.GREEN)));
    }
}