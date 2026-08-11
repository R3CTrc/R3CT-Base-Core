package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.block.MailboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MailboxVisitorMenu extends AbstractContainerMenu {
    public boolean isComposeView = false;
    private final Container attachmentContainer;
    private final ContainerData data;

    public MailboxVisitorMenu(int containerId, Inventory playerInventory, MailboxBlockEntity blockEntity) {
        this(containerId, playerInventory, new SimpleContainerData(3));
        this.data.set(0, blockEntity.getBlockPos().getX());
        this.data.set(1, blockEntity.getBlockPos().getY());
        this.data.set(2, blockEntity.getBlockPos().getZ());
    }

    public MailboxVisitorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(3));
    }

    public MailboxVisitorMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.MAILBOX_VISITOR_MENU, containerId);
        this.data = data;
        this.addDataSlots(data);
        this.attachmentContainer = new SimpleContainer(3);

        for (int i = 0; i < 3; ++i) {
            this.addSlot(new Slot(this.attachmentContainer, i, 24 + (i * 20), 76) {
                @Override
                public boolean isActive() {
                    return isComposeView;
                }
            });
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 142 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 200));
        }
    }

    public BlockPos getMailboxPos() {
        return new BlockPos(this.data.get(0), this.data.get(1), this.data.get(2));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.attachmentContainer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 3) {
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(itemstack1, 0, 3, false)) return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }
}