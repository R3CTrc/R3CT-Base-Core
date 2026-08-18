package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.data.MailMessage;
import com.r3ct.base_core.network.CollectMailPayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class MailboxOwnerScreen extends AbstractContainerScreen<MailboxOwnerMenu> {

    private MultiLineEditBox messageBox;
    private int selectedMessageIndex = -1;

    public MailboxOwnerScreen(MailboxOwnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 224);
    }

    @Override
    protected void init() {
        super.init();

        this.messageBox = MultiLineEditBox.builder()
                .setX(this.leftPos + 7)
                .setY(this.topPos + 24)
                .setShowBackground(false)
                .setTextColor(0xFF333333)
                .setTextShadow(false)
                .build(this.font, 162, 54, Component.empty());

        this.messageBox.visible = false;
        this.messageBox.active = false;

        this.addRenderableWidget(this.messageBox);
    }

    @Override
    protected void extractLabels(@NonNull GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, Component.translatable("block.r3ct_base_core.mailbox"), 8, 6, 0xFFEFEBE9, true);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xFFEFEBE9, true);
    }

    private com.r3ct.base_core.block.MailboxBlockEntity getMailboxEntity() {
        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.getMailboxPos());
            if (be instanceof com.r3ct.base_core.block.MailboxBlockEntity mailbox) {
                return mailbox;
            }
        }
        return null;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCopperPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.isActive() && slot.index >= 3) {
                drawSlotBackground(graphics, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }

        if (!this.menu.isMessageView) {
            renderListView(graphics, mouseX, mouseY);
        } else {
            renderMessageView(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderListView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.messageBox.visible = false;
        com.r3ct.base_core.block.MailboxBlockEntity mailbox = getMailboxEntity();

        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.inbox"),
                this.leftPos + 8, this.topPos + 34, 0xFFEFEBE9, true);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int sx = this.leftPos + 7 + col * 18;
                int sy = this.topPos + 46 + row * 18;

                drawSlotBackground(graphics, sx, sy);

                boolean isOccupied = mailbox != null && !mailbox.getMessages().get(index).isEmpty();

                if (isOccupied) {
                    graphics.fakeItem(new ItemStack(Items.PAPER), sx, sy);
                }

                if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);

                    if (isOccupied) {
                        String sender = mailbox.getMessages().get(index).getSenderName();
                        Component tooltip = Component.literal("Od: ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(sender).withStyle(ChatFormatting.GOLD));
                        graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }

    private void renderMessageView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.messageBox.visible = true;
        com.r3ct.base_core.block.MailboxBlockEntity mailbox = getMailboxEntity();
        if (mailbox == null) return;

        drawPaperBackground(graphics, this.leftPos + 7, this.topPos + 24, 162, 54);

        MailMessage msg = mailbox.getMessages().get(this.selectedMessageIndex);

        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.from").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(msg.getSenderName()).withStyle(ChatFormatting.GOLD)),
                this.leftPos + 8, this.topPos + 12, 0xFFFFFF, false);

        for (int i = 0; i < 3; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            drawSlotBackground(graphics, this.leftPos + slot.x, this.topPos + slot.y);
        }

        int btnCollectX = this.leftPos + 109;
        int btnCollectY = this.topPos + 98;
        int btnCancelX = this.leftPos + 109;
        int btnCancelY = this.topPos + 118;

        int[] pal = getCopperPalette();

        boolean hoverCollect = mouseX >= btnCollectX && mouseX < btnCollectX + 60 && mouseY >= btnCollectY && mouseY < btnCollectY + 16;
        int collectColor = hoverCollect ? pal[1] : pal[0];

        graphics.fill(btnCollectX, btnCollectY, btnCollectX + 60, btnCollectY + 16, collectColor);
        drawThickOutline(graphics, btnCollectX, btnCollectY, 60, 16, 1, pal[3]);
        centeredText(graphics, Component.translatable("r3ct_base_core.gui.collect"), btnCollectX + 30, btnCollectY + 4, 0xFFFFFFFF);

        boolean hoverCancel = mouseX >= btnCancelX && mouseX < btnCancelX + 60 && mouseY >= btnCancelY && mouseY < btnCancelY + 16;
        int cancelColor = hoverCancel ? pal[1] : pal[0];

        graphics.fill(btnCancelX, btnCancelY, btnCancelX + 60, btnCancelY + 16, cancelColor);
        drawThickOutline(graphics, btnCancelX, btnCancelY, 60, 16, 1, pal[3]);
        centeredText(graphics, Component.translatable("r3ct_base_core.gui.cancel"), btnCancelX + 30, btnCancelY + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();

            if (!this.menu.isMessageView) {
                com.r3ct.base_core.block.MailboxBlockEntity mailbox = getMailboxEntity();
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int index = row * 9 + col;
                        int sx = this.leftPos + 7 + col * 18;
                        int sy = this.topPos + 46 + row * 18;

                        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                            boolean isOccupied = mailbox != null && !mailbox.getMessages().get(index).isEmpty();

                            if (isOccupied) {
                                this.selectedMessageIndex = index;
                                this.menu.isMessageView = true;
                                MailMessage msg = mailbox.getMessages().get(index);

                                this.messageBox.setValue(msg.getMessage());
                                this.menu.setAttachments(msg.getAttachedItems());
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            }
                            return true;
                        }
                    }
                }
            } else {
                int btnCollectX = this.leftPos + 109;
                int btnCollectY = this.topPos + 98;
                int btnCancelX = this.leftPos + 109;
                int btnCancelY = this.topPos + 118;

                if (mouseX >= btnCollectX && mouseX < btnCollectX + 60 && mouseY >= btnCollectY && mouseY < btnCollectY + 16) {
                    if (this.selectedMessageIndex != -1) {
                        Services.PLATFORM.sendToServer(new CollectMailPayload(this.menu.getMailboxPos(), this.selectedMessageIndex));
                        this.closeMessageView();
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }

                if (mouseX >= btnCancelX && mouseX < btnCancelX + 60 && mouseY >= btnCancelY && mouseY < btnCancelY + 16) {
                    this.closeMessageView();
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void closeMessageView() {
        this.menu.isMessageView = false;
        this.selectedMessageIndex = -1;
        this.messageBox.setValue("");
        this.menu.attachmentContainer.clearContent();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.menu.isMessageView && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.closeMessageView();
            return true;
        }
        return super.keyPressed(event);
    }

    private void drawSlotBackground(GuiGraphicsExtractor graphics, int sx, int sy) {
        graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
        graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
        graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);
        graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
    }

    private int[] getCopperPalette() {
        int[] colors = new int[]{0xFFB96C4D, 0xFFD2825E, 0xFF914E36, 0xFF54291B};

        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.world.level.block.Block block = this.minecraft.level.getBlockState(this.menu.getMailboxPos()).getBlock();

            if (block == com.r3ct.base_core.block.ModBlocks.EXPOSED_MAILBOX || block == com.r3ct.base_core.block.ModBlocks.WAXED_EXPOSED_MAILBOX) {
                colors = new int[]{0xFFA07361, 0xFFBA8A75, 0xFF7E5748, 0xFF4A3127};
            } else if (block == com.r3ct.base_core.block.ModBlocks.WEATHERED_MAILBOX || block == com.r3ct.base_core.block.ModBlocks.WAXED_WEATHERED_MAILBOX) {
                colors = new int[]{0xFF5D9078, 0xFF71A78B, 0xFF456D59, 0xFF2A4235};
            } else if (block == com.r3ct.base_core.block.ModBlocks.OXIDIZED_MAILBOX || block == com.r3ct.base_core.block.ModBlocks.WAXED_OXIDIZED_MAILBOX) {
                colors = new int[]{0xFF3BA18B, 0xFF4BB59D, 0xFF297C6A, 0xFF174C40};
            }
        }
        return colors;
    }

    private void drawCopperPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int[] pal = getCopperPalette();
        graphics.fill(x, y, x + w, y + h, pal[0]);
        graphics.fill(x, y, x + w, y + 2, pal[1]);
        graphics.fill(x, y, x + 2, y + h, pal[1]);
        graphics.fill(x, y + h - 2, x + w, y + h, pal[2]);
        graphics.fill(x + w - 2, y, x + w, y + h, pal[2]);

        graphics.fill(x + 4, y + 4, x + 6, y + 6, pal[3]);
        graphics.fill(x + 5, y + 5, x + 7, y + 7, pal[1]);
        graphics.fill(x + w - 6, y + 4, x + w - 4, y + 6, pal[3]);
        graphics.fill(x + w - 5, y + 5, x + w - 3, y + 7, pal[1]);
        graphics.fill(x + 4, y + h - 6, x + 6, y + h - 4, pal[3]);
        graphics.fill(x + 5, y + h - 5, x + 7, y + h - 3, pal[1]);
        graphics.fill(x + w - 6, y + h - 6, x + w - 4, y + h - 4, pal[3]);
        graphics.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, pal[1]);

        drawThickOutline(graphics, x, y, w, h, 2, pal[3]);
    }

    private void drawPaperBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x - 1, y - 1, x + w + 1, y, 0xFFC2A878);
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFFC2A878);
        graphics.fill(x - 1, y, x, y + h, 0xFFC2A878);
        graphics.fill(x + w, y, x + w + 1, y + h, 0xFFC2A878);
        graphics.fill(x, y, x + w, y + h, 0xFFF4E5C3);
    }

    private void drawThickOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int thickness, int color) {
        graphics.fill(x - thickness, y - thickness, x + w + thickness, y, color);
        graphics.fill(x - thickness, y + h, x + w + thickness, y + h + thickness, color);
        graphics.fill(x - thickness, y, x, y + h, color);
        graphics.fill(x + w, y, x + w + thickness, y + h, color);
    }

    private void centeredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(this.font, text, x - this.font.width(text) / 2, y, color, true);
    }
}