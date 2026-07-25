package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ToggleBorderPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public class BaseCoreScreen extends AbstractContainerScreen<BaseCoreMenu> {

    private static final int CUSTOM_IMAGE_WIDTH = 280;
    private static final int CUSTOM_IMAGE_HEIGHT = 240;

    private Tab currentTab = Tab.OVERVIEW;

    private final int tabWidth = 100;
    private final int tabHeight = 22;
    private final int tabSpacing = 5;

    private boolean isBorderVisible = false;
    private final int toggleButtonSize = 26;
    private int toggleButtonX = 0;
    private int toggleButtonY = 0;

    public enum Tab {
        OVERVIEW("r3ct_base_core.gui.tab.overview"),
        UPGRADES("r3ct_base_core.gui.tab.upgrades");

        final String key;
        Tab(String key) { this.key = key; }
        public Component getComponent() { return Component.translatable(key); }
    }

    public BaseCoreScreen(BaseCoreMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, CUSTOM_IMAGE_WIDTH, CUSTOM_IMAGE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.isBorderVisible = this.menu.isBorderVisible();
        this.menu.isOverviewTab = (this.currentTab == Tab.OVERVIEW);
    }

    @Override
    protected void extractLabels(@NonNull GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int totalTabsWidth = (tabWidth * 2) + tabSpacing;
        int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
        int tabY = this.topPos - tabHeight + 2;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int currentTabX = startX + (i * (tabWidth + tabSpacing));
            boolean isSelected = (currentTab == tab);
            boolean isHovered = mouseX >= currentTabX && mouseX < currentTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight;
            renderCustomTab(graphics, currentTabX, tabY, tabWidth, tabHeight, tab.getComponent(), isSelected, isHovered);
        }

        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFF5DEB3);
        drawThickOutline(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 2, 0xFF3E2723);

        int innerMargin = 8;
        graphics.fill(this.leftPos + innerMargin, this.topPos + 18,
                this.leftPos + this.imageWidth - innerMargin, this.topPos + 138,
                0xFFFFF8DC);
        drawThickOutline(graphics, this.leftPos + innerMargin, this.topPos + 18,
                this.imageWidth - (innerMargin * 2), 120, 2, 0xFF8D6E63);

        int invX = this.leftPos + 59;
        int invY = this.topPos + 145;

        graphics.fill(invX - 2, invY - 2, invX + 164, invY + 56, 0xFF3E2723);
        graphics.fill(invX - 1, invY - 1, invX + 163, invY + 55, 0xFFC6C6C6);

        graphics.fill(invX - 2, invY + 56, invX + 164, invY + 78, 0xFF3E2723);
        graphics.fill(invX - 1, invY + 57, invX + 163, invY + 77, 0xFFC6C6C6);

        for (net.minecraft.world.inventory.Slot slot : this.menu.slots) {
            if (slot.isActive() && slot.index >= 4) {
                int sx = this.leftPos + slot.x;
                int sy = this.topPos + slot.y;
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
                graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
                graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);
            }
        }

        int tier = this.menu.getTier();
        Component tierDisplay = tier == 0 ? Component.translatable("r3ct_base_core.gui.tier.0") : Component.translatable("r3ct_base_core.gui.tier.n", toRoman(tier));
        Component titleDisplay = Component.translatable("block.r3ct_base_core.base_core");

        graphics.text(this.font, titleDisplay, this.leftPos + 12, this.topPos + 6, 0xFF000000, false);
        graphics.text(this.font, tierDisplay, this.leftPos + this.imageWidth - this.font.width(tierDisplay) - 12, this.topPos + 6, 0xFF3E2723, false);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);

        if (currentTab == Tab.OVERVIEW) {
            renderOverviewTab(graphics, mouseX, mouseY);
        } else if (currentTab == Tab.UPGRADES) {
            renderUpgradesTab(graphics, mouseX, mouseY);
        }
    }

    private void renderCustomTab(GuiGraphicsExtractor graphics, int x, int y, int width, int height, Component text, boolean isSelected, boolean isHovered) {
        int bgColor = isSelected ? 0xFFF5DEB3 : (isHovered ? 0xFFA1887F : 0xFF8D6E63);
        int borderColor = 0xFF3E2723;
        int textColor = isSelected ? 0xFF000000 : (isHovered ? 0xFFFFFFFF : 0xFFEFEBE9);

        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x - 2, y - 2, x + width + 2, y, borderColor);
        graphics.fill(x - 2, y, x, y + height, borderColor);
        graphics.fill(x + width, y, x + width + 2, y + height, borderColor);

        if (!isSelected) {
            graphics.fill(x, y + height, x + width, y + height + 2, borderColor);
        } else {
            graphics.fill(x, y + height, x + width, y + height + 2, 0xFFF5DEB3);
        }

        int textX = x + (width - this.font.width(text)) / 2;
        graphics.text(this.font, text, textX, y + (height - 8) / 2, textColor, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();

            int totalTabsWidth = (tabWidth * 2) + tabSpacing;
            int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
            int tabY = this.topPos - tabHeight + 2;

            for (int i = 0; i < Tab.values().length; i++) {
                int currentTabX = startX + (i * (tabWidth + tabSpacing));
                if (mouseX >= currentTabX && mouseX < currentTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                    if (this.currentTab != Tab.values()[i]) {
                        this.currentTab = Tab.values()[i];
                        this.menu.isOverviewTab = (this.currentTab == Tab.OVERVIEW);
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }

            if (this.currentTab == Tab.OVERVIEW) {
                if (mouseX >= this.toggleButtonX && mouseX < this.toggleButtonX + this.toggleButtonSize &&
                        mouseY >= this.toggleButtonY && mouseY < this.toggleButtonY + this.toggleButtonSize) {
                    this.isBorderVisible = !this.isBorderVisible;
                    Services.PLATFORM.sendToServer(new ToggleBorderPayload(this.menu.getCorePos()));
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }

            if (this.currentTab == Tab.UPGRADES) {
                int currentTier = this.menu.getTier();
                BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);

                if (nextTierConfig != null) {
                    int btnWidth = 140;
                    int btnHeight = 20;
                    int btnX = this.leftPos + (this.imageWidth / 2) - (btnWidth / 2);
                    int btnY = this.topPos + 105;

                    if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
                        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

                        boolean canAfford = countItemInClientInventory(mainItem) >= nextTierConfig.mainAmount &&
                                countItemInClientInventory(bulkItem) >= nextTierConfig.bulkAmount;

                        if (canAfford) {
                            Services.PLATFORM.sendToServer(new UpgradeBaseCorePayload(this.menu.getCorePos()));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SMITHING_TABLE_USE, 1.0F));
                        }
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void renderOverviewTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int tier = this.menu.getTier();

        int infoX = this.leftPos + 20;
        int infoY = this.topPos + 20;
        int currentRange = calculateRangeUpToTier(tier);
        String diameterStr = currentRange == 0 ? "0x0x0" : (currentRange * 2 + 1) + "x" + (currentRange * 2 + 1) + "x" + (currentRange * 2 + 1);
        int diameterNum = currentRange == 0 ? 0 : (currentRange * 2 + 1);
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(tier);

        Component tierComp = Component.translatable("r3ct_base_core.gui.stats.tier").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(String.valueOf(tier)).withStyle(net.minecraft.ChatFormatting.AQUA));
        graphics.text(this.font, tierComp, infoX, infoY, 0xFF000000, false);

        Component areaComp = Component.translatable("r3ct_base_core.gui.stats.area").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(currentRange + " (" + diameterStr + ")").withStyle(net.minecraft.ChatFormatting.AQUA));
        graphics.text(this.font, areaComp, infoX, infoY + 15, 0xFF000000, false);

        Component slotsComp = Component.translatable("r3ct_base_core.gui.stats.slots").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(String.valueOf(maxSlots)).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        graphics.text(this.font, slotsComp, infoX, infoY + 30, 0xFF000000, false);

        int activeModulesCount = 0;
        for (int i = 0; i < 4; i++) {
            if (this.menu.getSlot(i).hasItem()) activeModulesCount++;
        }
        Component effectsComp = Component.translatable("r3ct_base_core.gui.stats.effects").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(String.valueOf(activeModulesCount)).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        graphics.text(this.font, effectsComp, infoX, infoY + 45, 0xFF000000, false);

        int size = 36;
        int boxX = this.leftPos + 206;
        int boxY = this.topPos + 20;
        int lineColor = 0xFF1E90FF;

        int frontX = boxX;
        int frontY = boxY + 18;
        int backX = boxX + 18;
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
        drawDashedLine(graphics, frontX, frontY + size, backX, backY + size, lineColor);

        int cx = frontX + (backX - frontX) / 2 + size / 2;
        int cy = frontY + (backY - frontY) / 2 + size / 2;

        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF8D6E63);
        drawLine(graphics, cx, cy, cx - size / 2, cy, 0xFF00AA00);

        centeredTextNoShadow(graphics, String.valueOf(currentRange), cx - 14, cy + 4, 0xFF00AA00);
        centeredTextNoShadow(graphics, String.valueOf(diameterNum), frontX + size / 2, frontY + size + 5, 0xFF444444);
        centeredTextNoShadow(graphics, String.valueOf(diameterNum), frontX - 8, frontY + size / 2 - 4, 0xFF444444);

        int visualSize = 26;
        int visualSpacing = 16;
        int startX = this.leftPos + 43;
        int startY = this.topPos + 96;

        for (int i = 0; i < 4; i++) {
            int sx = startX + (i * (visualSize + visualSpacing));
            boolean isLocked = i >= maxSlots;

            graphics.fill(sx, startY, sx + visualSize, startY + visualSize, isLocked ? 0xFFD7CCC8 : 0xFFF5DEB3);
            drawThickOutline(graphics, sx, startY, visualSize, visualSize, 2, 0xFF8D6E63);

            if (isLocked) {
                centeredTextNoShadow(graphics, "X", sx + (visualSize / 2), startY + (visualSize / 2) - 4, 0xFFFF5555);
            } else if (!this.menu.getSlot(i).hasItem()) {
                graphics.text(this.font, "+", sx + (visualSize - this.font.width("+")) / 2, startY + (visualSize - 8) / 2, 0xFF8D6E63, false);
            }
        }

        this.toggleButtonX = startX + (4 * (visualSize + visualSpacing));
        this.toggleButtonY = startY;
        boolean isHoveringToggle = mouseX >= toggleButtonX && mouseX < toggleButtonX + toggleButtonSize && mouseY >= toggleButtonY && mouseY < toggleButtonY + toggleButtonSize;

        graphics.fill(toggleButtonX, toggleButtonY, toggleButtonX + toggleButtonSize, toggleButtonY + toggleButtonSize, isHoveringToggle ? 0xFFE7CDB3 : 0xFFF5DEB3);
        drawThickOutline(graphics, toggleButtonX, toggleButtonY, toggleButtonSize, toggleButtonSize, 2, 0xFF8D6E63);

        graphics.fakeItem(new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS), toggleButtonX + 5, toggleButtonY + 5);

        if (this.isBorderVisible) {
            centeredTextNoShadow(graphics, "V", toggleButtonX + toggleButtonSize - 3, toggleButtonY + toggleButtonSize - 7, 0xFF00AA00);
        } else {
            centeredTextNoShadow(graphics, "X", toggleButtonX + toggleButtonSize - 3, toggleButtonY + toggleButtonSize - 7, 0xFFFF5555);
        }

        if (isHoveringToggle) {
            java.util.List<Component> toggleTooltip = new java.util.ArrayList<>();
            toggleTooltip.add(Component.translatable("r3ct_base_core.gui.tooltip.border_toggle.title").withStyle(net.minecraft.ChatFormatting.GOLD));
            toggleTooltip.add(Component.translatable(this.isBorderVisible ? "r3ct_base_core.gui.tooltip.border_toggle.on" : "r3ct_base_core.gui.tooltip.border_toggle.off").withStyle(this.isBorderVisible ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            graphics.setComponentTooltipForNextFrame(this.font, toggleTooltip, mouseX, mouseY);
        }
    }

    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int currentTier = this.menu.getTier();
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(currentTier);
        BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);

        int panelWidth = 240;
        int panelX = this.leftPos + 20;
        int panelY = this.topPos + 40;

        if (nextTierConfig == null) {
            centeredTextNoShadow(graphics, Component.translatable("r3ct_base_core.gui.upgrades.max_limit"), panelX + (panelWidth / 2), panelY + 30, 0xFF000000);
            return;
        }

        int centerX = panelX + (panelWidth / 2);

        int leftBoxX = centerX - 80;
        int boxY = panelY + 15;
        graphics.fill(leftBoxX, boxY, leftBoxX + 40, boxY + 40, 0xFFFFF8DC);
        drawThickOutline(graphics, leftBoxX, boxY, 40, 40, 2, 0xFF8D6E63);

        graphics.pose().pushMatrix();
        graphics.pose().translate(leftBoxX + 20, boxY + 20);
        graphics.pose().scale(2.0f, 2.0f);
        Item currentMain = currentTier == 0 ? Items.STICK : BuiltInRegistries.ITEM.get(Identifier.parse(currentTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        graphics.fakeItem(new ItemStack(currentMain), -8, -8);
        graphics.pose().popMatrix();

        int arrowX = leftBoxX + 45;
        int arrowY = boxY + 18;
        graphics.fill(arrowX, arrowY + 2, arrowX + 15, arrowY + 4, 0xFF8D6E63);
        graphics.fill(arrowX + 10, arrowY, arrowX + 12, arrowY + 6, 0xFF8D6E63);
        graphics.fill(arrowX + 12, arrowY + 1, arrowX + 15, arrowY + 5, 0xFF8D6E63);

        int rightBoxX = centerX + 40;
        graphics.fill(rightBoxX, boxY, rightBoxX + 40, boxY + 40, 0xFFFFF8DC);
        drawThickOutline(graphics, rightBoxX, boxY, 40, 40, 2, 0xFF8D6E63);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rightBoxX + 20, boxY + 20);
        graphics.pose().scale(2.0f, 2.0f);
        Item nextMainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        graphics.fakeItem(new ItemStack(nextMainItem), -8, -8);
        graphics.pose().popMatrix();

        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);
        int playerMainCount = countItemInClientInventory(nextMainItem);
        int playerBulkCount = countItemInClientInventory(bulkItem);
        boolean canAfford = playerMainCount >= nextTierConfig.mainAmount && playerBulkCount >= nextTierConfig.bulkAmount;

        int costX = panelX + 20;
        int costY = boxY + 50;

        int cappedMain = Math.min(playerMainCount, nextTierConfig.mainAmount);
        graphics.text(this.font, cappedMain + "/" + nextTierConfig.mainAmount + " " + nextMainItem.getName(nextMainItem.getDefaultInstance()).getString(), costX, costY, cappedMain >= nextTierConfig.mainAmount ? 0xFF55FF55 : 0xFFFF5555, false);

        int cappedBulk = Math.min(playerBulkCount, nextTierConfig.bulkAmount);
        graphics.text(this.font, cappedBulk + "/" + nextTierConfig.bulkAmount + " " + bulkItem.getName(bulkItem.getDefaultInstance()).getString(), costX, costY + 12, cappedBulk >= nextTierConfig.bulkAmount ? 0xFF55FF55 : 0xFFFF5555, false);

        int btnWidth = 140;
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = this.topPos + 110;

        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
        int pulseG = (int) (170 + (85 * pulse));
        int blinkColorBg = 0xFF000000 | (255 << 16) | (pulseG << 8);

        int btnColor = canAfford ? blinkColorBg : 0xFF8D6E63;
        if (isBtnHovered && !canAfford) btnColor = 0xFFA1887F;

        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
        drawThickOutline(graphics, btnX, btnY, btnWidth, btnHeight, 1, 0xFF5D4037);
        centeredTextNoShadow(graphics, Component.translatable("r3ct_base_core.gui.upgrades.start_upgrade"), centerX, btnY + 6, canAfford ? 0xFF000000 : 0xFFEFEBE9);

        if (mouseX >= leftBoxX && mouseX < leftBoxX + 40 && mouseY >= boxY && mouseY < boxY + 40) {
            if (currentTier != 0) renderTierTooltip(graphics, currentTierConfig, false, mouseX, mouseY);
        }
        if (mouseX >= rightBoxX && mouseX < rightBoxX + 40 && mouseY >= boxY && mouseY < boxY + 40) {
            renderTierTooltip(graphics, nextTierConfig, true, mouseX, mouseY);
        }
    }

    private void renderTierTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.TierUpgrade tierConfig, boolean isNextTier, int mouseX, int mouseY) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
        tooltipLines.add(Component.translatable("r3ct_base_core.gui.tier.format", Component.translatable(tierConfig.title), tierConfig.tierLevel).withStyle(net.minecraft.ChatFormatting.GOLD));
        if (tierConfig.bonusRadius > 0) tooltipLines.add(Component.translatable("r3ct_base_core.gui.tooltip.bonus_radius", tierConfig.bonusRadius).withStyle(net.minecraft.ChatFormatting.AQUA));
        if (tierConfig.bonusSlots > 0) tooltipLines.add(Component.translatable("r3ct_base_core.gui.tooltip.bonus_slots", tierConfig.bonusSlots).withStyle(net.minecraft.ChatFormatting.GREEN));
        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
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
        double dx = (x2 - x1) / dist;
        double dy = (y2 - y1) / dist;
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

    private void centeredTextNoShadow(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(this.font, text, x - this.font.width(text) / 2, y, color, false);
    }

    private void centeredTextNoShadow(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.text(this.font, text, x - this.font.width(text) / 2, y, color, false);
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

    private int countItemInClientInventory(Item itemType) {
        net.minecraft.client.player.LocalPlayer player = this.minecraft.player;
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == itemType) count += stack.getCount();
        }
        return count;
    }
}