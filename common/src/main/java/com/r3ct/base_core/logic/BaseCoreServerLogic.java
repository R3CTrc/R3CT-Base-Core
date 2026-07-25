package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlock;
import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.client.screen.ArcaneLecternMenu;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.network.*;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
    }

    public static void handleToggleBorderRequest(ServerPlayer player, ToggleBorderPayload payload) {
        Level level = player.level();

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;

        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        coreBE.toggleShowBorder();
    }

    public static void handleLecternAutoFill(ServerPlayer player, LecternAutoFillPayload payload) {
        if (!(player.containerMenu instanceof ArcaneLecternMenu menu)) return;

        LecternRecipes.getRecipeById(payload.effectId()).ifPresent(recipe -> {
            Item requiredTome = recipe.getTomeItem();
            Item requiredIngredient = recipe.getCostItem();
            int requiredAmount = recipe.itemAmount();

            if (player.getInventory().countItem(requiredTome) < 1) return;
            if (player.getInventory().countItem(requiredIngredient) < requiredAmount) return;

            Slot tomeSlot = menu.getSlot(0);
            Slot ingredientSlot = menu.getSlot(1);

            if (tomeSlot.hasItem()) {
                player.getInventory().placeItemBackInInventory(tomeSlot.getItem());
                tomeSlot.set(ItemStack.EMPTY);
            }
            if (ingredientSlot.hasItem()) {
                player.getInventory().placeItemBackInInventory(ingredientSlot.getItem());
                ingredientSlot.set(ItemStack.EMPTY);
            }

            if (consumeItems(player.getInventory(), requiredTome, 1)) {
                tomeSlot.set(new ItemStack(requiredTome, 1));
            }
            if (consumeItems(player.getInventory(), requiredIngredient, requiredAmount)) {
                ingredientSlot.set(new ItemStack(requiredIngredient, requiredAmount));
            }
        });
    }

    public static void handleLecternCraft(ServerPlayer player, LecternCraftPayload payload) {
        if (!(player.containerMenu instanceof ArcaneLecternMenu menu)) return;

        LecternRecipes.getRecipeById(payload.effectId()).ifPresent(recipe -> {
            Slot tomeSlot = menu.getSlot(0);
            Slot ingredientSlot = menu.getSlot(1);
            Slot outputSlot = menu.getSlot(2);

            if (outputSlot.hasItem()) return;

            if (!tomeSlot.getItem().is(recipe.getTomeItem())) return;
            if (!ingredientSlot.getItem().is(recipe.getCostItem()) || ingredientSlot.getItem().getCount() < recipe.itemAmount()) return;

            if (getTotalExperience(player) < recipe.xpCost()) return;

            removeExperience(player, recipe.xpCost());

            tomeSlot.remove(1);
            ingredientSlot.remove(recipe.itemAmount());

            Item empoweredTomeItem = BuiltInRegistries.ITEM.get(Identifier.parse("r3ct_base_core:empowered_tome")).map(net.minecraft.core.Holder::value).orElse(net.minecraft.world.item.Items.BOOK);

            ItemStack resultTome = new ItemStack(empoweredTomeItem);

            resultTome.set(ModDataComponents.EFFECT_ID, recipe.id());

            outputSlot.set(resultTome);

            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        });
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