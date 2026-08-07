package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.LecternRecipeDef;
import com.r3ct.base_core.logic.LecternRecipes;
import com.r3ct.base_core.network.LecternAutoFillPayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ArcaneLecternScreen extends AbstractContainerScreen<ArcaneLecternMenu> {

    private String selectedRecipeId = null;

    private final int panelW = 146;
    private final int recipeRowHeight = 28;
    private double scrollOffset = 0;
    private boolean isDraggingScrollbar = false;

    public ArcaneLecternScreen(ArcaneLecternMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
    }

    private int getMaxScroll() {
        int listHeight = LecternRecipes.getRecipes().size() * recipeRowHeight;
        int visibleHeight = this.imageHeight - 24;
        return Math.max(0, listHeight - visibleHeight);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {

        int panelX = this.leftPos - panelW - 4;
        int panelY = this.topPos;

        drawPlanks(graphics, panelX, panelY, panelW, this.imageHeight);
        drawPlanks(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
            graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
            graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);
        }

        int plusCenterX = this.leftPos + 52 - (this.font.width("+") / 2);
        graphics.text(this.font, "+", plusCenterX, this.topPos + 39, 0xFFEFEBE9, false);

        int arrowCenterX = this.leftPos + 110 - (this.font.width("->") / 2);
        graphics.text(this.font, "->", arrowCenterX, this.topPos + 39, 0xFFEFEBE9, false);

        int listStartX = panelX + 4;
        int listStartY = panelY + 20;
        int maxScroll = getMaxScroll();

        ItemStack hoveredItem = null;
        Component hoveredRecipeText = null;

        graphics.enableScissor(listStartX, listStartY, listStartX + panelW - 12, panelY + this.imageHeight - 4);

        List<LecternRecipeDef> recipes = LecternRecipes.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            LecternRecipeDef recipe = recipes.get(i);
            int rowY = listStartY + (i * recipeRowHeight) - (int) scrollOffset;

            if (rowY + recipeRowHeight < listStartY || rowY > panelY + this.imageHeight) continue;

            boolean isSelected = recipe.id().equals(selectedRecipeId);
            boolean isHovered = mouseX >= listStartX && mouseX < listStartX + panelW - 14 && mouseY >= rowY && mouseY < rowY + recipeRowHeight;

            int bgColor = isSelected ? 0xFF8D6E63 : (isHovered ? 0xFF634631 : 0xFF3E2723);
            graphics.fill(listStartX, rowY, listStartX + panelW - 14, rowY + recipeRowHeight - 2, bgColor);
            drawThickOutline(graphics, listStartX, rowY, panelW - 14, recipeRowHeight - 2, 1, 0xFF2E1F14);

            ItemStack inputStack = new ItemStack(recipe.getInputItem());
            ItemStack ingStack = new ItemStack(recipe.getIngredientItem(), recipe.ingredientAmount());

            ItemStack outputStack = com.r3ct.base_core.item.EmpoweredTomeItem.createFromRecipe(recipe.getOutputItem(), recipe);

            graphics.item(inputStack, listStartX + 4, rowY + 5);
            graphics.text(this.font, "+", listStartX + 24, rowY + 9, 0xFFEFEBE9, false);

            graphics.item(ingStack, listStartX + 34, rowY + 5);
            graphics.itemDecorations(this.font, ingStack, listStartX + 34, rowY + 5);

            graphics.text(this.font, "->", listStartX + 54, rowY + 9, 0xFFEFEBE9, false);

            graphics.item(outputStack, listStartX + 68, rowY + 5);

            if (recipe.xpCost() > 0) {
                boolean hasEnoughListXp = this.minecraft.player.isCreative() || ArcaneLecternMenu.getTotalExperience(this.minecraft.player) >= recipe.xpCost();
                int xpColor = hasEnoughListXp ? 0xFF55FF55 : 0xFFFF5555;
                Component costComponent = Component.literal(String.valueOf(recipe.xpCost())).append(" ").append(Component.translatable("r3ct_base_core.gui.xp"));
                graphics.text(this.font, costComponent, listStartX + 89, rowY + 9, xpColor, true);
            }

            if (isHovered) {
                boolean onInput = mouseX >= listStartX + 4 && mouseX < listStartX + 20 && mouseY >= rowY + 5 && mouseY < rowY + 21;
                boolean onIng = mouseX >= listStartX + 34 && mouseX < listStartX + 50 && mouseY >= rowY + 5 && mouseY < rowY + 21;
                boolean onOut = mouseX >= listStartX + 68 && mouseX < listStartX + 84 && mouseY >= rowY + 5 && mouseY < rowY + 21;

                if (onInput) hoveredItem = inputStack;
                else if (onIng) hoveredItem = ingStack;
                else if (onOut) hoveredItem = outputStack;
                else hoveredRecipeText = Component.translatable(recipe.nameKey()).withStyle(ChatFormatting.GOLD);
            }
        }
        graphics.disableScissor();

        if (hoveredItem != null) {
            graphics.setTooltipForNextFrame(this.font, hoveredItem, mouseX, mouseY);
        } else if (hoveredRecipeText != null) {
            graphics.setTooltipForNextFrame(this.font, hoveredRecipeText, mouseX, mouseY);
        }

        int scrollbarX = panelX + panelW - 10;
        int scrollbarY = panelY + 20;
        int scrollbarH = this.imageHeight - 24;

        graphics.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarH, 0xFF2E1F14);

        if (maxScroll > 0) {
            int thumbH = Math.max(12, (int) (((float) scrollbarH / (recipes.size() * recipeRowHeight)) * scrollbarH));
            int thumbY = scrollbarY + (int) ((scrollOffset / maxScroll) * (scrollbarH - thumbH));

            int thumbColor = isDraggingScrollbar ? 0xFFA1887F : 0xFF79553A;
            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + 5, thumbY + thumbH, thumbColor);
        }

        int currentXpCost = this.menu.xpCost.get();
        if (currentXpCost > 0) {
            boolean hasEnoughXp = this.minecraft.player.isCreative() || ArcaneLecternMenu.getTotalExperience(this.minecraft.player) >= currentXpCost;

            Component prefix = Component.translatable("r3ct_base_core.gui.cost").append(": ").withStyle(ChatFormatting.GRAY);
            Component xpAmount = Component.literal(String.valueOf(currentXpCost)).append(" ").append(Component.translatable("r3ct_base_core.gui.xp")).withStyle(hasEnoughXp ? ChatFormatting.GREEN : ChatFormatting.RED);
            Component fullText = Component.empty().append(prefix).append(xpAmount);

            int textW = this.font.width(fullText);
            int textX = this.leftPos + 146 - (textW / 2);

            if (textX + textW > this.leftPos + this.imageWidth - 6) {
                textX = this.leftPos + this.imageWidth - 6 - textW;
            }

            graphics.text(this.font, fullText, textX, this.topPos + 58, 0xFFFFFFFF, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);

        if (this.selectedRecipeId != null) {
            LecternRecipes.getRecipeById(this.selectedRecipeId).ifPresent(recipe -> {
                if (!this.menu.getSlot(0).hasItem()) {
                    int s0x = this.leftPos + this.menu.getSlot(0).x;
                    int s0y = this.topPos + this.menu.getSlot(0).y;
                    graphics.fakeItem(new ItemStack(recipe.getInputItem()), s0x, s0y);
                    graphics.fill(s0x, s0y, s0x + 16, s0y + 16, 0x66FFFFFF);
                }
                if (!this.menu.getSlot(1).hasItem()) {
                    int s1x = this.leftPos + this.menu.getSlot(1).x;
                    int s1y = this.topPos + this.menu.getSlot(1).y;
                    graphics.fakeItem(new ItemStack(recipe.getIngredientItem(), recipe.ingredientAmount()), s1x, s1y);
                    graphics.fill(s1x, s1y, s1x + 16, s1y + 16, 0x66FFFFFF);
                }
                if (!this.menu.getSlot(2).hasItem()) {
                    int s2x = this.leftPos + this.menu.getSlot(2).x;
                    int s2y = this.topPos + this.menu.getSlot(2).y;

                    ItemStack ghostOutput = com.r3ct.base_core.item.EmpoweredTomeItem.createFromRecipe(recipe.getOutputItem(), recipe);

                    graphics.fakeItem(ghostOutput, s2x, s2y);
                    graphics.fill(s2x, s2y, s2x + 16, s2y + 16, 0x66FFFFFF);
                }
            });
        }
    }

    @Override
    protected void extractLabels(@NonNull GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFEFEBE9, true);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFEFEBE9, true);
        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.available_recipes"), -panelW + 6, 6, 0xFFEFEBE9, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int panelX = this.leftPos - panelW - 4;
        int panelY = this.topPos;

        int scrollbarX = panelX + panelW - 10;
        if (mouseX >= scrollbarX && mouseX <= scrollbarX + 6 && mouseY >= panelY + 20 && mouseY <= panelY + this.imageHeight - 4) {
            this.isDraggingScrollbar = true;
            return true;
        }

        int listStartX = panelX + 4;
        int listStartY = panelY + 20;

        if (mouseX >= listStartX && mouseX < listStartX + panelW - 14 && mouseY >= listStartY && mouseY < panelY + this.imageHeight - 4) {
            int clickY = (int) (mouseY - listStartY + scrollOffset);
            int clickedIndex = clickY / recipeRowHeight;

            if (clickedIndex >= 0 && clickedIndex < LecternRecipes.getRecipes().size()) {
                LecternRecipeDef clickedRecipe = LecternRecipes.getRecipes().get(clickedIndex);
                this.selectedRecipeId = clickedRecipe.id();

                Services.PLATFORM.sendToServer(new LecternAutoFillPayload(clickedRecipe.id()));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isDraggingScrollbar) {
            int scrollbarH = this.imageHeight - 24;
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                scrollOffset += dy * (maxScroll / (double) scrollbarH);
                scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double rawMouseX, double rawMouseY, double scrollX, double scrollY) {
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (scrollY * 14), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(rawMouseX, rawMouseY, scrollX, scrollY);
    }

    private void drawPlanks(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF4A3424);

        for (int i = y; i < y + h; i += 16) {
            int lineY = i;
            if (lineY > y && lineY < y + h - 1) {
                graphics.fill(x, lineY, x + w, lineY + 1, 0xFF2E1F14);
                graphics.fill(x, lineY + 1, x + w, lineY + 2, 0xFF634631);
            }
        }
        drawThickOutline(graphics, x, y, w, h, 2, 0xFF2E1F14);
    }

    private void drawThickOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int thickness, int color) {
        graphics.fill(x - thickness, y - thickness, x + w + thickness, y, color);
        graphics.fill(x - thickness, y + h, x + w + thickness, y + h + thickness, color);
        graphics.fill(x - thickness, y, x, y + h, color);
        graphics.fill(x + w, y, x + w + thickness, y + h, color);
    }
}