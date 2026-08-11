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

public class MailboxOwnerMenu extends AbstractContainerMenu {
    public boolean isMessageView = false;
    public final Container attachmentContainer;
    private final ContainerData data;

    public MailboxOwnerMenu(int containerId, Inventory playerInventory, MailboxBlockEntity blockEntity) {
        this(containerId, playerInventory, new SimpleContainerData(3));
        this.data.set(0, blockEntity.getBlockPos().getX());
        this.data.set(1, blockEntity.getBlockPos().getY());
        this.data.set(2, blockEntity.getBlockPos().getZ());
    }

    public MailboxOwnerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(3));
    }

    public MailboxOwnerMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.MAILBOX_OWNER_MENU, containerId);
        this.data = data;
        this.addDataSlots(data);
        this.attachmentContainer = new SimpleContainer(3);

        for (int i = 0; i < 3; ++i) {
            this.addSlot(new Slot(this.attachmentContainer, i, 24 + (i * 20), 76) {
                @Override
                public boolean isActive() {
                    return isMessageView;
                }
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

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 142 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 200));
        }
    }

    public void setAttachments(net.minecraft.core.NonNullList<ItemStack> items) {
        this.attachmentContainer.clearContent();
        for (int i = 0; i < Math.min(items.size(), 3); i++) {
            this.attachmentContainer.setItem(i, items.get(i).copy());
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
        this.attachmentContainer.clearContent();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 3) {
                return ItemStack.EMPTY;
            } else {
                if (index >= 3 && index < 30) {
                    if (!this.moveItemStackTo(itemstack1, 30, 39, false)) return ItemStack.EMPTY;
                } else if (index >= 30 && index < 39) {
                    if (!this.moveItemStackTo(itemstack1, 3, 30, false)) return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemstack1);
        }
        return itemstack;
    }
}