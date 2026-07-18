package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlock;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BaseCoreServerLogic {

    private static class UpgradeCost {
        final int xpRequired;
        final int itemAmount;
        final Item itemRequired;

        UpgradeCost(int xpRequired, int itemAmount, Item itemRequired) {
            this.xpRequired = xpRequired;
            this.itemAmount = itemAmount;
            this.itemRequired = itemRequired;
        }
    }

    private static UpgradeCost getUpgradeCost(int targetTier) {
        return switch (targetTier) {
            case 1 -> new UpgradeCost(100, 10, Items.IRON_BLOCK);
            case 2 -> new UpgradeCost(250, 15, Items.GOLD_BLOCK);
            case 3 -> new UpgradeCost(500, 5, Items.DIAMOND);
            case 4 -> new UpgradeCost(1000, 1, Items.NETHERITE_INGOT);
            case 5 -> new UpgradeCost(2000, 1, Items.NETHER_STAR);
            default -> new UpgradeCost(targetTier * 1000, targetTier, Items.EMERALD_BLOCK);
        };
    }

    public static void handleUpgradeRequest(ServerPlayer player, UpgradeBaseCorePayload payload) {
        Level level = player.level();
        PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());

        if (data.baseCoreTier >= 10) {
            player.sendSystemMessage(Component.literal("Osiągnięto limit architektury.").withStyle(ChatFormatting.RED), true);
            return;
        }

        int nextTier = data.baseCoreTier + 1;
        UpgradeCost cost = getUpgradeCost(nextTier);

        if (getTotalExperience(player) < cost.xpRequired) {
            player.sendSystemMessage(Component.literal("Brak wystarczającej ilości XP do ulepszenia!").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!consumeItems(player.getInventory(), cost.itemRequired, cost.itemAmount)) {
            player.sendSystemMessage(Component.literal("Brak wymaganych przedmiotów do ulepszenia!").withStyle(ChatFormatting.RED), true);
            return;
        }

        removeExperience(player, cost.xpRequired);

        data.baseCoreTier = nextTier;

        BlockState currentState = level.getBlockState(payload.pos());
        if (currentState.hasProperty(BaseCoreBlock.TIER)) {
            level.setBlock(payload.pos(), currentState.setValue(BaseCoreBlock.TIER, nextTier), 3);
        }

        level.playSound(null, payload.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("Serce Bazy ulepszone do Poziomu " + nextTier + "!").withStyle(ChatFormatting.GREEN), true);

        ModState.get(level.getServer()).setDirty();

        // TODO: Wysłanie pakietu OpenBaseCoreGuiPayload, aby ekran się przeładował
    }

    public static void handleUnlockRequest(ServerPlayer player, UnlockEffectPayload payload) {
        Level level = player.level();
        PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());

        if (payload.slotIndex() == -1) {
            if (data.activeEffects.contains(payload.effectId())) {
                player.sendSystemMessage(Component.literal("Ten protokół został już odblokowany!").withStyle(ChatFormatting.RED), true);
                return;
            }

            // TODO: Podobnie jak przy ulepszeniach bloku, cennik poszczególnych efektów

            if (getTotalExperience(player) >= 150) {
                removeExperience(player, 150);
            } else {
                player.sendSystemMessage(Component.literal("Brak wystarczającej ilości XP do odblokowania efektu.").withStyle(ChatFormatting.RED), true);
                return;
            }

            data.activeEffects.add(payload.effectId());
            ModState.get(level.getServer()).setDirty();

            level.playSound(null, payload.pos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.literal("Odblokowano protokół: " + payload.effectId()).withStyle(ChatFormatting.GOLD), true);

        } else {
            player.sendSystemMessage(Component.literal("Przypisano efekt do slotu " + payload.slotIndex()).withStyle(ChatFormatting.AQUA), true);
        }
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