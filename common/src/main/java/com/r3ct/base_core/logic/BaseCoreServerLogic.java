package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlock;
import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.block.MailboxBlockEntity;
import com.r3ct.base_core.client.screen.ArcaneLecternMenu;
import com.r3ct.base_core.client.screen.BaseCoreMenu;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.MailMessage;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

public class BaseCoreServerLogic {

    public static void handleUpgradeRequest(ServerPlayer player, UpgradeBaseCorePayload payload) {
        Level level = player.level();

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;

        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        if (!(player.containerMenu instanceof BaseCoreMenu menu)) return;

        if (coreBE.getTier() >= 11) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.max_tier").withStyle(ChatFormatting.RED), true);
            return;
        }

        int nextTier = coreBE.getTier() + 1;

        BaseCoreServerConfig.TierUpgrade tierConfig = BaseCoreServerConfig.getTier(nextTier);
        if (tierConfig == null) return;

        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

        int stagedMain = 0;
        int stagedBulk = 0;

        for (int i = 4; i <= 7; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.is(mainItem)) stagedMain += stack.getCount();
        }
        for (int i = 8; i <= 11; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.is(bulkItem)) stagedBulk += stack.getCount();
        }

        if (stagedMain < tierConfig.mainAmount) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.missing_main").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (stagedBulk < tierConfig.bulkAmount) {
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.upgrade.missing_bulk").withStyle(ChatFormatting.RED), true);
            return;
        }

        consumeFromStaging(menu, mainItem, tierConfig.mainAmount, 4, 7);
        consumeFromStaging(menu, bulkItem, tierConfig.bulkAmount, 8, 11);

        for (int i = 4; i <= 11; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.hasItem()) {
                ItemStack leftover = slot.getItem().copy();
                slot.set(ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(leftover);
            }
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

    public static void handleApplyEffectsRequest(ServerPlayer player, ApplyEffectsPayload payload) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;
        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        if (!(player.containerMenu instanceof BaseCoreMenu menu)) return;

        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(coreBE.getTier());
        int requiredXp = 0;

        for (int i = 0; i < maxSlots; i++) {
            ItemStack stagedStack = menu.getSlot(i).getItem();
            if (!stagedStack.isEmpty() && stagedStack.has(ModDataComponents.EFFECT_ID)) {
                String effectId = stagedStack.get(ModDataComponents.EFFECT_ID);
                var recipe = com.r3ct.base_core.logic.LecternRecipes.getRecipeById(effectId).orElse(null);
                if (recipe != null) requiredXp += BaseCoreServerConfig.calculateActivationCost(recipe);
            }
        }

        if (requiredXp > 0) {
            if (!player.isCreative() && com.r3ct.base_core.client.screen.ArcaneLecternMenu.getTotalExperience(player) < requiredXp) return;
            if (!player.isCreative()) {
                com.r3ct.base_core.client.screen.ArcaneLecternMenu.removeExperience(player, requiredXp);
            }
        } else {
            return;
        }

        boolean appliedAny = false;

        for (int i = 0; i < maxSlots; i++) {
            Slot stagingSlot = menu.getSlot(i);
            ItemStack stagedStack = stagingSlot.getItem();

            if (!stagedStack.isEmpty() && stagedStack.has(ModDataComponents.EFFECT_ID)) {
                ItemStack oldStack = coreBE.getItem(i);
                if (!oldStack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(oldStack);
                }

                coreBE.setItem(i, stagedStack.copy());
                stagingSlot.set(ItemStack.EMPTY);
                appliedAny = true;
            }
        }

        if (appliedAny) {
            coreBE.forceSync();
            level.playSound(null, payload.pos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            player.sendSystemMessage(Component.translatable("r3ct_base_core.message.effects_applied").withStyle(ChatFormatting.AQUA), true);

            List<String> activeEffects = coreBE.getActiveEffectsFromTomes();

            try {
                UUID ownerId = UUID.fromString(coreBE.getOwnerUUID());
                PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);
                data.activeSlots = new java.util.ArrayList<>(activeEffects);
                ModState.get(level.getServer()).setDirty();
            } catch (IllegalArgumentException ignored) {}

            boolean allFull = true;
            for (int i = 0; i < 4; i++) {
                if (i < maxSlots && coreBE.getItem(i).isEmpty()) {
                    allFull = false;
                    break;
                } else if (i >= maxSlots) {
                    allFull = false;
                }
            }

            if (allFull && maxSlots == 4) grantAdvancement(player, "all_slots");
            if (!activeEffects.isEmpty()) grantAdvancement(player, "first_effect");
            if (activeEffects.contains("crop_growth") && activeEffects.contains("anti_trample")) grantAdvancement(player, "farming_combo");
            if (activeEffects.contains("industrial_overclock") && activeEffects.contains("fuel_efficiency")) grantAdvancement(player, "industrial_and_fuel");
            if (activeEffects.contains("livestock_boost") && activeEffects.contains("twin_breeding")) grantAdvancement(player, "breeding_and_growth");
            if (activeEffects.contains("anti_explosion") && activeEffects.contains("hostile_slowness")) grantAdvancement(player, "pacified_base");
            if (activeEffects.contains("anti_explosion") && activeEffects.contains("hostile_slowness") && activeEffects.contains("anti_spawn")) grantAdvancement(player, "absolute_defense");
        }
    }

    public static void handleRemoveEffectRequest(ServerPlayer player, com.r3ct.base_core.network.RemoveEffectPayload payload) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof BaseCoreBlockEntity coreBE)) return;
        if (!coreBE.getOwnerUUID().equals(player.getUUID().toString())) return;

        int slot = payload.slotIndex();
        if (slot >= 0 && slot < 4) {
            ItemStack activeStack = coreBE.getItem(slot);
            if (!activeStack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(activeStack);
                coreBE.setItem(slot, ItemStack.EMPTY);
                coreBE.forceSync();

                try {
                    UUID ownerId = UUID.fromString(coreBE.getOwnerUUID());
                    PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);
                    data.activeSlots = new java.util.ArrayList<>(coreBE.getActiveEffectsFromTomes());
                    ModState.get(level.getServer()).setDirty();
                } catch (IllegalArgumentException ignored) {}

                level.playSound(null, payload.pos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
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
            Item requiredInput = recipe.getInputItem();
            Item requiredIngredient = recipe.getIngredientItem();
            int requiredAmount = recipe.ingredientAmount();

            if (player.getInventory().countItem(requiredInput) < 1) return;
            if (player.getInventory().countItem(requiredIngredient) < requiredAmount) return;

            Slot inputSlot = menu.getSlot(0);
            Slot ingredientSlot = menu.getSlot(1);

            if (inputSlot.hasItem()) {
                player.getInventory().placeItemBackInInventory(inputSlot.getItem());
                inputSlot.set(ItemStack.EMPTY);
            }
            if (ingredientSlot.hasItem()) {
                player.getInventory().placeItemBackInInventory(ingredientSlot.getItem());
                ingredientSlot.set(ItemStack.EMPTY);
            }

            if (consumeItems(player.getInventory(), requiredInput, 1)) {
                inputSlot.set(new ItemStack(requiredInput, 1));
            }
            if (consumeItems(player.getInventory(), requiredIngredient, requiredAmount)) {
                ingredientSlot.set(new ItemStack(requiredIngredient, requiredAmount));
            }
        });
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

        if (count < amountNeeded) return false;

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

    private static void consumeFromStaging(BaseCoreMenu menu, Item item, int amountNeeded, int startIdx, int endIdx) {
        if (amountNeeded <= 0 || item == Items.AIR) return;
        int left = amountNeeded;

        for (int i = startIdx; i <= endIdx; i++) {
            if (left <= 0) break;
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();

            if (stack.is(item)) {
                int take = Math.min(stack.getCount(), left);
                slot.remove(take);
                left -= take;
            }
        }
    }

    public static void handleSendMail(ServerPlayer player, SendMailPayload payload) {
        net.minecraft.core.BlockPos pos = payload.pos();
        BlockEntity be = player.level().getBlockEntity(pos);

        if (be instanceof MailboxBlockEntity mailboxBE) {
            int slot = payload.slotIndex();
            if (slot < 0 || slot >= 27) return;

            if (!mailboxBE.getMessages().get(slot).isEmpty()) {
                player.sendSystemMessage(Component.literal("Ten slot jest już zajęty!").withStyle(ChatFormatting.RED));
                return;
            }

            net.minecraft.core.NonNullList<ItemStack> attachedItems = net.minecraft.core.NonNullList.withSize(3, ItemStack.EMPTY);

            for (int i = 0; i < 3; i++) {
                ItemStack stackInSlot = player.containerMenu.getSlot(i).getItem();
                if (!stackInSlot.isEmpty()) {
                    attachedItems.set(i, stackInSlot.copy());
                    player.containerMenu.getSlot(i).set(ItemStack.EMPTY);
                }
            }

            MailMessage message = new MailMessage(player.getName().getString(), payload.message(), attachedItems);

            if (message.isEmpty()) {
                player.sendSystemMessage(Component.literal("Nie możesz wysłać pustego listu!").withStyle(ChatFormatting.RED));
                return;
            }

            boolean wasEmpty = true;
            for (MailMessage m : mailboxBE.getMessages()) {
                if (!m.isEmpty()) {
                    wasEmpty = false;
                    break;
                }
            }

            mailboxBE.getMessages().set(slot, message);
            mailboxBE.forceSync();

            if (wasEmpty) {
                BlockState state = player.level().getBlockState(pos);
                if (state.getBlock() instanceof com.r3ct.base_core.block.MailboxBlock) {
                    player.level().setBlock(pos, state.setValue(com.r3ct.base_core.block.MailboxBlock.HAS_MAIL, true), 3);
                }
            }

            player.level().playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("Wiadomość została wysłana!").withStyle(ChatFormatting.GREEN));
            player.closeContainer();
        }
    }

    public static void handleCollectMail(ServerPlayer player, CollectMailPayload payload) {
        net.minecraft.core.BlockPos pos = payload.pos();
        BlockEntity be = player.level().getBlockEntity(pos);

        if (be instanceof MailboxBlockEntity mailboxBE) {
            if (!mailboxBE.getOwnerUUID().equals(player.getUUID().toString())) return;

            int index = payload.messageIndex();
            if (index >= 0 && index < 27) {
                MailMessage msg = mailboxBE.getMessages().get(index);
                if (msg.isEmpty()) return;

                for (ItemStack stack : msg.getAttachedItems()) {
                    if (!stack.isEmpty()) {
                        if (!player.getInventory().add(stack)) {
                            ItemEntity drop = player.drop(stack, false);
                            if (drop != null) drop.setNoPickUpDelay();
                        }
                    }
                }

                mailboxBE.getMessages().set(index, MailMessage.EMPTY);
                mailboxBE.forceSync();

                boolean isNowEmpty = true;
                for (MailMessage m : mailboxBE.getMessages()) {
                    if (!m.isEmpty()) {
                        isNowEmpty = false;
                        break;
                    }
                }

                if (isNowEmpty) {
                    BlockState state = player.level().getBlockState(pos);
                    if (state.getBlock() instanceof com.r3ct.base_core.block.MailboxBlock && state.getValue(com.r3ct.base_core.block.MailboxBlock.HAS_MAIL)) {
                        player.level().setBlock(pos, state.setValue(com.r3ct.base_core.block.MailboxBlock.HAS_MAIL, false), 3);
                    }
                }

                player.level().playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
            }
        }
    }

    public static void handleCancelMail(ServerPlayer player, com.r3ct.base_core.network.CancelMailPayload payload) {
        if (player.containerMenu instanceof com.r3ct.base_core.client.screen.MailboxVisitorMenu menu) {
            for (int i = 0; i < 3; i++) {
                net.minecraft.world.inventory.Slot slot = menu.getSlot(i);
                if (slot.hasItem()) {
                    player.getInventory().placeItemBackInInventory(slot.getItem().copy());
                    slot.set(net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
        }
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