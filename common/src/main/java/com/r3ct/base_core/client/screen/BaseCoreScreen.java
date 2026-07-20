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

        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF3C3C3C);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF5A5A5A);

        int innerMargin = 8;
        graphics.fill(this.leftPos + innerMargin, this.topPos + 25,
                this.leftPos + this.imageWidth - innerMargin, this.topPos + this.imageHeight - innerMargin,
                0xFF2A2A2A);
        graphics.outline(this.leftPos + innerMargin, this.topPos + 25,
                this.imageWidth - (innerMargin * 2), this.imageHeight - 25 - innerMargin, 0xFF4A4A4A);

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
        int bgColor = isSelected ? 0xFF3C3C3C : (isHovered ? 0xFF444444 : 0xFF282828);
        int borderColor = isSelected ? 0xFF5A5A5A : 0xFF444444;
        int textColor = isSelected ? 0xFFFFD700 : (isHovered ? 0xFFFFFFFF : 0xFFAAAAAA);

        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x, y, x + width, y + 1, borderColor);
        graphics.fill(x, y, x + 1, y + height, borderColor);
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        if (!isSelected) {
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        } else {
            graphics.fill(x + 1, y + height - 2, x + width - 1, y + height + 1, 0xFF3C3C3C);
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
                    int btnWidth = 120;
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

    private void renderOverviewTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int infoX = this.leftPos + 20;
        int infoY = this.topPos + 45;
        int currentRange = calculateRangeUpToTier(data.tier());
        int diameter = currentRange * 2 + 1;

        graphics.text(this.font, "STATYSTYKI BAZY", infoX, infoY, 0xFFD700, true);
        graphics.text(this.font, "Poziom Bazy: " + data.tier() + " (" + toRoman(data.tier()) + ")", infoX, infoY + 20, 0xFFDDDDDD, false);
        graphics.text(this.font, "Obszar: " + currentRange + "  (" + diameter + "x" + diameter + "x" + diameter + ")", infoX, infoY + 35, 0xFFDDDDDD, false);
        graphics.text(this.font, "Dostępne moduły: " + BaseCoreServerConfig.calculateTotalSlots(data.tier()), infoX, infoY + 50, 0xFFDDDDDD, false);
        graphics.text(this.font, "Zakupione protokoły: " + data.unlockedEffects().size(), infoX, infoY + 65, 0xFFDDDDDD, false);

        int boxSize = 64;
        int boxX = this.leftPos + this.imageWidth - boxSize - 35;
        int boxY = this.topPos + 45;

        graphics.outline(boxX, boxY, boxSize, boxSize, 0xFF777777);
        graphics.fill(boxX + (boxSize / 2) - 2, boxY + (boxSize / 2) - 2, boxX + (boxSize / 2) + 2, boxY + (boxSize / 2) + 2, 0xFF00FF00);

        graphics.text(this.font, String.valueOf(diameter), boxX + boxSize + 6, boxY + (boxSize / 2) - 4, 0xFFAAAAAA, false);
        graphics.centeredText(this.font, String.valueOf(diameter), boxX + (boxSize / 2), boxY + boxSize + 6, 0xFFAAAAAA);

        graphics.fill(boxX + (boxSize / 2) + 2, boxY + (boxSize / 2), boxX + boxSize, boxY + (boxSize / 2) + 1, 0xFF999999);
        graphics.centeredText(this.font, String.valueOf(currentRange), boxX + (boxSize / 2) + (boxSize / 4), boxY + (boxSize / 2) + 3, 0xFFFFFFFF);

        int slotSize = 36;
        int slotSpacing = 16;
        int totalSlotsWidth = (4 * slotSize) + (3 * slotSpacing);
        int slotsStartX = this.leftPos + (this.imageWidth - totalSlotsWidth) / 2;
        int slotsStartY = this.topPos + this.imageHeight - slotSize - 20;
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(data.tier());

        graphics.centeredText(this.font, "Aktywne Moduły", this.leftPos + (this.imageWidth / 2), slotsStartY - 15, 0xFFDDDDDD);

        for (int i = 0; i < 4; i++) {
            int slotX = slotsStartX + (i * (slotSize + slotSpacing));
            boolean isLocked = i >= maxSlots;

            graphics.fill(slotX, slotsStartY, slotX + slotSize, slotsStartY + slotSize, isLocked ? 0xFF151515 : 0xFF222222);
            graphics.outline(slotX, slotsStartY, slotSize, slotSize, 0xFF5A5A5A);

            if (isLocked) {
                graphics.centeredText(this.font, "x", slotX + (slotSize / 2), slotsStartY + (slotSize / 2) - 4, 0xFF660000);
            } else {
                String effectId = "empty";
                if (i < data.activeSlots().size()) {
                    effectId = data.activeSlots().get(i);
                }

                if (effectId.equals("empty")) {
                    String emptyText = "+";
                    int textW = this.font.width(emptyText);
                    graphics.text(this.font, emptyText, slotX + (slotSize - textW) / 2, slotsStartY + (slotSize - 8) / 2, 0xFF555555, false);
                } else {
                    BaseCoreServerConfig.EffectConfig ec = BaseCoreServerConfig.getEffect(effectId);
                    if (ec != null) {
                        Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(ec.itemCost)).map(Holder::value).orElse(Items.AIR);
                        graphics.fakeItem(new ItemStack(costItem), slotX + 10, slotsStartY + 10);
                    }
                }
            }

            if (!isLocked && mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotsStartY && mouseY < slotsStartY + slotSize) {
                graphics.fill(slotX + 1, slotsStartY + 1, slotX + slotSize - 1, slotsStartY + slotSize - 1, 0x33FFFFFF);
            }
        }
    }

    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 25;

        int totalInnerWidth = this.imageWidth - (innerMargin * 2);
        int colWidth = totalInnerWidth / 5;
        int startY = contentY + 10;
        int iconSize = 24;

        List<BaseCoreServerConfig.EffectConfig> allEffects = BaseCoreServerConfig.getInstance().effects;
        int maxPool = BaseCoreServerConfig.getMaxUnlockedPool(data.tier());

        for (int p = 1; p <= 5; p++) {
            boolean poolUnlocked = p <= maxPool;
            int poolX = contentX + (p - 1) * colWidth;

            if (p < 5) {
                graphics.fill(poolX + colWidth, startY + 5, poolX + colWidth + 1, startY + 160, 0xFF444444);
            }

            graphics.centeredText(this.font, "Pula " + p, poolX + colWidth / 2, startY + 5, poolUnlocked ? 0xFFD700 : 0xFF666666);

            int eIndex = 0;
            for (BaseCoreServerConfig.EffectConfig effect : allEffects) {
                if (effect.pool != p) continue;

                int ex = poolX + (colWidth - iconSize) / 2;
                int ey = startY + 25 + (eIndex * (iconSize + 10));

                boolean isUnlocked = data.unlockedEffects().contains(effect.id);
                boolean isHovered = mouseX >= ex && mouseX < ex + iconSize && mouseY >= ey && mouseY < ey + iconSize;

                int bgColor = isUnlocked ? 0xFF353525 : (poolUnlocked ? 0xFF252525 : 0xFF1A1A1A);
                int borderColor = isUnlocked ? 0xFFD700 : (poolUnlocked ? 0xFF5A5A5A : 0xFF333333);

                if (isHovered && !isUnlocked && poolUnlocked) bgColor = 0xFF3A3A3A;

                graphics.fill(ex, ey, ex + iconSize, ey + iconSize, bgColor);
                graphics.outline(ex, ey, iconSize, iconSize, borderColor);

                Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(effect.itemCost)).map(Holder::value).orElse(Items.AIR);

                if (poolUnlocked) {
                    graphics.fakeItem(new ItemStack(costItem), ex + 4, ey + 4);
                } else {
                    graphics.centeredText(this.font, "?", ex + (iconSize / 2), ey + (iconSize / 2) - 4, 0xFF555555);
                }

                if (!poolUnlocked) {
                    graphics.fill(ex, ey, ex + iconSize, ey + iconSize, 0x88000000);
                } else if (!isUnlocked) {
                    graphics.fill(ex + iconSize - 6, ey + iconSize - 6, ex + iconSize, ey + iconSize, 0xAA000000);
                    graphics.text(this.font, "X", ex + iconSize - 5, ey + iconSize - 6, 0xFFFF5555, false);
                }

                if (isHovered) {
                    renderEffectTooltip(graphics, effect, isUnlocked, poolUnlocked, mouseX, mouseY, costItem);
                }
                eIndex++;
            }
        }
    }

    private void renderEffectTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.EffectConfig effect, boolean isUnlocked, boolean isPoolUnlocked, int mouseX, int mouseY, Item costItem) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();

        tooltipLines.add(Component.literal(effect.name).withStyle(isUnlocked ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.YELLOW));
        tooltipLines.add(Component.literal(effect.description).withStyle(net.minecraft.ChatFormatting.GRAY));

        if (!isPoolUnlocked) {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zablokowane (Wymaga Puli " + effect.pool + ")").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            tooltipLines.add(Component.literal("Ulepsz Serce Bazy, aby uzyskać dostęp.").withStyle(net.minecraft.ChatFormatting.RED));
        } else if (!isUnlocked) {
            int playerXp = getTotalExperienceClient();
            int playerItemCount = countItemInClientInventory(costItem);

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Wymagania odblokowania:").withStyle(net.minecraft.ChatFormatting.WHITE));
            tooltipLines.add(Component.literal("- Koszt XP: " + effect.xpCost + " (Masz: " + playerXp + ")")
                    .withStyle(playerXp >= effect.xpCost ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            String itemName = costItem.getName(costItem.getDefaultInstance()).getString();
            tooltipLines.add(Component.literal("- Przedmiot: " + effect.itemAmount + "x " + itemName + " (Masz: " + playerItemCount + ")")
                    .withStyle(playerItemCount >= effect.itemAmount ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Kliknij, aby odblokować na zawsze").withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Zakupiono! Zarządzaj w zakładce 'Przegląd'").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int currentTier = data.tier();
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(currentTier);
        BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);
        boolean isMaxTier = (nextTierConfig == null);

        int panelWidth = 260;
        int panelHeight = 100;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
        int panelY = this.topPos + 40;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF222222);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF555555);

        if (isMaxTier) {
            graphics.centeredText(this.font, "Rdzeń Osiągnął Limit Architektury", panelX + (panelWidth / 2), panelY + 40, 0xFFFF55);
            graphics.centeredText(this.font, "Maksymalny Poziom: " + toRoman(currentTier), panelX + (panelWidth / 2), panelY + 55, 0xFFD700);
            return;
        }

        int centerX = panelX + (panelWidth / 2);

        graphics.fill(centerX - 80, panelY + 15, centerX - 40, panelY + 55, 0xFF333333);
        graphics.outline(centerX - 80, panelY + 15, 40, 40, 0xFF666666);

        if (currentTierConfig != null) {
            Item currentMain = BuiltInRegistries.ITEM.get(Identifier.parse(currentTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
            graphics.fakeItem(new ItemStack(currentMain), centerX - 80 + 12, panelY + 15 + 12);
            String currentName = currentTierConfig.title + " (Poziom " + currentTier + ")";
            graphics.centeredText(this.font, currentName, centerX - 60, panelY + 65, 0xFFAAAAAA);
        } else {
            graphics.centeredText(this.font, toRoman(currentTier), centerX - 60, panelY + 31, 0xFFD700);
            graphics.centeredText(this.font, "Obecny", centerX - 60, panelY + 65, 0xFFAAAAAA);
        }

        graphics.fill(centerX - 15, panelY + 33, centerX + 10, panelY + 37, 0xFF888888);
        graphics.fill(centerX + 5, panelY + 28, centerX + 10, panelY + 42, 0xFF888888);
        graphics.fill(centerX + 10, panelY + 30, centerX + 13, panelY + 40, 0xFF888888);
        graphics.fill(centerX + 13, panelY + 32, centerX + 16, panelY + 38, 0xFF888888);

        graphics.fill(centerX + 40, panelY + 15, centerX + 80, panelY + 55, 0xFF3C3C22);
        graphics.outline(centerX + 40, panelY + 15, 40, 40, 0xFFD700);

        Item nextMainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        graphics.fakeItem(new ItemStack(nextMainItem), centerX + 40 + 12, panelY + 15 + 12);

        String nextName = nextTierConfig.title + " (Poziom " + (currentTier + 1) + ")";
        graphics.centeredText(this.font, nextName, centerX + 60, panelY + 65, 0xFFD700);

        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);
        int playerMainCount = countItemInClientInventory(nextMainItem);
        int playerBulkCount = countItemInClientInventory(bulkItem);
        boolean canAfford = playerMainCount >= nextTierConfig.mainAmount && playerBulkCount >= nextTierConfig.bulkAmount;

        int costY = panelY + panelHeight + 15;
        graphics.centeredText(this.font, "Wymagane zasoby:", centerX, costY, 0xFFDDDDDD);

        String mainNameStr = nextMainItem.getName(nextMainItem.getDefaultInstance()).getString();
        String mainText = "Główny: " + nextTierConfig.mainAmount + "x " + mainNameStr + " (Masz: " + playerMainCount + ")";
        graphics.centeredText(this.font, mainText, centerX, costY + 12, playerMainCount >= nextTierConfig.mainAmount ? 0xFF55FF55 : 0xFFFF5555);

        String bulkNameStr = bulkItem.getName(bulkItem.getDefaultInstance()).getString();
        String bulkText = "Pospolity: " + nextTierConfig.bulkAmount + "x " + bulkNameStr + " (Masz: " + playerBulkCount + ")";
        graphics.centeredText(this.font, bulkText, centerX, costY + 24, playerBulkCount >= nextTierConfig.bulkAmount ? 0xFF55FF55 : 0xFFFF5555);

        int btnWidth = 140;
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

        graphics.fill(ddX, ddY, ddX + ddW, ddY + ddH, 0xFF2A2A2A);
        graphics.outline(ddX, ddY, ddW, ddH, 0xFF666666);

        boolean hoverEmpty = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY + 5 && mouseY < ddY + 5 + rowHeight;
        if (hoverEmpty) graphics.fill(ddX + 1, ddY + 5, ddX + ddW - 1, ddY + 5 + rowHeight, 0x44FFFFFF);
        graphics.text(this.font, "[ X ] Wyczyść ten Slot", ddX + 5, ddY + 5 + 4, 0xFFFF5555, false);

        for (int i = 0; i < unlockedOnly.size(); i++) {
            BaseCoreServerConfig.EffectConfig ec = unlockedOnly.get(i);
            int itemY = ddY + 5 + ((i + 1) * rowHeight);
            boolean hover = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= itemY && mouseY < itemY + rowHeight;

            if (hover) {
                graphics.fill(ddX + 1, itemY, ddX + ddW - 1, itemY + rowHeight, 0x44FFFFFF);
            }

            boolean isActive = data.activeSlots().contains(ec.id);
            int color = isActive ? 0xFF777777 : 0xFFDDDDDD;

            Item costItem = BuiltInRegistries.ITEM.get(Identifier.parse(ec.itemCost)).map(Holder::value).orElse(Items.AIR);

            graphics.fakeItem(new ItemStack(costItem), ddX + 5, itemY);
            graphics.text(this.font, ec.name, ddX + 25, itemY + 4, color, false);

            if (isActive) {
                graphics.text(this.font, "(Zajęty)", ddX + ddW - 45, itemY + 4, 0xFFAAAAAA, false);
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