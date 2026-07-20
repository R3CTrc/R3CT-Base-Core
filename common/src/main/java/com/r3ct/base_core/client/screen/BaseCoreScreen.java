package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class BaseCoreScreen extends Screen {

    private final OpenBaseCoreGuiPayload data;
    private Tab currentTab = Tab.OVERVIEW;

    private int openedSlotDropdown = -1;

    private final int imageWidth = 300;
    private final int imageHeight = 220;
    private int leftPos;
    private int topPos;

    private final int tabWidth = 90;
    private final int tabHeight = 22;
    private final int tabSpacing = 4;

    public enum Tab {
        OVERVIEW("Przegląd"),
        EFFECTS("Efekty"),
        UPGRADES("Ulepszenia");

        final String name;
        Tab(String name) { this.name = name; }
    }

    public BaseCoreScreen(OpenBaseCoreGuiPayload data) {
        super(Component.literal("Serce Bazy"));
        this.data = data;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
        int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
        int tabY = this.topPos - tabHeight + 2;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int currentTabX = startX + (i * (tabWidth + tabSpacing));
            boolean isSelected = (currentTab == tab);
            boolean isHovered = mouseX >= currentTabX && mouseX < currentTabX + tabWidth &&
                    mouseY >= tabY && mouseY < tabY + tabHeight;
            renderCustomTab(graphics, currentTabX, tabY, tabWidth, tabHeight, tab.name, isSelected, isHovered);
        }

        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF242424);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF4A4A4A);

        int innerMargin = 8;
        graphics.fill(this.leftPos + innerMargin, this.topPos + 25,
                this.leftPos + this.imageWidth - innerMargin, this.topPos + this.imageHeight - innerMargin,
                0xFF1A1A1A);
        graphics.outline(this.leftPos + innerMargin, this.topPos + 25,
                this.imageWidth - (innerMargin * 2), this.imageHeight - 25 - innerMargin, 0xFF333333);

        String tierText = "Poziom " + toRoman(data.tier());
        int titleWidth = this.font.width("Serce Bazy");
        int tierWidth = this.font.width(tierText);

        graphics.text(this.font, "Serce Bazy", this.leftPos + 12, this.topPos + 10, 0xFFFFFF, true);
        graphics.text(this.font, tierText, this.leftPos + this.imageWidth - tierWidth - 12, this.topPos + 10, 0xFFD700, true);
        graphics.fill(this.leftPos + 12, this.topPos + 21, this.leftPos + titleWidth + 12, this.topPos + 22, 0x88FFD700);

        switch (currentTab) {
            case OVERVIEW -> renderOverviewTab(graphics, mouseX, mouseY);
            case EFFECTS -> renderEffectsTab(graphics, mouseX, mouseY);
            case UPGRADES -> renderUpgradesTab(graphics, mouseX, mouseY);
        }

        if (openedSlotDropdown != -1) {
            renderSlotDropdown(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCustomTab(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String text, boolean isSelected, boolean isHovered) {
        int bgColor = isSelected ? 0xFF242424 : (isHovered ? 0xFF303030 : 0xFF1C1C1C);
        int borderColor = isSelected ? 0xFF4A4A4A : 0xFF333333;
        int textColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        if (!isSelected) {
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        } else {
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height + 1, 0xFF242424);
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
            double mouseX = event.x();
            double mouseY = event.y();

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
                    this.currentTab = Tab.values()[i];
                    return true;
                }
            }

            if (this.currentTab == Tab.OVERVIEW) {
                int contentX = this.leftPos + 8 + 10;
                int slotsStartY = this.topPos + 35 + 15;
                int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.tier());

                for (int i = 0; i < 4; i++) {
                    int slotX = contentX + (i * 44);
                    if (mouseX >= slotX && mouseX < slotX + 36 && mouseY >= slotsStartY && mouseY < slotsStartY + 36) {
                        if (i < maxSlots) {
                            this.openedSlotDropdown = i;
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
                    int panelY = this.topPos + 30 + 25;
                    int costY = panelY + 90 + 15;
                    int btnWidth = 100;
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
                        }
                        return true;
                    }
                }
            }
        }
        return false;
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
        int rowHeight = 12;
        int ddH = (unlockedOnly.size() + 1) * rowHeight + 10;
        int ddX = this.leftPos + (this.imageWidth - ddW) / 2;
        int ddY = this.topPos + (this.imageHeight - ddH) / 2;

        graphics.fill(ddX, ddY, ddX + ddW, ddY + ddH, 0xFF1C1C1C);
        graphics.outline(ddX, ddY, ddW, ddH, 0xFF555555);

        boolean hoverEmpty = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY + 5 && mouseY < ddY + 5 + rowHeight;
        if (hoverEmpty) graphics.fill(ddX + 1, ddY + 5, ddX + ddW - 1, ddY + 5 + rowHeight, 0x44FFFFFF);
        graphics.text(this.font, "[ X ] Wyczyść ten Slot", ddX + 5, ddY + 5 + 2, 0xFFFF5555, false);

        for (int i = 0; i < unlockedOnly.size(); i++) {
            BaseCoreServerConfig.EffectConfig ec = unlockedOnly.get(i);
            int itemY = ddY + 5 + ((i + 1) * rowHeight);
            boolean hover = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= itemY && mouseY < itemY + rowHeight;

            if (hover) {
                graphics.fill(ddX + 1, itemY, ddX + ddW - 1, itemY + rowHeight, 0x44FFFFFF);
            }

            boolean isActive = data.activeSlots().contains(ec.id);
            int color = isActive ? 0xFF555555 : 0xFFDDDDDD;
            graphics.text(this.font, "- " + ec.name, ddX + 5, itemY + 2, color, false);

            if (isActive) {
                graphics.text(this.font, "(Zajęty)", ddX + ddW - 45, itemY + 2, 0xFF888888, false);
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
        int rowHeight = 12;
        int ddH = (unlockedOnly.size() + 1) * rowHeight + 10;
        int ddX = this.leftPos + (this.imageWidth - ddW) / 2;
        int ddY = this.topPos + (this.imageHeight - ddH) / 2;

        if (mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY && mouseY < ddY + ddH) {
            int clickedRow = (int)((mouseY - (ddY + 5)) / rowHeight);

            if (clickedRow == 0) {
                Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), "empty", this.openedSlotDropdown));
            } else if (clickedRow > 0 && clickedRow <= unlockedOnly.size()) {
                BaseCoreServerConfig.EffectConfig ec = unlockedOnly.get(clickedRow - 1);
                if (!data.activeSlots().contains(ec.id)) {
                    Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), ec.id, this.openedSlotDropdown));
                }
            }
        }
        this.openedSlotDropdown = -1;
    }

    private void handleEffectClick(double mouseX, double mouseY) {
        int colWidth = 66;
        int startX = this.leftPos + 18;
        int startY = this.topPos + 50;
        int iconSize = 24;

        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(data.tier());

        for (int p = 1; p <= 4; p++) {
            boolean poolUnlocked = p <= maxPool;
            if (!poolUnlocked) continue;

            int poolX = startX + (p - 1) * colWidth;
            int eIndex = 0;

            for (BaseCoreServerConfig.EffectConfig effect : allEffects) {
                if (effect.pool != p) continue;

                int ex = poolX + (colWidth - iconSize) / 2;

                int ey = startY + 20 + (eIndex * (iconSize + 10));

                if (mouseX >= ex && mouseX < ex + iconSize && mouseY >= ey && mouseY < ey + iconSize) {
                    if (!data.unlockedEffects().contains(effect.id)) {
                        Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effect.itemCost)).map(Holder::value).orElse(Items.AIR);
                        if (getTotalExperienceClient() >= effect.xpCost && countItemInClientInventory(costItem) >= effect.itemAmount) {
                            Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), effect.id, -1));
                        }
                    }
                    return;
                }
                eIndex++;
            }
        }
    }

    private void renderOverviewTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin + 10;
        int contentY = this.topPos + 35;

        graphics.text(this.font, "Zainstalowane Moduły", contentX, contentY, 0xFFDDDDDD, false);

        int slotSize = 36;
        int slotSpacing = 8;
        int slotsStartX = contentX;
        int slotsStartY = contentY + 15;
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.tier());

        for (int i = 0; i < 4; i++) {
            int slotX = slotsStartX + (i * (slotSize + slotSpacing));
            boolean isLocked = i >= maxSlots;

            graphics.fill(slotX, slotsStartY, slotX + slotSize, slotsStartY + slotSize, isLocked ? 0xFF0A0A0A : 0xFF101010);
            graphics.outline(slotX, slotsStartY, slotSize, slotSize, 0xFF555555);

            if (isLocked) {
                graphics.centeredText(this.font, "x", slotX + (slotSize / 2), slotsStartY + (slotSize / 2) - 4, 0xFF550000);
            } else {
                String effectId = "empty";
                if (i < data.activeSlots().size()) {
                    effectId = data.activeSlots().get(i);
                }

                if (effectId.equals("empty")) {
                    String emptyText = "+";
                    int textW = this.font.width(emptyText);
                    graphics.text(this.font, emptyText, slotX + (slotSize - textW) / 2, slotsStartY + (slotSize - 8) / 2, 0xFF444444, false);
                } else {
                    BaseCoreServerConfig.EffectConfig ec = BaseCoreServerConfig.getEffect(effectId);
                    String initial = ec != null ? ec.name.substring(0, 1) : "?";
                    graphics.centeredText(this.font, initial, slotX + (slotSize / 2), slotsStartY + (slotSize / 2) - 4, 0xFF00FF00);
                }
            }

            if (!isLocked && mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotsStartY && mouseY < slotsStartY + slotSize) {
                graphics.fill(slotX + 1, slotsStartY + 1, slotX + slotSize - 1, slotsStartY + slotSize - 1, 0x44FFFFFF);
            }
        }

        int rangeBoxX = this.leftPos + this.imageWidth - 100 - innerMargin;
        int rangeBoxY = contentY;
        int rangeBoxWidth = 90;
        int rangeBoxHeight = 60;

        graphics.fill(rangeBoxX, rangeBoxY, rangeBoxX + rangeBoxWidth, rangeBoxY + rangeBoxHeight, 0xFF202020);
        graphics.outline(rangeBoxX, rangeBoxY, rangeBoxWidth, rangeBoxHeight, 0xFF444444);
        graphics.centeredText(this.font, "Zasięg Rdzenia", rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 8, 0xFFAAAAAA);

        int currentRange = calculateRangeUpToTier(data.tier());
        String rangeValue = currentRange + "x" + currentRange;
        graphics.centeredText(this.font, rangeValue, rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 30, 0xFFFF55);
        graphics.centeredText(this.font, "bloków", rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 45, 0xFF777777);
    }

    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 30;

        graphics.text(this.font, "Sklep z Protokołami", contentX + 8, contentY, 0xFFDDDDDD, false);

        int colWidth = 66;
        int startX = this.leftPos + 18;
        int startY = contentY + 20;
        int iconSize = 24;

        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(data.tier());

        for (int p = 1; p <= 4; p++) {
            boolean poolUnlocked = p <= maxPool;
            int poolX = startX + (p - 1) * colWidth;

            graphics.fill(poolX, startY, poolX + colWidth, startY + 140, poolUnlocked ? 0xFF1E1E1E : 0xFF101010);

            if (p < 4) {
                graphics.fill(poolX + colWidth, startY, poolX + colWidth + 1, startY + 140, 0xFF333333);
            }

            graphics.centeredText(this.font, "Pula " + p, poolX + colWidth / 2, startY + 5, poolUnlocked ? 0xFFD700 : 0xFF555555);

            int eIndex = 0;
            for (BaseCoreServerConfig.EffectConfig effect : allEffects) {
                if (effect.pool != p) continue;

                int ex = poolX + (colWidth - iconSize) / 2;
                int ey = startY + 20 + (eIndex * (iconSize + 10));

                boolean isUnlocked = data.unlockedEffects().contains(effect.id);
                boolean isHovered = mouseX >= ex && mouseX < ex + iconSize && mouseY >= ey && mouseY < ey + iconSize;

                int bgColor = isUnlocked ? 0xFF2A2A1A : (poolUnlocked ? 0xFF151515 : 0xFF0A0A0A);
                int borderColor = isUnlocked ? 0xFFD700 : (poolUnlocked ? 0xFF444444 : 0xFF222222);

                if (isHovered && !isUnlocked && poolUnlocked) {
                    bgColor = 0xFF252525;
                }

                graphics.fill(ex, ey, ex + iconSize, ey + iconSize, bgColor);
                graphics.outline(ex, ey, iconSize, iconSize, borderColor);

                String shortName = effect.name.substring(0, 1);
                int textColor = isUnlocked ? 0xFFFFFF : (poolUnlocked ? 0xFF777777 : 0xFF333333);
                graphics.centeredText(this.font, shortName, ex + (iconSize / 2), ey + (iconSize / 2) - 4, textColor);

                if (!poolUnlocked) {
                    graphics.fill(ex, ey, ex + iconSize, ey + iconSize, 0x88000000);
                } else if (!isUnlocked) {
                    graphics.fill(ex + iconSize - 6, ey + iconSize - 6, ex + iconSize, ey + iconSize, 0xAA000000);
                    graphics.text(this.font, "X", ex + iconSize - 5, ey + iconSize - 6, 0xFFFF5555, false);
                }

                if (isHovered) {
                    renderEffectTooltip(graphics, effect, isUnlocked, poolUnlocked, mouseX, mouseY);
                }

                eIndex++;
            }
        }
    }

    private void renderEffectTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.EffectConfig effect, boolean isUnlocked, boolean isPoolUnlocked, int mouseX, int mouseY) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();

        tooltipLines.add(Component.literal(effect.name).withStyle(isUnlocked ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
        tooltipLines.add(Component.literal(effect.description).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        if (!isPoolUnlocked) {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zablokowane (Wymaga Puli " + effect.pool + ")").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            tooltipLines.add(Component.literal("Ulepsz Serce Bazy, aby uzyskać dostęp.").withStyle(net.minecraft.ChatFormatting.RED));
        } else if (!isUnlocked) {
            Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effect.itemCost)).map(Holder::value).orElse(Items.AIR);
            int playerXp = getTotalExperienceClient();
            int playerItemCount = countItemInClientInventory(costItem);

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Wymagania odblokowania:").withStyle(net.minecraft.ChatFormatting.RED));
            tooltipLines.add(Component.literal("- Koszt XP: " + effect.xpCost + " (Masz: " + playerXp + ")")
                    .withStyle(playerXp >= effect.xpCost ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            String itemName = costItem.getName(costItem.getDefaultInstance()).getString();
            tooltipLines.add(Component.literal("- Przedmiot: " + effect.itemAmount + "x " + itemName + " (Masz: " + playerItemCount + ")")
                    .withStyle(playerItemCount >= effect.itemAmount ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Kliknij, aby odblokować na zawsze").withStyle(net.minecraft.ChatFormatting.YELLOW));
        } else {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zakupiono! Zarządzaj w zakładce 'Przegląd'").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 30;

        graphics.text(this.font, "Architektura Rdzenia", contentX + 8, contentY, 0xFFDDDDDD, false);

        int currentTier = data.tier();
        BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);
        boolean isMaxTier = (nextTierConfig == null);

        int panelWidth = 200;
        int panelHeight = 90;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
        int panelY = contentY + 25;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1C1C1C);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF444444);

        if (isMaxTier) {
            graphics.centeredText(this.font, "Rdzeń Osiągnął Limit Architektury", panelX + (panelWidth / 2), panelY + 40, 0xFFFF55);
            graphics.centeredText(this.font, "Maksymalny Poziom: " + toRoman(currentTier), panelX + (panelWidth / 2), panelY + 55, 0xFFD700);
            return;
        }

        int centerX = panelX + (panelWidth / 2);

        graphics.fill(centerX - 70, panelY + 15, centerX - 30, panelY + 55, 0xFF2A2A2A);
        graphics.outline(centerX - 70, panelY + 15, 40, 40, 0xFF555555);
        graphics.centeredText(this.font, toRoman(currentTier), centerX - 50, panelY + 31, 0xFFD700);
        graphics.centeredText(this.font, "Obecny", centerX - 50, panelY + 60, 0xFFAAAAAA);

        graphics.fill(centerX - 15, panelY + 33, centerX + 10, panelY + 37, 0xFF777777);
        graphics.fill(centerX + 5, panelY + 28, centerX + 10, panelY + 42, 0xFF777777);
        graphics.fill(centerX + 10, panelY + 30, centerX + 13, panelY + 40, 0xFF777777);
        graphics.fill(centerX + 13, panelY + 32, centerX + 16, panelY + 38, 0xFF777777);

        graphics.fill(centerX + 30, panelY + 15, centerX + 70, panelY + 55, 0xFF333322);
        graphics.outline(centerX + 30, panelY + 15, 40, 40, 0xFFD700);
        graphics.centeredText(this.font, toRoman(currentTier + 1), centerX + 50, panelY + 31, 0xFFFFFF);
        graphics.centeredText(this.font, nextTierConfig.title, centerX + 50, panelY + 60, 0xFFD700);

        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

        int playerMainCount = countItemInClientInventory(mainItem);
        int playerBulkCount = countItemInClientInventory(bulkItem);
        boolean canAfford = playerMainCount >= nextTierConfig.mainAmount && playerBulkCount >= nextTierConfig.bulkAmount;

        int costY = panelY + panelHeight + 15;
        graphics.centeredText(this.font, "Wymagane zasoby:", centerX, costY, 0xFFDDDDDD);

        String mainName = mainItem.getName(mainItem.getDefaultInstance()).getString();
        String mainText = "Główny: " + nextTierConfig.mainAmount + "x " + mainName + " (Masz: " + playerMainCount + ")";
        graphics.centeredText(this.font, mainText, centerX, costY + 12, playerMainCount >= nextTierConfig.mainAmount ? 0xFF55FF55 : 0xFFFF5555);

        String bulkName = bulkItem.getName(bulkItem.getDefaultInstance()).getString();
        String bulkText = "Pospolity: " + nextTierConfig.bulkAmount + "x " + bulkName + " (Masz: " + playerBulkCount + ")";
        graphics.centeredText(this.font, bulkText, centerX, costY + 24, playerBulkCount >= nextTierConfig.bulkAmount ? 0xFF55FF55 : 0xFFFF5555);

        int btnWidth = 100;
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = costY + 40;

        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;
        int btnColor = isBtnHovered ? 0xFF2A8B2A : 0xFF1B5E1B;
        int btnOutline = isBtnHovered ? 0xFF3CDA3C : 0xFF288B28;
        int textColor = 0xFFFFFFFF;

        if (!canAfford) {
            btnColor = 0xFF333333;
            btnOutline = 0xFF555555;
            textColor = 0xFFAAAAAA;
        }

        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
        graphics.outline(btnX, btnY, btnWidth, btnHeight, btnOutline);
        graphics.centeredText(this.font, "ROZPOCZNIJ ULEPSZENIE", centerX, btnY + 6, textColor);
    }

    @Override
    public boolean isPauseScreen() { return false; }

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
        int totalRange = 0;
        for (int i = 1; i <= currentTier; i++) {
            BaseCoreServerConfig.TierUpgrade tier = BaseCoreServerConfig.getTier(i);
            if (tier != null) totalRange += tier.bonusRadius;
        }
        return totalRange == 0 ? 16 : totalRange;
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