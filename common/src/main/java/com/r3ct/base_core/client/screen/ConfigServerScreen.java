package com.r3ct.base_core.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Paths;

public class ConfigServerScreen extends Screen {
    private final Screen parent;

    public ConfigServerScreen(Screen parent) {
        super(Component.translatable("r3ct_base_core.config.server.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 20;

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct_base_core.config.server.button.main"), button -> openFile("r3ct_base_core_server.json"))
                .bounds(centerX, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX, startY + 40, buttonWidth, buttonHeight).build());
    }

    private void openFile(String fileName) {
        File configFile = Paths.get("config", "r3ct_base_core", fileName).toFile();
        if (configFile.exists()) {
            Util.getPlatform().openUri(configFile.toURI());
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}