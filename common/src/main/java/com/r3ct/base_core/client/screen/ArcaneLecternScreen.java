package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.EffectDef;
import com.r3ct.base_core.logic.LecternRecipes;
import com.r3ct.base_core.network.LecternAutoFillPayload;
import com.r3ct.base_core.network.LecternCraftPayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ArcaneLecternScreen extends AbstractContainerScreen<ArcaneLecternMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/anvil.png");

    private String selectedRecipeId = null;
    private Button craftButton;

    public ArcaneLecternScreen(ArcaneLecternMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);

        this.titleLabelX = 60;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.craftButton = Button.builder(Component.literal("Zaklinaj"), button -> {
            if (this.selectedRecipeId != null) {
                Services.PLATFORM.sendToServer(new LecternCraftPayload(this.selectedRecipeId));
            }
        }).bounds(this.leftPos + 105, this.topPos + 58, 60, 20).build();

        this.craftButton.active = false;
        this.addRenderableWidget(this.craftButton);

        List<EffectDef> recipes = LecternRecipes.RECIPES;
        for (int i = 0; i < recipes.size(); i++) {
            EffectDef recipe = recipes.get(i);
            int col = i % 2;
            int row = i / 2;

            int btnX = this.leftPos - 110 + (col * 52);
            int btnY = this.topPos + 10 + (row * 22);

            Button recipeBtn = Button.builder(Component.translatable(recipe.name()), btn -> {
                this.selectedRecipeId = recipe.id();
                this.craftButton.active = true;
                Services.PLATFORM.sendToServer(new LecternAutoFillPayload(recipe.id()));
            }).bounds(btnX, btnY, 50, 20).build();

            this.addRenderableWidget(recipeBtn);
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        graphics.fill(this.leftPos - 115, this.topPos, this.leftPos - 5, this.topPos + this.imageHeight, 0xAA000000);

        graphics.blit(TEXTURE, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0.0f, (float) this.imageWidth / 256.0f, 0.0f, (float) this.imageHeight / 256.0f);

        super.extractContents(graphics, mouseX, mouseY, a);

        if (this.selectedRecipeId != null) {
            net.minecraft.world.inventory.Slot outputSlot = this.menu.getSlot(2);

            if (!outputSlot.hasItem()) {
                LecternRecipes.getRecipeById(this.selectedRecipeId).ifPresent(recipe -> {

                    ItemStack ghostStack = new ItemStack(Items.ENCHANTED_BOOK);

                    int ghostX = this.leftPos + outputSlot.x;
                    int ghostY = this.topPos + outputSlot.y;

                    graphics.fakeItem(ghostStack, ghostX, ghostY);

                    graphics.fill(ghostX, ghostY, ghostX + 16, ghostY + 16, 0x66FFFFFF);
                });
            }
        }
    }

    @Override
    protected void extractLabels(@NonNull GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        graphics.text(this.font, Component.literal("Dostępne Księgi"), -110, 0, 0xFFFFFF, true);
    }
}