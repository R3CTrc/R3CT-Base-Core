package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BaseCoreMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    public boolean isOverviewTab = true;

    public BaseCoreMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(4), new SimpleContainerData(5));
    }

    public BaseCoreMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.BASE_CORE_MENU, containerId);

        checkContainerSize(container, 4);
        checkContainerDataCount(data, 5);

        this.container = container;
        this.data = data;
        this.container.startOpen(playerInventory.player);
        this.addDataSlots(data);

        int slotVisualSize = 26;
        int slotSpacing = 16;
        int slotsStartX = 43;
        int slotsStartY = 96;

        for (int i = 0; i < 4; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(container, i, slotsStartX + (i * (slotVisualSize + slotSpacing)) + 5, slotsStartY + 5) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.has(ModDataComponents.EFFECT_ID);
                }
                @Override
                public boolean isActive() {
                    if (!BaseCoreMenu.this.isOverviewTab) return false;
                    return slotIndex < BaseCoreServerConfig.calculateTotalSlots(BaseCoreMenu.this.getTier());
                }
            });
        }

        int invX = 59;
        int invY = 145;

        this.addStandardInventorySlots(playerInventory, invX, invY);
    }

    public int getTier() { return this.data.get(0); }
    public boolean isBorderVisible() { return this.data.get(1) != 0; }
    public net.minecraft.core.BlockPos getCorePos() {
        return new net.minecraft.core.BlockPos(this.data.get(2), this.data.get(3), this.data.get(4));
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

            if (index < 4) {
                if (!this.moveItemStackTo(itemstack1, 4, 40, true)) return ItemStack.EMPTY;
            } else if (itemstack1.has(ModDataComponents.EFFECT_ID)) {
                if (!this.moveItemStackTo(itemstack1, 0, 4, false)) return ItemStack.EMPTY;
            } else if (index < 31) {
                if (!this.moveItemStackTo(itemstack1, 31, 40, false)) return ItemStack.EMPTY;
            } else if (index >= 31 && index < 40 && !this.moveItemStackTo(itemstack1, 4, 31, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
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