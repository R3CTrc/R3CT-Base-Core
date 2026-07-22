package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class BaseCoreScreen extends Screen {

    private final OpenBaseCoreGuiPayload data;
    private Tab currentTab = Tab.OVERVIEW;

    private int openedSlotDropdown = -1;

    private final int imageWidth = 360;
    private final int imageHeight = 240;
    private int leftPos;
    private int topPos;

    private final int tabWidth = 100;
    private final int tabHeight = 22;
    private final int tabSpacing = 5;

    public enum Tab {
        OVERVIEW("Przegląd"),
        EFFECTS("Sklep Efektów"),
        UPGRADES("Ulepszenia");

        final String name;
        Tab(String name) { this.name = name; }
    }

    public BaseCoreScreen(OpenBaseCoreGuiPayload data) {
        super(Component.literal("Serce Bazy"));
        this.data = data;
    }

    public float calculateEffectiveScale() {
        int actualWidth = this.width > 0 ? this.width : this.minecraft.getWindow().getGuiScaledWidth();
        int actualHeight = this.height > 0 ? this.height : this.minecraft.getWindow().getGuiScaledHeight();

        float maxPossibleScale = Math.min((float) actualWidth / (imageWidth + 20), (float) actualHeight / (imageHeight + 40));

        float configScale = com.r3ct.base_core.config.BaseCoreClientConfig.getInstance().guiScale;

        return Math.min(configScale, maxPossibleScale);
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int rawMouseX, int rawMouseY, float partialTick) {
        float scale = calculateEffectiveScale();
        int mouseX = (int)((rawMouseX - this.width / 2f) / scale + this.width / 2f);
        int mouseY = (int)((rawMouseY - this.height / 2f) / scale + this.height / 2f);

        graphics.pose().pushMatrix();
        graphics.pose().translate(this.width / 2f, this.height / 2f);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-this.width / 2f, -this.height / 2f);

        int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
        int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
        int tabY = this.topPos - tabHeight + 2;

        boolean blink = (System.currentTimeMillis() / 400L) % 2 == 0;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int currentTabX = startX + (i * (tabWidth + tabSpacing));
            boolean isSelected = (currentTab == tab);
            boolean isHovered = mouseX >= currentTabX && mouseX < currentTabX + tabWidth &&
                    mouseY >= tabY && mouseY < tabY + tabHeight;
            renderCustomTab(graphics, currentTabX, tabY, tabWidth, tabHeight, tab.name, isSelected, isHovered);
        }

        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFF5DEB3);
        drawThickOutline(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 2, 0xFF3E2723);

        int innerMargin = 8;
        graphics.fill(this.leftPos + innerMargin, this.topPos + 25,
                this.leftPos + this.imageWidth - innerMargin, this.topPos + this.imageHeight - innerMargin,
                0xFFFFF8DC);
        drawThickOutline(graphics, this.leftPos + innerMargin, this.topPos + 25,
                this.imageWidth - (innerMargin * 2), this.imageHeight - 25 - innerMargin, 2, 0xFF8D6E63);

        String tierDisplay = data.tier() == 0 ? "Baza (Poziom 0)" : "Poziom " + toRoman(data.tier());
        int titleWidth = this.font.width("Serce Bazy");
        int tierWidth = this.font.width(tierDisplay);

        graphics.text(this.font, "Serce Bazy", this.leftPos + 12, this.topPos + 10, 0xFF000000, false);
        graphics.text(this.font, tierDisplay, this.leftPos + this.imageWidth - tierWidth - 12, this.topPos + 10, 0xFF3E2723, false);
        graphics.fill(this.leftPos + 12, this.topPos + 21, this.leftPos + titleWidth + 12, this.topPos + 22, 0xFF8D6E63);

        switch (currentTab) {
            case OVERVIEW -> renderOverviewTab(graphics, mouseX, mouseY);
            case EFFECTS -> renderEffectsTab(graphics, mouseX, mouseY, blink);
            case UPGRADES -> renderUpgradesTab(graphics, mouseX, mouseY, blink);
        }

        if (openedSlotDropdown != -1) {
            renderSlotDropdown(graphics, mouseX, mouseY);
        }

        graphics.pose().popMatrix();
        super.extractRenderState(graphics, rawMouseX, rawMouseY, partialTick);
    }

    private void renderCustomTab(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String text, boolean isSelected, boolean isHovered) {
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

        int textWidth = this.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        graphics.text(this.font, text, textX, textY, textColor, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        if (event.button() == 0) {
            float scale = calculateEffectiveScale();
            int mouseX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mouseY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            if (openedSlotDropdown != -1) {
                handleDropdownClick(mouseX, mouseY);
                return true;
            }

            int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
            int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
            int tabY = this.topPos - tabHeight + 2;

            for (int i = 0; i < Tab.values().length; i++) {
                int currentTabX = startX + (i * (tabWidth + tabSpacing));
                if (mouseX >= currentTabX && mouseX < currentTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                    if (this.currentTab != Tab.values()[i]) {
                        this.currentTab = Tab.values()[i];
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }

            if (this.currentTab == Tab.OVERVIEW) {
                int slotSize = 36;
                int slotSpacing = 16;
                int totalSlotsWidth = (4 * slotSize) + (3 * slotSpacing);
                int slotsStartX = this.leftPos + (this.imageWidth - totalSlotsWidth) / 2;
                int slotsStartY = this.topPos + this.imageHeight - slotSize - 20;
                int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.tier());

                for (int i = 0; i < 4; i++) {
                    int slotX = slotsStartX + (i * (slotSize + slotSpacing));
                    if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotsStartY && mouseY < slotsStartY + slotSize) {
                        if (i < maxSlots) {
                            this.openedSlotDropdown = i;
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            if (this.currentTab == Tab.EFFECTS) {
                handleEffectClick(mouseX, mouseY);
                return true;
            }

            if (this.currentTab == Tab.UPGRADES) {
                int currentTier = data.tier();
                BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);

                if (nextTierConfig != null) {
                    int panelY = this.topPos + 40;
                    int costY = panelY + 90 + 15;
                    int btnWidth = 140;
                    int btnHeight = 20;
                    int btnX = this.leftPos + (this.imageWidth / 2) - (btnWidth / 2);
                    int btnY = costY + 40;

                    if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
                        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

                        boolean canAfford = countItemInClientInventory(mainItem) >= nextTierConfig.mainAmount &&
                                countItemInClientInventory(bulkItem) >= nextTierConfig.bulkAmount;

                        if (canAfford) {
                            Services.PLATFORM.sendToServer(new UpgradeBaseCorePayload(data.pos()));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SMITHING_TABLE_USE, 1.0F));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void renderOverviewTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int infoX = this.leftPos + 20;
        int infoY = this.topPos + 45;
        int currentRange = calculateRangeUpToTier(data.tier());
        String diameterStr = currentRange == 0 ? "0x0x0" : (currentRange * 2 + 1) + "x" + (currentRange * 2 + 1) + "x" + (currentRange * 2 + 1);
        int diameterNum = currentRange == 0 ? 0 : (currentRange * 2 + 1);
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.tier());
        String displayTier = data.tier() == 0 ? "0" : String.valueOf(data.tier());

        graphics.text(this.font, "§lSTATYSTYKI BAZY", infoX, infoY, 0xFF000000, false);

        Component tierComp = Component.literal("Poziom Bazy: ").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(displayTier).withStyle(net.minecraft.ChatFormatting.AQUA));
        graphics.text(this.font, tierComp, infoX, infoY + 20, 0xFF000000, false);

        Component areaComp = Component.literal("Obszar: ").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(currentRange + "  (" + diameterStr + ")").withStyle(net.minecraft.ChatFormatting.AQUA));
        graphics.text(this.font, areaComp, infoX, infoY + 35, 0xFF000000, false);

        Component slotsComp = Component.literal("Dostępne sloty: ").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(String.valueOf(maxSlots)).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        graphics.text(this.font, slotsComp, infoX, infoY + 50, 0xFF000000, false);

        Component effectsComp = Component.literal("Odblokowane efekty: ").withStyle(net.minecraft.ChatFormatting.BLACK)
                .append(Component.literal(String.valueOf(data.unlockedEffects().size())).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        graphics.text(this.font, effectsComp, infoX, infoY + 65, 0xFF000000, false);

        int boxX = this.leftPos + 240;
        int boxY = this.topPos + 45;
        int size = 50;
        int lineColor = 0xFF1E90FF;

        int frontX = boxX;
        int frontY = boxY + 25;
        int backX = boxX + 25;
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

        centeredTextNoShadow(graphics, String.valueOf(currentRange), cx - 20, cy + 4, 0xFF00AA00);
        centeredTextNoShadow(graphics, String.valueOf(diameterNum), frontX + size / 2, frontY + size + 5, 0xFF444444);
        centeredTextNoShadow(graphics, String.valueOf(diameterNum), frontX - 12, frontY + size / 2 - 4, 0xFF444444);


        int slotSize = 36;
        int slotSpacing = 16;
        int totalSlotsWidth = (4 * slotSize) + (3 * slotSpacing);
        int slotsStartX = this.leftPos + (this.imageWidth - totalSlotsWidth) / 2;
        int slotsStartY = this.topPos + this.imageHeight - slotSize - 20;

        centeredTextNoShadow(graphics, "Aktywne Sloty", this.leftPos + (this.imageWidth / 2), slotsStartY - 15, 0xFF000000);

        for (int i = 0; i < 4; i++) {
            int slotX = slotsStartX + (i * (slotSize + slotSpacing));
            boolean isLocked = i >= maxSlots;

            graphics.fill(slotX, slotsStartY, slotX + slotSize, slotsStartY + slotSize, isLocked ? 0xFFD7CCC8 : 0xFFF5DEB3);
            drawThickOutline(graphics, slotX, slotsStartY, slotSize, slotSize, 2, 0xFF8D6E63);

            if (isLocked) {
                centeredTextNoShadow(graphics, "X", slotX + (slotSize / 2), slotsStartY + (slotSize / 2) - 4, 0xFFFF5555);
            } else {
                String effectId = "empty";
                if (i < data.activeSlots().size()) {
                    effectId = data.activeSlots().get(i);
                }

                if (effectId.equals("empty")) {
                    String emptyText = "+";
                    int textW = this.font.width(emptyText);
                    graphics.text(this.font, emptyText, slotX + (slotSize - textW) / 2, slotsStartY + (slotSize - 8) / 2, 0xFF8D6E63, false);
                } else {
                    BaseCoreServerConfig.EffectConfig ec = BaseCoreServerConfig.getEffect(effectId);
                    if (ec != null) {
                        Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(ec.itemCost)).map(Holder::value).orElse(Items.AIR);
                        graphics.fakeItem(new ItemStack(costItem), slotX + 10, slotsStartY + 10);
                    }
                }
            }

            if (!isLocked && mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotsStartY && mouseY < slotsStartY + slotSize) {
                graphics.fill(slotX + 1, slotsStartY + 1, slotX + slotSize - 1, slotsStartY + slotSize - 1, 0x20000000);
            }
        }
    }

    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean blink) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 25;

        int totalInnerWidth = this.imageWidth - (innerMargin * 2);
        int colWidth = totalInnerWidth / 5;
        int startY = contentY + 10;
        int iconSize = 24;

        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(data.tier());

        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
        int pulseG = (int) (170 + (85 * pulse));
        int blinkColor = 0xFF000000 | (255 << 16) | (pulseG << 8);
        int blinkOutlineColor = 0xFF000000 | (204 << 16) | ((int)(122 + (82 * pulse)) << 8);

        for (int p = 1; p <= 5; p++) {
            boolean poolUnlocked = p <= maxPool;
            int poolX = contentX + (p - 1) * colWidth;

            if (p < 5) {
                graphics.fill(poolX + colWidth, startY + 5, poolX + colWidth + 1, startY + 160, 0xFF8D6E63);
            }

            centeredTextNoShadow(graphics, "Pula " + p, poolX + colWidth / 2, startY + 5, poolUnlocked ? 0xFF3E2723 : 0xFF8D6E63);

            int eIndex = 0;
            for (BaseCoreServerConfig.EffectConfig effect : allEffects) {
                if (effect.pool != p) continue;

                int ex = poolX + (colWidth - iconSize) / 2;
                int ey = startY + 25 + (eIndex * (iconSize + 10));

                boolean isUnlocked = data.unlockedEffects().contains(effect.id);
                boolean isHovered = mouseX >= ex && mouseX < ex + iconSize && mouseY >= ey && mouseY < ey + iconSize;

                Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effect.itemCost)).map(Holder::value).orElse(Items.AIR);
                boolean canAfford = false;
                if (!isUnlocked) {
                    canAfford = getTotalExperienceClient() >= effect.xpCost && countItemInClientInventory(costItem) >= effect.itemAmount;
                }

                int bgColor = isUnlocked ? 0xFFC8E6C9 : (poolUnlocked ? 0xFFF5DEB3 : 0xFFD7CCC8);
                int borderColor = isUnlocked ? 0xFF2E7D32 : (poolUnlocked ? 0xFF8D6E63 : 0xFFBCAAA4);

                if (!isUnlocked && poolUnlocked) {
                    if (canAfford) {
                        bgColor = blinkColor;
                        borderColor = blinkOutlineColor;
                    } else if (isHovered) {
                        bgColor = 0xFFE7CDB3;
                    }
                }

                graphics.fill(ex, ey, ex + iconSize, ey + iconSize, bgColor);
                drawThickOutline(graphics, ex, ey, iconSize, iconSize, 1, borderColor);

                graphics.fakeItem(new ItemStack(costItem), ex + 4, ey + 4);

                if (!poolUnlocked) {
                    graphics.fill(ex + 1, ey + 1, ex + iconSize - 1, ey + iconSize - 1, 0x66FFFFFF);
                } else if (!isUnlocked) {
                    graphics.fill(ex + iconSize - 6, ey + iconSize - 6, ex + iconSize, ey + iconSize, 0x44000000);
                    centeredTextNoShadow(graphics, "X", ex + iconSize - 2, ey + iconSize - 6, 0xFFFF5555);
                }

                if (isHovered) {
                    renderEffectTooltip(graphics, effect, isUnlocked, poolUnlocked, canAfford, mouseX, mouseY, costItem);
                }
                eIndex++;
            }
        }
    }

    private void renderEffectTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.EffectConfig effect, boolean isUnlocked, boolean isPoolUnlocked, boolean canAfford, int mouseX, int mouseY, Item costItem) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();

        tooltipLines.add(Component.literal(effect.name).withStyle(isUnlocked ? net.minecraft.ChatFormatting.DARK_GREEN : net.minecraft.ChatFormatting.GOLD));

        String[] words = effect.description.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (this.font.width(currentLine.toString() + word) > 170) {
                tooltipLines.add(Component.literal(currentLine.toString().trim()).withStyle(net.minecraft.ChatFormatting.GRAY));
                currentLine = new StringBuilder(word + " ");
            } else {
                currentLine.append(word).append(" ");
            }
        }
        if (currentLine.length() > 0) {
            tooltipLines.add(Component.literal(currentLine.toString().trim()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        if (!isPoolUnlocked) {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zablokowane (Wymaga Puli " + effect.pool + ")").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            tooltipLines.add(Component.literal("Ulepsz Serce Bazy, aby uzyskać dostęp.").withStyle(net.minecraft.ChatFormatting.RED));
        } else if (!isUnlocked) {
            int playerXp = getTotalExperienceClient();
            int playerItemCount = countItemInClientInventory(costItem);

            int cappedXp = Math.min(playerXp, effect.xpCost);
            int cappedItem = Math.min(playerItemCount, effect.itemAmount);
            String itemName = costItem.getName(costItem.getDefaultInstance()).getString();

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Wymagane zasoby:").withStyle(net.minecraft.ChatFormatting.WHITE));

            tooltipLines.add(Component.literal("- " + cappedXp + "/" + effect.xpCost + " XP")
                    .withStyle(cappedXp >= effect.xpCost ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            tooltipLines.add(Component.literal("- " + cappedItem + "/" + effect.itemAmount + " " + itemName)
                    .withStyle(cappedItem >= effect.itemAmount ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            if (canAfford) {
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
                int pulseG = (int) (170 + (85 * pulse));
                int blinkColorText = 0xFF000000 | (255 << 16) | (pulseG << 8);

                tooltipLines.add(Component.literal(""));
                tooltipLines.add(Component.literal("Kliknij, aby odblokować").withStyle(style -> style.withColor(blinkColorText)));
            }
        } else {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zakupiono! Zarządzaj w zakładce 'Przegląd'").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean blink) {
        int currentTier = data.tier();
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(currentTier);
        BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);
        boolean isMaxTier = (nextTierConfig == null);

        int panelWidth = 260;
        int panelHeight = 100;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
        int panelY = this.topPos + 40;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFF5DEB3);
        drawThickOutline(graphics, panelX, panelY, panelWidth, panelHeight, 2, 0xFF8D6E63);

        if (isMaxTier) {
            centeredTextNoShadow(graphics, "Rdzeń Osiągnął Limit Architektury", panelX + (panelWidth / 2), panelY + 40, 0xFF000000);
            centeredTextNoShadow(graphics, "Maksymalny Poziom: " + toRoman(currentTier), panelX + (panelWidth / 2), panelY + 55, 0xFF3E2723);

            if (mouseX >= panelX && mouseX < panelX + panelWidth && mouseY >= panelY && mouseY < panelY + panelHeight) {
                renderTierTooltip(graphics, currentTierConfig, false, mouseX, mouseY);
            }
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
        if (currentTier == 0) {
            graphics.fakeItem(new ItemStack(Items.STICK), -8, -8);
            graphics.pose().popMatrix();
            centeredTextNoShadow(graphics, "Baza (Poziom 0)", leftBoxX + 20, boxY + 50, 0xFF000000);
        } else {
            Item currentMain = BuiltInRegistries.ITEM.get(Identifier.parse(currentTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
            graphics.fakeItem(new ItemStack(currentMain), -8, -8);
            graphics.pose().popMatrix();
            String currentName = currentTierConfig.title + " (Poziom " + currentTier + ")";
            centeredTextNoShadow(graphics, currentName, leftBoxX + 20, boxY + 50, 0xFF000000);
        }

        graphics.fill(centerX - 15, panelY + 33, centerX + 10, panelY + 37, 0xFF8D6E63);
        graphics.fill(centerX + 5, panelY + 28, centerX + 10, panelY + 42, 0xFF8D6E63);
        graphics.fill(centerX + 10, panelY + 30, centerX + 13, panelY + 40, 0xFF8D6E63);
        graphics.fill(centerX + 13, panelY + 32, centerX + 16, panelY + 38, 0xFF8D6E63);

        int rightBoxX = centerX + 40;
        graphics.fill(rightBoxX, boxY, rightBoxX + 40, boxY + 40, 0xFFFFF8DC);
        drawThickOutline(graphics, rightBoxX, boxY, 40, 40, 2, 0xFF8D6E63);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rightBoxX + 20, boxY + 20);
        graphics.pose().scale(2.0f, 2.0f);
        Item nextMainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        graphics.fakeItem(new ItemStack(nextMainItem), -8, -8);
        graphics.pose().popMatrix();

        String nextName = nextTierConfig.title + " (Poziom " + (currentTier + 1) + ")";
        centeredTextNoShadow(graphics, nextName, rightBoxX + 20, boxY + 50, 0xFF3E2723);

        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);
        int playerMainCount = countItemInClientInventory(nextMainItem);
        int playerBulkCount = countItemInClientInventory(bulkItem);
        boolean canAfford = playerMainCount >= nextTierConfig.mainAmount && playerBulkCount >= nextTierConfig.bulkAmount;

        int costY = panelY + panelHeight + 15;
        centeredTextNoShadow(graphics, "Wymagane zasoby:", centerX, costY, 0xFF000000);

        String mainNameStr = nextMainItem.getName(nextMainItem.getDefaultInstance()).getString();
        int cappedMain = Math.min(playerMainCount, nextTierConfig.mainAmount);
        centeredTextNoShadow(graphics, cappedMain + "/" + nextTierConfig.mainAmount + " " + mainNameStr, centerX, costY + 12, cappedMain >= nextTierConfig.mainAmount ? 0xFF55FF55 : 0xFFFF5555);

        String bulkNameStr = bulkItem.getName(bulkItem.getDefaultInstance()).getString();
        int cappedBulk = Math.min(playerBulkCount, nextTierConfig.bulkAmount);
        centeredTextNoShadow(graphics, cappedBulk + "/" + nextTierConfig.bulkAmount + " " + bulkNameStr, centerX, costY + 24, cappedBulk >= nextTierConfig.bulkAmount ? 0xFF55FF55 : 0xFFFF5555);

        int btnWidth = 140;
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = costY + 40;

        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        int btnColor;
        int btnOutline;
        int textColor;

        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
        int pulseG = (int) (170 + (85 * pulse));
        int blinkColorBg = 0xFF000000 | (255 << 16) | (pulseG << 8);
        int blinkOutlineColor = 0xFF000000 | (204 << 16) | ((int)(122 + (82 * pulse)) << 8);

        if (canAfford) {
            btnColor = blinkColorBg;
            btnOutline = blinkOutlineColor;
            textColor = 0xFF000000;
        } else {
            btnColor = 0xFF8D6E63;
            btnOutline = 0xFF5D4037;
            textColor = 0xFFEFEBE9;
        }

        if (isBtnHovered && canAfford) {
            btnColor = 0xFF000000 | (255 << 16) | (Math.min(255, pulseG + 30) << 8);
        } else if (isBtnHovered && !canAfford) {
            btnColor = 0xFFA1887F;
        }

        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
        drawThickOutline(graphics, btnX, btnY, btnWidth, btnHeight, 1, btnOutline);
        centeredTextNoShadow(graphics, "ROZPOCZNIJ ULEPSZENIE", centerX, btnY + 6, textColor);

        if (mouseX >= leftBoxX && mouseX < leftBoxX + 40 && mouseY >= boxY && mouseY < boxY + 40) {
            if (currentTier == 0) {
                java.util.List<Component> t0 = new java.util.ArrayList<>();
                t0.add(Component.literal("Baza (Poziom 0)").withStyle(net.minecraft.ChatFormatting.GOLD));
                t0.add(Component.literal("Zbuduj ulepszenie poziomu 1, aby aktywować Rdzeń.").withStyle(net.minecraft.ChatFormatting.GRAY));
                graphics.setComponentTooltipForNextFrame(this.font, t0, mouseX, mouseY);
            } else {
                renderTierTooltip(graphics, currentTierConfig, false, mouseX, mouseY);
            }
        }

        if (mouseX >= rightBoxX && mouseX < rightBoxX + 40 && mouseY >= boxY && mouseY < boxY + 40) {
            renderTierTooltip(graphics, nextTierConfig, true, mouseX, mouseY);
        }
    }

    private void renderTierTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.TierUpgrade tierConfig, boolean isNextTier, int mouseX, int mouseY) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();

        tooltipLines.add(Component.literal(tierConfig.title + " (Poziom " + tierConfig.tierLevel + ")").withStyle(net.minecraft.ChatFormatting.GOLD));
        tooltipLines.add(Component.literal("Odblokowuje:").withStyle(net.minecraft.ChatFormatting.GRAY));

        boolean hasUnlocks = false;
        if (tierConfig.bonusRadius > 0) {
            tooltipLines.add(Component.literal("- +" + tierConfig.bonusRadius + " do Zasięgu Bazy").withStyle(net.minecraft.ChatFormatting.AQUA));
            hasUnlocks = true;
        }
        if (tierConfig.bonusSlots > 0) {
            tooltipLines.add(Component.literal("- +" + tierConfig.bonusSlots + " Slot na Efekt").withStyle(net.minecraft.ChatFormatting.GREEN));
            hasUnlocks = true;
        }
        if (tierConfig.unlocksPool > 0) {
            tooltipLines.add(Component.literal("- Pula Efektów " + tierConfig.unlocksPool).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            hasUnlocks = true;
        }

        if (!hasUnlocks) {
            tooltipLines.add(Component.literal("- Brak nowych odblokowań").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }

        if (isNextTier) {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Wymagane zasoby do ulepszenia:").withStyle(net.minecraft.ChatFormatting.WHITE));

            Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
            Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(tierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

            int playerMainCount = countItemInClientInventory(mainItem);
            int playerBulkCount = countItemInClientInventory(bulkItem);

            int cappedMain = Math.min(playerMainCount, tierConfig.mainAmount);
            tooltipLines.add(Component.literal("- " + cappedMain + "/" + tierConfig.mainAmount + " " + mainItem.getName(mainItem.getDefaultInstance()).getString())
                    .withStyle(cappedMain >= tierConfig.mainAmount ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            int cappedBulk = Math.min(playerBulkCount, tierConfig.bulkAmount);
            tooltipLines.add(Component.literal("- " + cappedBulk + "/" + tierConfig.bulkAmount + " " + bulkItem.getName(bulkItem.getDefaultInstance()).getString())
                    .withStyle(cappedBulk >= tierConfig.bulkAmount ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private void renderSlotDropdown(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;

        List<BaseCoreServerConfig.EffectConfig> unlockedOnly = new java.util.ArrayList<>();
        for (BaseCoreServerConfig.EffectConfig ec : allEffects) {
            if (data.unlockedEffects().contains(ec.id)) {
                unlockedOnly.add(ec);
            }
        }

        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x88000000);

        int ddW = 160;
        int rowHeight = 16;
        int ddH = (unlockedOnly.size() + 1) * rowHeight + 10;
        int ddX = this.leftPos + (this.imageWidth - ddW) / 2;
        int ddY = this.topPos + (this.imageHeight - ddH) / 2;

        graphics.fill(ddX, ddY, ddX + ddW, ddY + ddH, 0xFFFFF8DC);
        drawThickOutline(graphics, ddX, ddY, ddW, ddH, 2, 0xFF3E2723);

        boolean hoverEmpty = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY + 5 && mouseY < ddY + 5 + rowHeight;
        if (hoverEmpty) graphics.fill(ddX + 1, ddY + 5, ddX + ddW - 1, ddY + 5 + rowHeight, 0x20000000);
        graphics.text(this.font, "[ X ] Wyczyść ten Slot", ddX + 5, ddY + 5 + 4, 0xFFFF5555, false);

        for (int i = 0; i < unlockedOnly.size(); i++) {
            BaseCoreServerConfig.EffectConfig ec = unlockedOnly.get(i);
            int itemY = ddY + 5 + ((i + 1) * rowHeight);
            boolean hover = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= itemY && mouseY < itemY + rowHeight;

            if (hover) {
                graphics.fill(ddX + 1, itemY, ddX + ddW - 1, itemY + rowHeight, 0x20000000);
            }

            boolean isActive = data.activeSlots().contains(ec.id);
            int color = isActive ? 0xFF888888 : 0xFF000000;

            Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(ec.itemCost)).map(Holder::value).orElse(Items.AIR);

            graphics.fakeItem(new ItemStack(costItem), ddX + 5, itemY);
            graphics.text(this.font, ec.name, ddX + 25, itemY + 4, color, false);

            if (isActive) {
                graphics.text(this.font, "(Zajęty)", ddX + ddW - 45, itemY + 4, 0xFF888888, false);
            }
        }
    }

    private void handleDropdownClick(double mouseX, double mouseY) {
        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        List<BaseCoreServerConfig.EffectConfig> unlockedOnly = new java.util.ArrayList<>();
        for (BaseCoreServerConfig.EffectConfig ec : allEffects) {
            if (data.unlockedEffects().contains(ec.id)) unlockedOnly.add(ec);
        }

        int ddW = 160;
        int rowHeight = 16;
        int ddH = (unlockedOnly.size() + 1) * rowHeight + 10;
        int ddX = this.leftPos + (this.imageWidth - ddW) / 2;
        int ddY = this.topPos + (this.imageHeight - ddH) / 2;

        if (mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY && mouseY < ddY + ddH) {
            int clickedRow = (int)((mouseY - (ddY + 5)) / rowHeight);

            if (clickedRow == 0) {
                Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), "empty", this.openedSlotDropdown));
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            } else if (clickedRow > 0 && clickedRow <= unlockedOnly.size()) {
                BaseCoreServerConfig.EffectConfig ec = unlockedOnly.get(clickedRow - 1);
                if (!data.activeSlots().contains(ec.id)) {
                    Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), ec.id, this.openedSlotDropdown));
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
            }
        }
        this.openedSlotDropdown = -1;
    }

    private void handleEffectClick(double mouseX, double mouseY) {
        int innerMargin = 8;
        int totalInnerWidth = this.imageWidth - (innerMargin * 2);
        int colWidth = totalInnerWidth / 5;
        int startX = this.leftPos + innerMargin;
        int startY = this.topPos + 25 + 10;
        int iconSize = 24;

        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(data.tier());

        for (int p = 1; p <= 5; p++) {
            boolean poolUnlocked = p <= maxPool;
            if (!poolUnlocked) continue;

            int poolX = startX + (p - 1) * colWidth;
            int eIndex = 0;

            for (BaseCoreServerConfig.EffectConfig effect : allEffects) {
                if (effect.pool != p) continue;

                int ex = poolX + (colWidth - iconSize) / 2;
                int ey = startY + 25 + (eIndex * (iconSize + 10));

                if (mouseX >= ex && mouseX < ex + iconSize && mouseY >= ey && mouseY < ey + iconSize) {
                    if (!data.unlockedEffects().contains(effect.id)) {
                        Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effect.itemCost)).map(Holder::value).orElse(Items.AIR);
                        if (getTotalExperienceClient() >= effect.xpCost && countItemInClientInventory(costItem) >= effect.itemAmount) {
                            Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), effect.id, -1));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                        }
                    }
                    return;
                }
                eIndex++;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

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
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void drawDashedLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dashLen = 4;
        int gapLen = 4;
        double dist = Math.hypot(x2 - x1, y2 - y1);
        int segments = (int) (dist / (dashLen + gapLen));
        if (segments == 0) return;

        double dx = (x2 - x1) / dist;
        double dy = (y2 - y1) / dist;

        for (int i = 0; i <= segments; i++) {
            int startX = x1 + (int) (dx * i * (dashLen + gapLen));
            int startY = y1 + (int) (dy * i * (dashLen + gapLen));
            int endX = startX + (int) (dx * dashLen);
            int endY = startY + (int) (dy * dashLen);

            if (i == segments && Math.hypot(endX - x1, endY - y1) > dist) {
                endX = x2;
                endY = y2;
            }
            drawLine(graphics, startX, startY, endX, endY, color);
        }
    }

    private void drawThickOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int thickness, int color) {
        graphics.fill(x - thickness, y - thickness, x + w + thickness, y, color);
        graphics.fill(x - thickness, y + h, x + w + thickness, y + h + thickness, color);
        graphics.fill(x - thickness, y, x, y + h, color);
        graphics.fill(x + w, y, x + w + thickness, y + h, color);
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

    private int getTotalExperienceClient() {
        net.minecraft.client.player.LocalPlayer player = this.minecraft.player;
        if (player == null) return 0;
        int level = player.experienceLevel;
        int totalExp = 0;
        if (level >= 0 && level <= 15) {
            totalExp = level * level + 6 * level;
        } else if (level > 15 && level <= 30) {
            totalExp = (int) (2.5 * level * level - 40.5 * level + 360.0);
        } else if (level > 30) {
            totalExp = (int) (4.5 * level * level - 162.5 * level + 2220.0);
        }
        return totalExp + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    private int countItemInClientInventory(Item itemType) {
        net.minecraft.client.player.LocalPlayer player = this.minecraft.player;
        if (player == null) return 0;
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == itemType) {
                count += stack.getCount();
            }
        }
        return count;
    }
}