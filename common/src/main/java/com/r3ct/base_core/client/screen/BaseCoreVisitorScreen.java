package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public class BaseCoreVisitorScreen extends AbstractContainerScreen<BaseCoreVisitorMenu> {

    private static final int CUSTOM_IMAGE_WIDTH = 176;
    private static final int CUSTOM_IMAGE_HEIGHT = 190;

    public BaseCoreVisitorScreen(BaseCoreVisitorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, CUSTOM_IMAGE_WIDTH, CUSTOM_IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void extractLabels(@NonNull GuiGraphicsExtractor graphics, int xm, int ym) { }

    private com.r3ct.base_core.block.BaseCoreBlockEntity getCoreEntity() {
        if (this.minecraft != null && this.minecraft.level != null) {
            net.minecraft.world.level.block.entity.BlockEntity be = this.minecraft.level.getBlockEntity(this.menu.getCorePos());
            if (be instanceof com.r3ct.base_core.block.BaseCoreBlockEntity coreBE) {
                return coreBE;
            }
        }
        return null;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawPlanks(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(this.menu.getTier());
        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
            graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
            graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);

            if (slot.index >= maxSlots) {
                String ghostId = (slot.index == 0 || slot.index == 1) ? "r3ct_base_core:magic_tome" : (slot.index == 2 ? "r3ct_base_core:alchemy_tome" : "r3ct_base_core:dark_magic_tome");
                Item ghostItem = BuiltInRegistries.ITEM.get(Identifier.parse(ghostId)).map(Holder::value).orElse(Items.BOOK);
                graphics.fakeItem(new ItemStack(ghostItem), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x66FFFFFF);
                centeredText(graphics, "X", sx + 8, sy + 4, 0xFFFF5555);
            } else if (!slot.hasItem()) {
                String ghostId = (slot.index == 0 || slot.index == 1) ? "r3ct_base_core:magic_tome" : (slot.index == 2 ? "r3ct_base_core:alchemy_tome" : "r3ct_base_core:dark_magic_tome");
                Item ghostItem = BuiltInRegistries.ITEM.get(Identifier.parse(ghostId)).map(Holder::value).orElse(Items.BOOK);
                graphics.fakeItem(new ItemStack(ghostItem), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x66FFFFFF);
            }
        }

        Component titleDisplay = Component.translatable("block.r3ct_base_core.base_core");
        Component ownerDisplay = this.title.copy().withStyle(ChatFormatting.AQUA);

        graphics.text(this.font, titleDisplay, this.leftPos + 12, this.topPos + 6, 0xFFEFEBE9, true);
        graphics.text(this.font, ownerDisplay, this.leftPos + this.imageWidth - this.font.width(ownerDisplay) - 12, this.topPos + 6, 0xFFEFEBE9, true);

        int tier = this.menu.getTier();
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(tier);
        Component titleComp = currentTierConfig != null ? Component.translatable(currentTierConfig.title) : Component.translatable("r3ct_base_core.gui.tier.0");
        String romanTier = tier == 0 ? "0" : toRoman(tier);

        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.stats.tier"), this.leftPos + 12, this.topPos + 22, 0xFFEFEBE9, true);

        Item currentMain = Items.STICK;
        if (currentTierConfig != null) {
            currentMain = BuiltInRegistries.ITEM.get(Identifier.parse(currentTierConfig.mainItem)).map(Holder::value).orElse(Items.STICK);
            if (currentMain == Items.AIR) currentMain = Items.STICK;
        }

        int tierBoxX = this.leftPos + 76;
        int tierBoxY = this.topPos + 36;

        graphics.fill(tierBoxX, tierBoxY, tierBoxX + 24, tierBoxY + 24, 0xFF4A3424);
        drawThickOutline(graphics, tierBoxX, tierBoxY, 24, 24, 2, 0xFF2E1F14);
        graphics.fakeItem(new ItemStack(currentMain), tierBoxX + 4, tierBoxY + 4);

        centeredText(graphics, Component.empty().append(titleComp).append(" (").append(romanTier).append(")"), tierBoxX + 12, this.topPos + 64, 0xFFEFEBE9);

        if (mouseX >= tierBoxX && mouseX < tierBoxX + 24 && mouseY >= tierBoxY && mouseY < tierBoxY + 24) {
            if (currentTierConfig != null) {
                java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
                tooltipLines.add(Component.translatable("r3ct_base_core.gui.tier.format", Component.translatable(currentTierConfig.title), currentTierConfig.tierLevel).withStyle(net.minecraft.ChatFormatting.GOLD));
                if (currentTierConfig.bonusRadius > 0) {
                    tooltipLines.add(Component.literal("+ " + currentTierConfig.bonusRadius + " ").append(Component.translatable("r3ct_base_core.gui.stats.area")).withStyle(net.minecraft.ChatFormatting.AQUA));
                }
                if (currentTierConfig.bonusSlots > 0) {
                    tooltipLines.add(Component.literal("+ " + currentTierConfig.bonusSlots + " ").append(Component.translatable("r3ct_base_core.gui.stats.slots")).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
                graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
            }
        }

        int currentRange = calculateRangeUpToTier(tier);
        int diameterNum = currentRange == 0 ? 0 : (currentRange * 2 + 1);

        int infoX = this.leftPos + 12;
        int infoY = this.topPos + 78;
        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.stats.area"), infoX, infoY, 0xFFEFEBE9, true);

        int size = 32;
        int boxX = this.leftPos + 20;
        int boxY = this.topPos + 94;
        int lineColor = 0xFF1E90FF;
        int redColor = 0xFFFF5555;

        int frontX = boxX;
        int frontY = boxY + 16;
        int backX = boxX + 16;
        int backY = boxY;

        drawLine(graphics, backX, backY, backX + size, backY, lineColor);
        drawLine(graphics, backX + size, backY, backX + size, backY + size, lineColor);
        drawLine(graphics, frontX, frontY, backX, backY, lineColor);
        drawLine(graphics, frontX + size, frontY, backX + size, backY, lineColor);
        drawLine(graphics, frontX + size, frontY + size, backX + size, backY + size, lineColor);
        drawLine(graphics, frontX, frontY, frontX + size, frontY, lineColor);
        drawLine(graphics, frontX, frontY, frontX, frontY + size, lineColor);
        drawLine(graphics, frontX + size, frontY, frontX + size, frontY + size, lineColor);
        drawLine(graphics, frontX, frontY + size, frontX + size, frontY + size, lineColor);
        drawDashedLine(graphics, backX, backY, backX, backY + size, lineColor);
        drawDashedLine(graphics, backX, backY + size, backX + size, backY + size, lineColor);

        drawLine(graphics, frontX, frontY + size, frontX + size, frontY + size, redColor);
        drawLine(graphics, frontX, frontY, frontX, frontY + size, redColor);
        drawDashedLine(graphics, frontX, frontY + size, backX, backY + size, redColor);

        int cx = frontX + (size / 2) + 8;
        int cy = frontY + (size / 2) - 8;
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF8D6E63);
        drawLine(graphics, cx, cy, cx - (size / 2), cy, 0xFF00AA00);

        graphics.text(this.font, currentRange + " " + Component.translatable("r3ct_base_core.gui.stats.blocks").getString(), boxX + 58, boxY + 12, 0xFF55FF55, true);
        graphics.text(this.font, diameterNum + " " + Component.translatable("r3ct_base_core.gui.stats.blocks").getString(), boxX + 58, boxY + 28, 0xFFFF5555, true);

        int effectsY = this.topPos + 149;
        graphics.text(this.font, Component.translatable("r3ct_base_core.gui.stats.effects"), infoX, effectsY, 0xFFEFEBE9, true);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        renderActiveEffectsPanel(graphics, mouseX, mouseY);
    }

    private void renderActiveEffectsPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        com.r3ct.base_core.block.BaseCoreBlockEntity core = getCoreEntity();
        if (core == null) return;

        java.util.List<String> activeEffects = core.getActiveEffectsFromTomes();
        if (activeEffects.isEmpty()) return;

        int maxTextWidth = 0;
        Component statusText = Component.literal("Aktywny");
        int statusWidth = this.font.width(statusText);

        for (String effectId : activeEffects) {
            Component effectName = Component.translatable("r3ct_base_core.effect." + effectId + ".name");
            int nameWidth = this.font.width(effectName);
            if (nameWidth > maxTextWidth) maxTextWidth = nameWidth;
        }

        if (statusWidth > maxTextWidth) maxTextWidth = statusWidth;

        int panelWidth = Math.max(120, 32 + maxTextWidth + 10);
        int panelX = this.leftPos + this.imageWidth + 2;
        int panelY = this.topPos;
        int yStep = 33;

        Identifier bgSprite = Identifier.withDefaultNamespace("container/inventory/effect_background");

        for (int i = 0; i < activeEffects.size(); i++) {
            String effectId = activeEffects.get(i);
            int rowY = panelY + (i * yStep);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, bgSprite, panelX, rowY, panelWidth, 32);

            Identifier iconSprite = Identifier.fromNamespaceAndPath("r3ct_base_core", "mob_effect/" + effectId);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, panelX + 7, rowY + 7, 18, 18);

            Component effectName = Component.translatable("r3ct_base_core.effect." + effectId + ".name");

            graphics.text(this.font, effectName, panelX + 32, rowY + 7, 0xFFFFFFFF, false);
            graphics.text(this.font, statusText, panelX + 32, rowY + 16, 0xFF808080, false);

            if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= rowY && mouseY < rowY + 32) {
                java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
                tooltipLines.add(Component.translatable("r3ct_base_core.effect." + effectId + ".desc.1").withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltipLines.add(Component.translatable("r3ct_base_core.effect." + effectId + ".desc.2").withStyle(net.minecraft.ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
            }
        }
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
    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }
    private void drawDashedLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dashLen = 4; int gapLen = 4;
        double dist = Math.hypot(x2 - x1, y2 - y1);
        if ((int) (dist / (dashLen + gapLen)) == 0) return;
        double dx = (x2 - x1) / dist, dy = (y2 - y1) / dist;
        for (int i = 0; i <= (int) (dist / (dashLen + gapLen)); i++) {
            int startX = x1 + (int) (dx * i * (dashLen + gapLen));
            int startY = y1 + (int) (dy * i * (dashLen + gapLen));
            int endX = startX + (int) (dx * dashLen);
            int endY = startY + (int) (dy * dashLen);
            if (i == (int) (dist / (dashLen + gapLen)) && Math.hypot(endX - x1, endY - y1) > dist) { endX = x2; endY = y2; }
            drawLine(graphics, startX, startY, endX, endY, color);
        }
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
    private void centeredText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.text(this.font, text, x - this.font.width(text) / 2, y, color, true);
    }
    private String toRoman(int num) {
        if (num <= 0) return "0";
        String[] romanSymbols = {"X", "IX", "V", "IV", "I"};
        int[] romanValues = {10, 9, 5, 4, 1};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < romanValues.length; i++) {
            while (num >= romanValues[i]) {
                num -= romanValues[i];
                result.append(romanSymbols[i]);
            }
        }
        return result.toString();
    }
    private int calculateRangeUpToTier(int currentTier) {
        if (currentTier == 0) return 0;
        int totalRange = 0;
        for (int i = 1; i <= currentTier; i++) {
            BaseCoreServerConfig.TierUpgrade tier = BaseCoreServerConfig.getTier(i);
            if (tier != null) totalRange += tier.bonusRadius;
        }
        return totalRange;
    }
}