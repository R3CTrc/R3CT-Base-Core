package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlock;
import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.ToggleBorderPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseCoreServerLogic {

    public static void handleUpgradeRequest(ServerPlayer player, UpgradeBaseCorePayload payload) {
        Level level = player.level();

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;

        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        if (coreBE.getTier() >= 11) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.max_tier").withStyle(ChatFormatting.RED), true);
            return;
        }

        int nextTier = coreBE.getTier() + 1;

        BaseCoreServerConfig.TierUpgrade tierConfig = BaseCoreServerConfig.getTier(nextTier);
        if (tierConfig == null) return;

        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

        if (!consumeItems(player.getInventory(), mainItem, tierConfig.mainAmount)) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.missing_main").withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!consumeItems(player.getInventory(), bulkItem, tierConfig.bulkAmount)) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.missing_bulk").withStyle(ChatFormatting.RED), true);
            return;
        }

        coreBE.setTier(nextTier);

        BlockState currentState = level.getBlockState(payload.pos());
        if (currentState.hasProperty(BaseCoreBlock.TIER)) {
            level.setBlock(payload.pos(), currentState.setValue(BaseCoreBlock.TIER, nextTier), 3);
        }

        try {
            UUID ownerId = UUID.fromString(coreBE.getOwnerUUID());
            PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);
            data.coreTier = nextTier;
            ModState.get(level.getServer()).setDirty();
        } catch (IllegalArgumentException ignored) {}

        level.playSound(null, payload.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.success", Component.translatable(tierConfig.title)).withStyle(ChatFormatting.GREEN), true);

        if (nextTier == 1) {
            grantAdvancement(player, "first_upgrade");
        } else if (nextTier == 5) {
            grantAdvancement(player, "tier_5");
        } else if (nextTier == 11) {
            grantAdvancement(player, "max_tier");
        }

        refreshGuiForPlayer(player, payload.pos(), coreBE);
    }

    public static void handleUnlockRequest(ServerPlayer player, UnlockEffectPayload payload) {
        Level level = player.level();

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;

        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        List<String> activeEffects = coreBE.getActiveEffects();
        List<String> activeSlots = coreBE.getActiveSlots();

        if (payload.slotIndex() == -1) {
            BaseCoreServerConfig.EffectConfig effectConfig = BaseCoreServerConfig.getEffect(payload.effectId());
            if (effectConfig == null) return;

            int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(coreBE.getTier());
            if (effectConfig.pool > maxPool) {
                player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effect.pool_locked").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            if (activeEffects.contains(payload.effectId())) {
                player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effect.already_unlocked").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effectConfig.itemCost)).map(Holder::value).orElse(Items.AIR);

            if (getTotalExperience(player) < effectConfig.xpCost) {
                player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effect.missing_xp").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            if (!consumeItems(player.getInventory(), costItem, effectConfig.itemAmount)) {
                player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effect.missing_items").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            removeExperience(player, effectConfig.xpCost);

            activeEffects.add(payload.effectId());
            coreBE.forceSync();

            level.playSound(null, payload.pos(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effect.unlock_success", Component.translatable(effectConfig.name)).withStyle(net.minecraft.ChatFormatting.GOLD), true);

            if (activeEffects.size() == 1) {
                grantAdvancement(player, "first_effect");
            }
            if (activeEffects.size() >= BaseCoreServerConfig.getInstance().effects.size()) {
                grantAdvancement(player, "all_effects");
            }

            refreshGuiForPlayer(player, payload.pos(), coreBE);
            return;
        }

        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(coreBE.getTier());
        if (payload.slotIndex() >= maxSlots || payload.slotIndex() < 0) return;

        if (payload.effectId().equals("empty")) {
            if (activeSlots.get(payload.slotIndex()).equals("empty")) {
                return;
            }
            activeSlots.set(payload.slotIndex(), "empty");
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.slot.cleared", payload.slotIndex() + 1).withStyle(net.minecraft.ChatFormatting.YELLOW), true);
        }
        else {
            BaseCoreServerConfig.EffectConfig effectConfig = BaseCoreServerConfig.getEffect(payload.effectId());
            if (effectConfig == null) return;

            if (!activeEffects.contains(payload.effectId())) {
                player.sendSystemMessage(Component.translatable("r3ct_base_core.message.slot.not_unlocked").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            if (activeSlots.get(payload.slotIndex()).equals(payload.effectId())) {
                return;
            }

            for (int i = 0; i < activeSlots.size(); i++) {
                if (i != payload.slotIndex() && activeSlots.get(i).equals(payload.effectId())) {
                    player.sendSystemMessage(Component.translatable("r3ct_base_core.message.slot.already_in_use").withStyle(net.minecraft.ChatFormatting.RED), true);
                    return;
                }
            }

            activeSlots.set(payload.slotIndex(), payload.effectId());
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.slot.assigned", payload.slotIndex() + 1).withStyle(net.minecraft.ChatFormatting.AQUA), true);
        }

        coreBE.forceSync();

        try {
            UUID ownerId = UUID.fromString(coreBE.getOwnerUUID());
            PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);
            data.activeSlots = new ArrayList<>(coreBE.getActiveSlots());
            ModState.get(level.getServer()).setDirty();
        } catch (IllegalArgumentException ignored) {}

        boolean allFull = true;
        for (int i = 0; i < 4; i++) {
            if (activeSlots.get(i).equals("empty")) {
                allFull = false;
                break;
            }
        }
        if (allFull) grantAdvancement(player, "all_slots");

        if (activeSlots.contains("anti_spawn") && activeSlots.contains("anti_explosion")) {
            grantAdvancement(player, "safe_zone");
        }

        if (activeSlots.contains("crop_growth") && activeSlots.contains("anti_trample")) {
            grantAdvancement(player, "farming_combo");
        }

        level.playSound(null, payload.pos(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);

        refreshGuiForPlayer(player, payload.pos(), coreBE);
    }

    public static void handleToggleBorderRequest(ServerPlayer player, ToggleBorderPayload payload) {
        Level level = player.level();

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;

        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        coreBE.toggleShowBorder();
    }

    private static void refreshGuiForPlayer(ServerPlayer player, BlockPos pos, BaseCoreBlockEntity coreBE) {
        OpenBaseCoreGuiPayload refreshPayload = new OpenBaseCoreGuiPayload(
                pos,
                coreBE.getTier(),
                coreBE.getActiveEffects(),
                coreBE.getActiveSlots()
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

    public static void grantAdvancement(ServerPlayer player, String advancementName) {
        Identifier id = Identifier.parse("r3ct_base_core:" + advancementName);
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }
}