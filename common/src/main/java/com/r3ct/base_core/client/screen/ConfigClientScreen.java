package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ConfigClientScreen extends Screen {
    private final Screen parent;
    private EditBox guiScaleBox;

    public ConfigClientScreen(Screen parent) {
        super(Component.translatable("r3ct_base_core.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rightColumnX = this.width / 2 + 20;
        int widgetWidth = 140;
        int widgetHeight = 20;

        this.guiScaleBox = new EditBox(this.font, rightColumnX, 60, widgetWidth, widgetHeight, Component.translatable("r3ct_base_core.config.entry.gui_scale"));
        this.guiScaleBox.setValue(String.valueOf(BaseCoreClientConfig.getInstance().guiScale));
        this.addRenderableWidget(this.guiScaleBox);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int leftColumnX = this.width / 2 - 160;

        guiGraphics.text(this.font, Component.translatable("r3ct_base_core.config.entry.gui_scale"), leftColumnX, 60 + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        try {
            BaseCoreClientConfig.getInstance().guiScale = Float.parseFloat(this.guiScaleBox.getValue());
        } catch (NumberFormatException ignored) {}

        BaseCoreClientConfig.save();

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}