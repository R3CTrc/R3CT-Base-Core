package com.r3ct.base_core.item;

import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.logic.BaseCoreClientLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

public class MailboxBlockItem extends BlockItem {

    public MailboxBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player != null) {
            if (level.isClientSide()) {
                if (BaseCoreClientLogic.clientHasMailbox) {
                    return InteractionResult.FAIL;
                }
            }
            else if (player instanceof ServerPlayer serverPlayer) {
                ModState state = ModState.get(level.getServer());
                PlayerData data = ModState.getPlayerData(level.getServer(), serverPlayer.getUUID());

                if (data.hasPlacedMailbox) {
                    ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(data.mailboxDimension));
                    ServerLevel targetLevel = level.getServer().getLevel(dimKey);
                    BlockPos targetPos = new BlockPos(data.mailboxX, data.mailboxY, data.mailboxZ);
                    boolean mailboxExists = false;

                    if (targetLevel != null) {
                        if (targetLevel.isLoaded(targetPos)) {
                            BlockEntity targetBE = targetLevel.getBlockEntity(targetPos);
                            if (targetBE instanceof com.r3ct.base_core.block.MailboxBlockEntity mailboxBE && serverPlayer.getUUID().toString().equals(mailboxBE.getOwnerUUID())) {
                                mailboxExists = true;
                            }
                        } else {
                            mailboxExists = true;
                        }
                    }

                    if (mailboxExists) {
                        serverPlayer.sendSystemMessage(Component.literal("Posiadasz już skrzynkę pocztową! Koordynaty: " + data.mailboxX + ", " + data.mailboxY + ", " + data.mailboxZ).withStyle(ChatFormatting.RED));
                        if (targetLevel != null && !targetLevel.isLoaded(targetPos)) {
                            serverPlayer.sendSystemMessage(Component.literal("Skrzynka znajduje się w niezaładowanym chunku.").withStyle(ChatFormatting.GRAY));
                        }
                        return InteractionResult.FAIL;
                    } else {
                        data.hasPlacedMailbox = false;
                        state.setDirty();

                        serverPlayer.connection.send(new ClientboundCustomPayloadPacket(
                                new com.r3ct.base_core.network.SyncMailboxStatePayload(false, BlockPos.ZERO, "")
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
        builder.accept(Component.literal("Limit: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("1 na Gracza").withStyle(ChatFormatting.AQUA)));
    }
}