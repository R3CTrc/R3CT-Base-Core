package com.r3ct.base_core.block;

import com.r3ct.base_core.data.MailMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class MailboxBlockEntity extends BlockEntity implements MenuProvider {

    private String ownerUUID = "";

    private final net.minecraft.core.NonNullList<MailMessage> messages = net.minecraft.core.NonNullList.withSize(27, MailMessage.EMPTY);

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MAILBOX_BE_TYPE, pos, state);
    }

    public void setOwnerUUID(String uuid) {
        this.ownerUUID = uuid;
        this.setChanged();
        syncToClient();
    }

    public String getOwnerUUID() {
        return this.ownerUUID;
    }

    public net.minecraft.core.NonNullList<MailMessage> getMessages() {
        return this.messages;
    }

    public boolean isCompletelyEmpty() {
        for (MailMessage msg : this.messages) {
            if (!msg.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isFull() {
        for (MailMessage msg : this.messages) {
            if (msg.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void forceSync() {
        this.setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.ownerUUID != null && !this.ownerUUID.isEmpty()) {
            output.putString("OwnerUUID", this.ownerUUID);
        }

        ValueOutput.TypedOutputList<MailMessage> outputList = output.list("Messages", MailMessage.CODEC);
        for (MailMessage msg : this.messages) {
            outputList.add(msg);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getString("OwnerUUID").ifPresent(uuid -> this.ownerUUID = uuid);

        for (int i = 0; i < 27; i++) {
            this.messages.set(i, MailMessage.EMPTY);
        }

        int index = 0;
        for (MailMessage msg : input.listOrEmpty("Messages", MailMessage.CODEC)) {
            if (index >= 27) break;
            this.messages.set(index, msg);
            index++;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.r3ct_base_core.mailbox");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.r3ct.base_core.client.screen.MailboxOwnerMenu(containerId, playerInventory, this);
    }

    public MenuProvider getVisitorMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.r3ct_base_core.mailbox");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new com.r3ct.base_core.client.screen.MailboxVisitorMenu(id, inv, MailboxBlockEntity.this);
            }
        };
    }
}