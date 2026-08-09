package com.r3ct.base_core.client.screen;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BaseCoreVisitorMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final Container container;

    public BaseCoreVisitorMenu(int containerId, Inventory playerInventory) {
        this(containerId, new SimpleContainer(4), new SimpleContainerData(5));
    }

    public BaseCoreVisitorMenu(int containerId, Container container, ContainerData data) {
        super(ModMenuTypes.BASE_CORE_VISITOR_MENU, containerId);
        this.container = container;
        this.data = data;
        this.addDataSlots(data);

        int[] xPositions = {22, 60, 98, 136};
        for (int i = 0; i < 4; ++i) {
            this.addSlot(new Slot(container, i, xPositions[i], 161) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player playerIn) {
                    return false;
                }
            });
        }
    }

    public int getTier() { return this.data.get(0); }
    public net.minecraft.core.BlockPos getCorePos() {
        return new net.minecraft.core.BlockPos(this.data.get(2), this.data.get(3), this.data.get(4));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}