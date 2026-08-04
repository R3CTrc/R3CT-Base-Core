package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BaseCoreMenu extends AbstractContainerMenu {

    private final ContainerData data;
    public boolean isOverviewTab = true;

    public final SimpleContainer stagingContainer;

    public BaseCoreMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(5));
    }

    public BaseCoreMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.BASE_CORE_MENU, containerId);
        checkContainerDataCount(data, 5);

        this.data = data;
        this.stagingContainer = new SimpleContainer(12);
        this.addDataSlots(data);

        int effStartX = 14;
        int effStartY = 105;
        for (int i = 0; i < 4; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(stagingContainer, i, effStartX + (i * 42) + 3, effStartY + 3) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.has(ModDataComponents.EFFECT_ID);
                }
                @Override
                public boolean isActive() {
                    return BaseCoreMenu.this.isOverviewTab && slotIndex < BaseCoreServerConfig.calculateTotalSlots(getTier());
                }
            });
        }

        for (int i = 0; i < 4; ++i) {
            final int localI = i;
            this.addSlot(new Slot(stagingContainer, 4 + i, 77 + (i * 18), 56) {
                @Override
                public boolean isActive() {
                    if (isOverviewTab) return false;
                    BaseCoreServerConfig.TierUpgrade nextTier = BaseCoreServerConfig.getTier(getTier() + 1);
                    if (nextTier == null) return false;
                    int needed = (int) Math.ceil(nextTier.mainAmount / 64.0);
                    return localI < needed;
                }
                @Override
                public boolean mayPlace(ItemStack stack) {
                    BaseCoreServerConfig.TierUpgrade nextTier = BaseCoreServerConfig.getTier(getTier() + 1);
                    if (nextTier == null) return false;
                    Item reqItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTier.mainItem)).map(Holder::value).orElse(Items.AIR);
                    return stack.is(reqItem);
                }
            });
        }

        for (int i = 0; i < 4; ++i) {
            final int localI = i;
            this.addSlot(new Slot(stagingContainer, 8 + i, 77 + (i * 18), 78) {
                @Override
                public boolean isActive() {
                    if (isOverviewTab) return false;
                    BaseCoreServerConfig.TierUpgrade nextTier = BaseCoreServerConfig.getTier(getTier() + 1);
                    if (nextTier == null) return false;
                    int needed = (int) Math.ceil(nextTier.bulkAmount / 64.0);
                    return localI < needed;
                }
                @Override
                public boolean mayPlace(ItemStack stack) {
                    BaseCoreServerConfig.TierUpgrade nextTier = BaseCoreServerConfig.getTier(getTier() + 1);
                    if (nextTier == null) return false;
                    Item reqItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTier.bulkItem)).map(Holder::value).orElse(Items.AIR);
                    return stack.is(reqItem);
                }
            });
        }

        this.addStandardInventorySlots(playerInventory, 8, 142);
    }

    public int getTier() { return this.data.get(0); }
    public boolean isBorderVisible() { return this.data.get(1) != 0; }
    public net.minecraft.core.BlockPos getCorePos() {
        return new net.minecraft.core.BlockPos(this.data.get(2), this.data.get(3), this.data.get(4));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 12) {
                if (!this.moveItemStackTo(itemstack1, 12, 48, true)) return ItemStack.EMPTY;
            } else {
                if (isOverviewTab && itemstack1.has(ModDataComponents.EFFECT_ID)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 4, false)) return ItemStack.EMPTY;
                } else if (!isOverviewTab) {
                    if (!this.moveItemStackTo(itemstack1, 4, 12, false)) return ItemStack.EMPTY;
                }
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
        this.clearContainer(player, this.stagingContainer);
    }
}