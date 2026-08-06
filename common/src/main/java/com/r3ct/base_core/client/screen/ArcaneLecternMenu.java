package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.LecternRecipeDef;
import com.r3ct.base_core.logic.LecternRecipes;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArcaneLecternMenu extends AbstractContainerMenu {

    private final Container container;

    public final DataSlot xpCost = DataSlot.standalone();
    public String activeRecipeId = null;
    private boolean isUpdating = false;

    public ArcaneLecternMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3));
    }

    public ArcaneLecternMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenuTypes.ARCANE_LECTERN_MENU, containerId);

        checkContainerSize(container, 3);
        this.container = container;
        container.startOpen(playerInventory.player);

        this.addDataSlot(this.xpCost);

        this.addSlot(new Slot(container, 0, 22, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                updateCraftingResult();
            }
        });

        this.addSlot(new Slot(container, 1, 66, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                updateCraftingResult();
            }
        });

        this.addSlot(new Slot(container, 2, 138, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return (player.isCreative() || getTotalExperience(player) >= xpCost.get()) && super.mayPickup(player);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                LecternRecipeDef recipe = LecternRecipes.getRecipeById(activeRecipeId).orElse(null);
                if (recipe != null) {
                    ArcaneLecternMenu.this.container.getItem(0).shrink(1);
                    ArcaneLecternMenu.this.container.getItem(1).shrink(recipe.ingredientAmount());

                    if (!player.level().isClientSide() && !player.isCreative()) {
                        removeExperience(player, recipe.xpCost());
                    }
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                updateCraftingResult();
                super.onTake(player, stack);
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        updateCraftingResult();
    }

    public void updateCraftingResult() {
        if (isUpdating) return;
        isUpdating = true;

        ItemStack input = this.container.getItem(0);
        ItemStack ingredient = this.container.getItem(1);

        if (input.isEmpty() || ingredient.isEmpty()) {
            this.container.setItem(2, ItemStack.EMPTY);
            this.xpCost.set(0);
            this.activeRecipeId = null;
            isUpdating = false;
            return;
        }

        for (LecternRecipeDef recipe : LecternRecipes.getRecipes()) {
            if (input.is(recipe.getInputItem()) && ingredient.is(recipe.getIngredientItem()) && ingredient.getCount() >= recipe.ingredientAmount()) {

                ItemStack result = com.r3ct.base_core.item.EmpoweredTomeItem.createFromRecipe(recipe.getOutputItem(), recipe);

                this.container.setItem(2, result);
                this.xpCost.set(recipe.xpCost());
                this.activeRecipeId = recipe.id();
                isUpdating = false;
                return;
            }
        }

        this.container.setItem(2, ItemStack.EMPTY);
        this.xpCost.set(0);
        this.activeRecipeId = null;
        isUpdating = false;
    }

    public static int getTotalExperience(Player player) {
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

    public static void removeExperience(Player player, int amount) {
        int newTotalExp = Math.max(0, getTotalExperience(player) - amount);
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;
        player.giveExperiencePoints(newTotalExp);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index == 2) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (index != 0 && index != 1) {
                if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}