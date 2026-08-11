package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.network.SendMailPayload;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

public class MailboxVisitorScreen extends AbstractContainerScreen<MailboxVisitorMenu> {

    private MultiLineEditBox messageBox;
    private int selectedSlotIndex = -1;

    public MailboxVisitorScreen(MailboxVisitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 224);
    }

    @Override
    protected void init() {
        super.init();

        this.messageBox = MultiLineEditBox.builder()
                .setX(this.leftPos + 18)
                .setY(this.topPos + 24)
                .setShowBackground(false)
                .setTextColor(0xFF333333)
                .setTextShadow(false)
                .build(this.font, 140, 42, Component.empty());

        this.messageBox.setCharacterLimit(256);
        this.messageBox.visible = false;

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
    protected void slotClicked(net.minecraft.world.inventory.Slot slot, int slotId, int mouseButton, ContainerInput type) {
        if (slot != null && type == ContainerInput.QUICK_MOVE) {
            if (!this.menu.isComposeView) {
                return;
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCopperPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.isActive() && slot.index >= 3) {
                drawSlotBackground(graphics, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }

        if (!this.menu.isComposeView) {
            renderListView(graphics, mouseX, mouseY);
        } else {
            renderComposeView(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderListView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.messageBox.visible = false;
        com.r3ct.base_core.block.MailboxBlockEntity mailbox = getMailboxEntity();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int sx = this.leftPos + 7 + col * 18;
                int sy = this.topPos + 24 + row * 18;

                drawSlotBackground(graphics, sx, sy);

                boolean isOccupied = mailbox != null && !mailbox.getMessages().get(index).isEmpty();

                if (isOccupied) {
                    graphics.fakeItem(new ItemStack(Items.PAPER), sx, sy);
                }

                if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);

                    if (isOccupied) {
                        String sender = mailbox.getMessages().get(index).getSenderName();
                        Component tooltip = Component.literal("Zajęte: ").withStyle(ChatFormatting.RED)
                                .append(Component.literal(sender).withStyle(ChatFormatting.GOLD));
                        graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                    } else {
                        Component tooltip = Component.literal("Pusto").withStyle(ChatFormatting.DARK_GRAY);
                        graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }

    private void renderComposeView(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.messageBox.visible = true;

        drawPaperBackground(graphics, this.leftPos + 18, this.topPos + 24, 140, 42);

        for (int i = 0; i < 3; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            drawSlotBackground(graphics, this.leftPos + slot.x, this.topPos + slot.y);
        }

        boolean hasText = !this.messageBox.getValue().trim().isEmpty();
        boolean hasItems = !this.menu.getSlot(0).getItem().isEmpty() || !this.menu.getSlot(1).getItem().isEmpty() || !this.menu.getSlot(2).getItem().isEmpty();
        boolean canSend = hasText || hasItems;

        int btnSendX = this.leftPos + 96;
        int btnSendY = this.topPos + 68;
        boolean hoverSend = mouseX >= btnSendX && mouseX < btnSendX + 60 && mouseY >= btnSendY && mouseY < btnSendY + 16;

        int sendColor = 0xFFB06A3B;
        if (canSend) {
            if (hoverSend) {
                sendColor = 0xFF000000 | (30 << 16) | (150 << 8) | 30;
            } else {
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
                int g = (int)(100 + 50 * pulse);
                sendColor = 0xFF000000 | (30 << 16) | (g << 8) | 30;
            }
        } else if (hoverSend) {
            sendColor = 0xFFC27E4D;
        }

        graphics.fill(btnSendX, btnSendY, btnSendX + 60, btnSendY + 16, sendColor);
        drawThickOutline(graphics, btnSendX, btnSendY, 60, 16, 1, 0xFF4A2511);
        centeredText(graphics, Component.literal("Wyślij"), btnSendX + 30, btnSendY + 4, canSend ? 0xFFFFFFFF : 0xFFBBBBBB);

        int btnCancelX = this.leftPos + 96;
        int btnCancelY = this.topPos + 88;
        boolean hoverCancel = mouseX >= btnCancelX && mouseX < btnCancelX + 60 && mouseY >= btnCancelY && mouseY < btnCancelY + 16;
        int cancelColor = hoverCancel ? 0xFFC27E4D : 0xFFB06A3B;

        graphics.fill(btnCancelX, btnCancelY, btnCancelX + 60, btnCancelY + 16, cancelColor);
        drawThickOutline(graphics, btnCancelX, btnCancelY, 60, 16, 1, 0xFF4A2511);
        centeredText(graphics, Component.literal("Anuluj"), btnCancelX + 30, btnCancelY + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();

            if (!this.menu.isComposeView) {
                com.r3ct.base_core.block.MailboxBlockEntity mailbox = getMailboxEntity();
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 9; col++) {
                        int index = row * 9 + col;
                        int sx = this.leftPos + 7 + col * 18;
                        int sy = this.topPos + 24 + row * 18;

                        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                            boolean isOccupied = mailbox != null && !mailbox.getMessages().get(index).isEmpty();

                            if (!isOccupied) {
                                this.selectedSlotIndex = index;
                                this.menu.isComposeView = true;
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            } else {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.5F));
                            }
                            return true;
                        }
                    }
                }
            } else {
                int btnSendX = this.leftPos + 96;
                int btnSendY = this.topPos + 68;
                int btnCancelX = this.leftPos + 96;
                int btnCancelY = this.topPos + 88;

                boolean hasText = !this.messageBox.getValue().trim().isEmpty();
                boolean hasItems = !this.menu.getSlot(0).getItem().isEmpty() || !this.menu.getSlot(1).getItem().isEmpty() || !this.menu.getSlot(2).getItem().isEmpty();

                if (mouseX >= btnSendX && mouseX < btnSendX + 60 && mouseY >= btnSendY && mouseY < btnSendY + 16) {
                    if (hasText || hasItems) {
                        Services.PLATFORM.sendToServer(new SendMailPayload(this.menu.getMailboxPos(), this.selectedSlotIndex, this.messageBox.getValue()));
                        this.messageBox.setValue("");
                        this.menu.isComposeView = false;
                        this.selectedSlotIndex = -1;
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }

                if (mouseX >= btnCancelX && mouseX < btnCancelX + 60 && mouseY >= btnCancelY && mouseY < btnCancelY + 16) {
                    this.messageBox.setValue("");
                    this.menu.isComposeView = false;
                    this.selectedSlotIndex = -1;
                    Services.PLATFORM.sendToServer(new com.r3ct.base_core.network.CancelMailPayload(this.menu.getMailboxPos()));
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.menu.isComposeView && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.messageBox.setValue("");
            this.menu.isComposeView = false;
            this.selectedSlotIndex = -1;
            Services.PLATFORM.sendToServer(new com.r3ct.base_core.network.CancelMailPayload(this.menu.getMailboxPos()));
            return true;
        }

        if (this.menu.isComposeView && this.messageBox.keyPressed(event)) {
            return true;
        }

        if (this.menu.isComposeView && this.messageBox.isFocused()) {
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

    private void drawCopperPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFFB06A3B);

        graphics.fill(x, y, x + w, y + 2, 0xFFC27E4D);
        graphics.fill(x, y, x + 2, y + h, 0xFFC27E4D);

        graphics.fill(x, y + h - 2, x + w, y + h, 0xFF7A4526);
        graphics.fill(x + w - 2, y, x + w, y + h, 0xFF7A4526);

        int rivetShadow = 0xFF4A2511;
        int rivetLight = 0xFFC27E4D;

        graphics.fill(x + 4, y + 4, x + 6, y + 6, rivetShadow);
        graphics.fill(x + 5, y + 5, x + 7, y + 7, rivetLight);
        graphics.fill(x + w - 6, y + 4, x + w - 4, y + 6, rivetShadow);
        graphics.fill(x + w - 5, y + 5, x + w - 3, y + 7, rivetLight);
        graphics.fill(x + 4, y + h - 6, x + 6, y + h - 4, rivetShadow);
        graphics.fill(x + 5, y + h - 5, x + 7, y + h - 3, rivetLight);
        graphics.fill(x + w - 6, y + h - 6, x + w - 4, y + h - 4, rivetShadow);
        graphics.fill(x + w - 5, y + h - 5, x + w - 3, y + h - 3, rivetLight);

        drawThickOutline(graphics, x, y, w, h, 2, 0xFF4A2511);
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