package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlock;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BaseCoreServerLogic {

    public static void handleUpgradeRequest(ServerPlayer player, UpgradeBaseCorePayload payload) {
        Level level = player.level();
        PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());

        if (data.baseCoreTier >= 10) {
            player.sendSystemMessage(Component.literal("Osiągnięto limit architektury.").withStyle(ChatFormatting.RED), true);
            return;
        }

        int nextTier = data.baseCoreTier + 1;

        BaseCoreServerConfig.TierUpgrade tierConfig = BaseCoreServerConfig.getTier(nextTier);
        if (tierConfig == null) return;

        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

        if (!consumeItems(player.getInventory(), mainItem, tierConfig.mainAmount)) {
            player.sendSystemMessage(Component.literal("Brak wymaganych zasobów głównych!").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!consumeItems(player.getInventory(), bulkItem, tierConfig.bulkAmount)) {
            player.sendSystemMessage(Component.literal("Brak wymaganych zasobów pospolitych!").withStyle(ChatFormatting.RED), true);
            return;
        }

        data.baseCoreTier = nextTier;

        BlockState currentState = level.getBlockState(payload.pos());
        if (currentState.hasProperty(BaseCoreBlock.TIER)) {
            level.setBlock(payload.pos(), currentState.setValue(BaseCoreBlock.TIER, nextTier), 3);
        }

        level.playSound(null, payload.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("Serce Bazy ulepszone do etapu: " + tierConfig.title).withStyle(ChatFormatting.GREEN), true);

        ModState.get(level.getServer()).setDirty();
        refreshGuiForPlayer(player, payload.pos(), data);
    }

    public static void handleUnlockRequest(ServerPlayer player, UnlockEffectPayload payload) {
        Level level = player.level();
        PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());

        BaseCoreServerConfig.EffectConfig effectConfig = BaseCoreServerConfig.getEffect(payload.effectId());
        if (effectConfig == null) return;

        if (payload.slotIndex() == -1) {
            if (data.activeEffects.contains(payload.effectId())) {
                player.sendSystemMessage(Component.literal("Ten protokół został już odblokowany!").withStyle(ChatFormatting.RED), true);
                return;
            }

            Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effectConfig.itemCost)).map(Holder::value).orElse(Items.AIR);

            if (getTotalExperience(player) < effectConfig.xpCost) {
                player.sendSystemMessage(Component.literal("Brak wystarczającej ilości XP do odblokowania efektu.").withStyle(ChatFormatting.RED), true);
                return;
            }

            if (!consumeItems(player.getInventory(), costItem, effectConfig.itemAmount)) {
                player.sendSystemMessage(Component.literal("Brak wymaganych przedmiotów do odblokowania!").withStyle(ChatFormatting.RED), true);
                return;
            }

            removeExperience(player, effectConfig.xpCost);

            data.activeEffects.add(payload.effectId());
            ModState.get(level.getServer()).setDirty();

            level.playSound(null, payload.pos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("Odblokowano protokół: " + effectConfig.name).withStyle(ChatFormatting.GOLD), true);

            refreshGuiForPlayer(player, payload.pos(), data);

        } else {
            int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.baseCoreTier);

            if (payload.slotIndex() >= maxSlots || payload.slotIndex() < 0) {
                player.sendSystemMessage(Component.literal("Ten slot jest jeszcze zablokowany!").withStyle(ChatFormatting.RED), true);
                return;
            }

            if (data.activeSlots.contains(payload.effectId())) {
                player.sendSystemMessage(Component.literal("Ten efekt jest już aktywny!").withStyle(ChatFormatting.RED), true);
                return;
            }

            data.activeSlots.set(payload.slotIndex(), payload.effectId());
            ModState.get(level.getServer()).setDirty();

            level.playSound(null, payload.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("Przypisano efekt " + effectConfig.name + " do slotu " + (payload.slotIndex() + 1)).withStyle(ChatFormatting.AQUA), true);

            refreshGuiForPlayer(player, payload.pos(), data);
        }
    }

    private static void refreshGuiForPlayer(ServerPlayer player, BlockPos pos, PlayerData data) {
        OpenBaseCoreGuiPayload refreshPayload = new OpenBaseCoreGuiPayload(
                pos,
                data.baseCoreTier,
                data.activeEffects,
                data.activeSlots
        );

        Services.PLATFORM.sendToPlayer(player, refreshPayload);
    }

    private static int getTotalExperience(ServerPlayer player) {
        int level = player.experienceLevel;
        int totalExp = 0;

        if (level >= 0 && level <= 15) {
            totalExp = level * level + 6 * level;
        } else if (level > 15 && level <= 30) {
            totalExp = (int) (2.5 * level * level - 40.5 * level + 360.0);
        } else if (level > 30) {
            totalExp = (int) (4.5 * level * level - 162.5 * level + 2220.0);
        }

        return totalExp + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private static void removeExperience(ServerPlayer player, int amount) {
        int newTotalExp = Math.max(0, getTotalExperience(player) - amount);
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.giveExperiencePoints(newTotalExp);
    }

    private static boolean consumeItems(Inventory inventory, Item itemToConsume, int amountNeeded) {
        if (amountNeeded <= 0 || itemToConsume == Items.AIR) return true;

        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == itemToConsume) {
                count += stack.getCount();
            }
        }

        if (count < amountNeeded) {
            return false;
        }

        int amountLeftToRemove = amountNeeded;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (amountLeftToRemove <= 0) break;

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == itemToConsume) {
                if (stack.getCount() <= amountLeftToRemove) {
                    amountLeftToRemove -= stack.getCount();
                    inventory.setItem(i, ItemStack.EMPTY);
                } else {
                    stack.shrink(amountLeftToRemove);
                    amountLeftToRemove = 0;
                }
            }
        }

        return true;
    }
}