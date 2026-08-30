package com.r3ct.base_core.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class ConfirmExtractScreen extends Screen {
    private final Screen parent;
    private final ItemStack stackToExtract;
    private final Runnable onConfirm;

    public ConfirmExtractScreen(Screen parent, ItemStack stackToExtract, Runnable onConfirm) {
        super(Component.translatable("r3ct_base_core.gui.confirm_extract.title"));
        this.parent = parent;
        this.stackToExtract = stackToExtract;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int btnY = this.height / 2 + 30;

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct_base_core.gui.confirm_extract.yes").withStyle(ChatFormatting.GREEN), button -> {
            this.onConfirm.run();
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(parent);
            }
        }).bounds(centerX - 105, btnY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct_base_core.gui.cancel").withStyle(ChatFormatting.RED), button -> {
            this.onClose();
        }).bounds(centerX + 5, btnY, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.translatable("r3ct_base_core.gui.confirm_extract.warning").withStyle(ChatFormatting.RED), this.width / 2, this.height / 2 - 30, 0xFFFFFFFF);

        int itemX = this.width / 2 - 8;
        int itemY = this.height / 2 - 5;

        guiGraphics.fill(itemX - 2, itemY - 2, itemX + 18, itemY + 18, 0x44FFFFFF);
        guiGraphics.item(this.stackToExtract, itemX, itemY);
        guiGraphics.itemDecorations(this.font, this.stackToExtract, itemX, itemY);

        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
            guiGraphics.setTooltipForNextFrame(this.font, this.stackToExtract.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, TooltipFlag.NORMAL), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}