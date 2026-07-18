package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.network.UnlockEffectPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class BaseCoreScreen extends Screen {

    private final OpenBaseCoreGuiPayload data;
    private Tab currentTab = Tab.OVERVIEW;

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
        // Usunięto this.renderBackground() dla jaśniejszego tła gry w tle

        // --- RYSOWANIE ZAKŁADEK ---
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

        // --- GŁÓWNE TŁO OKNA ---
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF242424);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF4A4A4A);

        int innerMargin = 8;
        graphics.fill(this.leftPos + innerMargin, this.topPos + 25,
                this.leftPos + this.imageWidth - innerMargin, this.topPos + this.imageHeight - innerMargin,
                0xFF1A1A1A);
        graphics.outline(this.leftPos + innerMargin, this.topPos + 25,
                this.imageWidth - (innerMargin * 2), this.imageHeight - 25 - innerMargin,
                0xFF333333);

        // --- NAGŁÓWEK ---
        String tierText = "Poziom " + toRoman(data.tier());
        int titleWidth = this.font.width("Serce Bazy");
        int tierWidth = this.font.width(tierText);

        graphics.text(this.font, "Serce Bazy", this.leftPos + 12, this.topPos + 10, 0xFFFFFF, true);
        graphics.text(this.font, tierText, this.leftPos + this.imageWidth - tierWidth - 12, this.topPos + 10, 0xFFD700, true);
        graphics.fill(this.leftPos + 12, this.topPos + 21, this.leftPos + titleWidth + 12, this.topPos + 22, 0x88FFD700);

        // --- RENDEROWANIE ZAWARTOŚCI ZAKŁADEK ---
        switch (currentTab) {
            case OVERVIEW -> renderOverviewTab(graphics, mouseX, mouseY);
            case EFFECTS -> renderEffectsTab(graphics, mouseX, mouseY);
            case UPGRADES -> renderUpgradesTab(graphics, mouseX, mouseY);
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

    // --- POPRAWIONA LOGIKA KLIKANIA (1.21.2+) ---
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();

            // 1. Sprawdzanie kliknięcia w zakładki
            int totalTabsWidth = (tabWidth * 3) + (tabSpacing * 2);
            int startX = this.leftPos + (this.imageWidth - totalTabsWidth) / 2;
            int tabY = this.topPos - tabHeight + 2;

            for (int i = 0; i < Tab.values().length; i++) {
                int currentTabX = startX + (i * (tabWidth + tabSpacing));
                if (mouseX >= currentTabX && mouseX < currentTabX + tabWidth &&
                        mouseY >= tabY && mouseY < tabY + tabHeight) {
                    this.currentTab = Tab.values()[i];
                    return true;
                }
            }

            // 2. Obsługa kliknięcia w efekt na Zakładce Efektów
            if (this.currentTab == Tab.EFFECTS) {
                handleEffectClick(mouseX, mouseY);
                return true;
            }

            // 3. Obsługa kliknięcia "Ulepsz"
            if (this.currentTab == Tab.UPGRADES) {
                int currentTier = data.tier();
                if (currentTier < 10) {
                    int panelY = this.topPos + 30 + 25;
                    int costY = panelY + 90 + 15;
                    int btnWidth = 100;
                    int btnHeight = 20;
                    int btnX = this.leftPos + (this.imageWidth / 2) - (btnWidth / 2);
                    int btnY = costY + 40;

                    if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {
                        // Wysłanie żądania ulepszenia!
                        Services.PLATFORM.sendToServer(new UpgradeBaseCorePayload(data.pos()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void handleEffectClick(double mouseX, double mouseY) {
        int columns = 5;
        int iconSize = 24;
        int spacingX = 20;
        int spacingY = 20;
        int startX = this.leftPos + 8 + 25;
        int startY = this.topPos + 30 + 15;

        for (int i = 0; i < AVAILABLE_EFFECTS.length; i++) {
            EffectInfo effect = AVAILABLE_EFFECTS[i];
            int col = i % columns;
            int row = i / columns;
            int x = startX + (col * (iconSize + spacingX));
            int y = startY + (row * (iconSize + spacingY));

            if (mouseX >= x && mouseX < x + iconSize && mouseY >= y && mouseY < y + iconSize) {
                boolean isUnlocked = data.unlockedEffects().contains(effect.id);

                if (!isUnlocked) {
                    // Kupowanie efektu (używamy slotIndex -1 jako flagi zakupu)
                    Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), effect.id, -1));
                } else {
                    // Przypisywanie odblokowanego (tymczasowo zawsze do slotu 0)
                    // TODO: Dodać logikę wybierania aktywnego slotu przez gracza na zakładce Przegląd
                    Services.PLATFORM.sendToServer(new UnlockEffectPayload(data.pos(), effect.id, 0));
                }
                return;
            }
        }
    }

    // --- ZAKŁADKA 1: GŁÓWNA ---
    private void renderOverviewTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin + 10;
        int contentY = this.topPos + 35;

        // Tytuł sekcji slotów
        graphics.text(this.font, "Zainstalowane Moduły", contentX, contentY, 0xFFDDDDDD, false);

        // 4 Ramki dla aktywnych slotów (wyśrodkowane po lewej stronie ekranu)
        int slotSize = 36;
        int slotSpacing = 8;
        int slotsStartX = contentX;
        int slotsStartY = contentY + 15;

        for (int i = 0; i < 4; i++) {
            int slotX = slotsStartX + (i * (slotSize + slotSpacing));

            // Tło slota (ciemne wgłębienie)
            graphics.fill(slotX, slotsStartY, slotX + slotSize, slotsStartY + slotSize, 0xFF101010);

            // Ramka slota
            int borderColor = 0xFF555555;
            graphics.outline(slotX, slotsStartY, slotSize, slotSize, borderColor);

            // Pobieramy ID efektu z naszej paczki sieciowej (lub "empty", jeśli pusty)
            String effectId = "empty";
            if (i < data.activeSlots().size()) {
                effectId = data.activeSlots().get(i);
            }

            // Jeśli slot jest pusty, rysujemy znak zapytania/plus
            if (effectId.equals("empty")) {
                String emptyText = "+";
                int textW = this.font.width(emptyText);
                graphics.text(this.font, emptyText, slotX + (slotSize - textW) / 2, slotsStartY + (slotSize - 8) / 2, 0xFF444444, false);
            } else {
                // TODO: Tutaj w przyszłości wyrysujemy ładną ikonę efektu (np. blitSprite)
                graphics.text(this.font, "ON", slotX + 10, slotsStartY + 14, 0xFF00FF00, false);
            }

            // Podświetlenie hover na slocie
            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotsStartY && mouseY < slotsStartY + slotSize) {
                graphics.fill(slotX + 1, slotsStartY + 1, slotX + slotSize - 1, slotsStartY + slotSize - 1, 0x44FFFFFF);
            }
        }

        // --- SEKCJA ZASIĘGU (Po prawej stronie) ---
        int rangeBoxX = this.leftPos + this.imageWidth - 100 - innerMargin;
        int rangeBoxY = contentY;
        int rangeBoxWidth = 90;
        int rangeBoxHeight = 60;

        // Panel zasięgu
        graphics.fill(rangeBoxX, rangeBoxY, rangeBoxX + rangeBoxWidth, rangeBoxY + rangeBoxHeight, 0xFF202020);
        graphics.outline(rangeBoxX, rangeBoxY, rangeBoxWidth, rangeBoxHeight, 0xFF444444);

        graphics.centeredText(this.font, "Zasięg Rdzenia", rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 8, 0xFFAAAAAA);

        // Obliczony zasięg na podstawie Tieru (np. 16 bloków + 8 za każdy Tier)
        int currentRange = 16 + (data.tier() * 8);

        // Duży, wyraźny tekst zasięgu (zazwyczaj byśmy tu powiększyli font, ale w domyślnym GUI po prostu go ładnie wyśrodkujemy i damy złoty kolor)
        String rangeValue = currentRange + "x" + currentRange;
        graphics.centeredText(this.font, rangeValue, rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 30, 0xFFFF55);
        graphics.centeredText(this.font, "bloków", rangeBoxX + (rangeBoxWidth / 2), rangeBoxY + 45, 0xFF777777);
    }

    // --- ZAKŁADKA 2: EFEKTY ---
    private void renderEffectsTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 30;

        graphics.text(this.font, "Dostępne Protokoły i Efekty", contentX + 8, contentY, 0xFFDDDDDD, false);

        // Parametry siatki efektów
        int columns = 5;
        int iconSize = 24;
        int spacingX = 20;
        int spacingY = 20;
        int startX = contentX + 25;
        int startY = contentY + 15;

        // Iterujemy przez naszą listę wszystkich możliwych efektów
        for (int i = 0; i < AVAILABLE_EFFECTS.length; i++) {
            EffectInfo effect = AVAILABLE_EFFECTS[i];

            // Obliczanie pozycji X i Y w siatce
            int col = i % columns;
            int row = i / columns;
            int x = startX + (col * (iconSize + spacingX));
            int y = startY + (row * (iconSize + spacingY));

            boolean isUnlocked = data.unlockedEffects().contains(effect.id);
            boolean isHovered = mouseX >= x && mouseX < x + iconSize && mouseY >= y && mouseY < y + iconSize;

            // Tło ikony (szare jeśli zablokowane, złote/jasne jeśli odblokowane)
            int bgColor = isUnlocked ? 0xFF2A2A1A : 0xFF151515;
            int borderColor = isUnlocked ? 0xFFD700 : 0xFF444444;

            if (isHovered && !isUnlocked) {
                bgColor = 0xFF252525; // Lekkie podświetlenie, gdy zablokowane
            } else if (isHovered && isUnlocked) {
                bgColor = 0xFF3A3A20; // Silniejsze podświetlenie, gdy odblokowane
            }

            graphics.fill(x, y, x + iconSize, y + iconSize, bgColor);
            graphics.outline(x, y, iconSize, iconSize, borderColor);

            // Ikona (Placeholder - pierwsza litera ID)
            String shortName = effect.name.substring(0, 1);
            int textColor = isUnlocked ? 0xFFFFFF : 0xFF777777;
            graphics.centeredText(this.font, shortName, x + (iconSize / 2), y + (iconSize / 2) - 4, textColor);

            // Rysowanie ikony "Kłódki", jeśli efekt jest zablokowany
            if (!isUnlocked) {
                graphics.fill(x + iconSize - 6, y + iconSize - 6, x + iconSize, y + iconSize, 0xAA000000);
                graphics.text(this.font, "X", x + iconSize - 5, y + iconSize - 6, 0xFFFF5555, false);
            }

            // --- OBSŁUGA HOVER I TOOLTIPÓW ---
            if (isHovered) {
                // W nowoczesnym systemie nie rysujemy tooltipu bezpośrednio w pętli.
                // Używamy zlecenia na następną klatkę (Deferred Tooltip), by rysował się ZAWSZE na wierzchu!
                renderEffectTooltip(graphics, effect, isUnlocked, mouseX, mouseY);
            }
        }
    }

    private void renderEffectTooltip(GuiGraphicsExtractor graphics, EffectInfo effect, boolean isUnlocked, int mouseX, int mouseY) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();

        // Tytuł (Złoty, jeśli odblokowany)
        tooltipLines.add(Component.literal(effect.name).withStyle(isUnlocked ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));

        // Opis
        tooltipLines.add(Component.literal(effect.description).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        // Koszt (tylko, jeśli zablokowane)
        if (!isUnlocked) {
            tooltipLines.add(Component.literal("")); // Pusta linia dla odstępu
            tooltipLines.add(Component.literal("Wymagania:").withStyle(net.minecraft.ChatFormatting.RED));

            // TODO: W przyszłości podepniemy tu faktyczne sprawdzanie ekwipunku gracza
            // np. "64/128 Diamenty" (zielone/czerwone w zależności od tego, czy gracz ma przedmioty)
            tooltipLines.add(Component.literal("- " + effect.costDescription).withStyle(net.minecraft.ChatFormatting.WHITE));

            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Kliknij, aby odblokować").withStyle(net.minecraft.ChatFormatting.YELLOW));
        } else {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Kliknij, aby przypisać do wolnego slotu").withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
    }

    private static class EffectInfo {
        final String id;
        final String name;
        final String description;
        final String costDescription;

        EffectInfo(String id, String name, String description, String costDescription) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.costDescription = costDescription;
        }
    }

    private static final EffectInfo[] AVAILABLE_EFFECTS = {
            new EffectInfo("haste", "Wzmocnienie Kopania", "Zwiększa prędkość niszczenia bloków.", "250 XP, 10x Blok Złota"),
            new EffectInfo("resistance", "Tarcza Bazy", "Redukuje obrażenia otrzymywane w zasięgu bazy.", "500 XP, 1x Tarcza, 5x Żelazo"),
            new EffectInfo("healing", "Pole Leczące", "Powoli regeneruje zdrowie sojuszników.", "800 XP, 1x Złote Jabłko"),
            new EffectInfo("grief_ward", "Zabezpieczenie", "Creepery nie niszczą terenu.", "1200 XP, 10x Obsydian"),
            new EffectInfo("pet_ward", "Ochrona Zwierząt", "Zwierzęta są niewrażliwe na obrażenia.", "300 XP, 5x Pszenica"),
            new EffectInfo("flight", "Strefa Lotu", "Pozwala na latanie wewnątrz bazy.", "5000 XP, 1x Elytra, 1x Gwiazda Netheru"),
            new EffectInfo("night_vision", "Noktowizja", "Usuwa ciemność na terenie bazy.", "150 XP, 1x Złota Marchew"),
            new EffectInfo("water_breathing", "Podwodna Kopuła", "Nieskończone oddychanie w wodzie.", "200 XP, 1x Skorupa Żółwia"),
            new EffectInfo("speed", "Szybkość", "Zwiększona prędkość poruszania się.", "150 XP, 10x Cukier"),
            new EffectInfo("jump_boost", "Skok", "Zwiększona wysokość skoku.", "150 XP, 1x Kurza Łapka"),
            new EffectInfo("fire_resistance", "Odporność na Ogień", "Odporność na lawę i ogień.", "250 XP, 1x Magmowy Krem"),
            new EffectInfo("strength", "Siła", "Zwiększone obrażenia w walce.", "350 XP, 5x Płomienna Różdżka"),
            new EffectInfo("invisibility", "Niewidzialność", "Pozwala ukryć graczy przed potworami.", "400 XP, 1x Sfermentowane Oko Pająka"),
            new EffectInfo("slow_falling", "Powolne Opadanie", "Ochrona przed obrażeniami od upadku.", "150 XP, 1x Membrana Fentoma")
    };

    // --- ZAKŁADKA 3: ULEPSZENIA ---
    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int innerMargin = 8;
        int contentX = this.leftPos + innerMargin;
        int contentY = this.topPos + 30;

        graphics.text(this.font, "Architektura Rdzenia", contentX + 8, contentY, 0xFFDDDDDD, false);

        int currentTier = data.tier();
        boolean isMaxTier = currentTier >= 10; // Zakładamy, że 10 to maks

        // --- GŁÓWNY PANEL ULEPSZENIA (Centralnie) ---
        int panelWidth = 200;
        int panelHeight = 90;
        int panelX = this.leftPos + (this.imageWidth - panelWidth) / 2;
        int panelY = contentY + 25;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1C1C1C);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF444444);

        if (isMaxTier) {
            graphics.centeredText(this.font, "Rdzeń Osiągnął Limit Architektury", panelX + (panelWidth / 2), panelY + 40, 0xFFFF55);
            graphics.centeredText(this.font, "Maksymalny Poziom: " + toRoman(currentTier), panelX + (panelWidth / 2), panelY + 55, 0xFFD700);
            return; // Przerwij rysowanie, bo nie ma już ulepszeń
        }

        int nextTier = currentTier + 1;

        // Rysowanie graficznego przejścia: "Poziom I -> Poziom II"
        int centerX = panelX + (panelWidth / 2);

        // Lewy blok (Obecny poziom)
        graphics.fill(centerX - 70, panelY + 15, centerX - 30, panelY + 55, 0xFF2A2A2A);
        graphics.outline(centerX - 70, panelY + 15, 40, 40, 0xFF555555);
        graphics.centeredText(this.font, toRoman(currentTier), centerX - 50, panelY + 31, 0xFFD700);
        graphics.centeredText(this.font, "Obecny", centerX - 50, panelY + 60, 0xFFAAAAAA);

        // Strzałka (Klimatyczna, składana z linii)
        graphics.fill(centerX - 15, panelY + 33, centerX + 10, panelY + 37, 0xFF777777); // Rdzeń strzałki
        graphics.fill(centerX + 5, panelY + 28, centerX + 10, panelY + 42, 0xFF777777);  // Grot
        graphics.fill(centerX + 10, panelY + 30, centerX + 13, panelY + 40, 0xFF777777);
        graphics.fill(centerX + 13, panelY + 32, centerX + 16, panelY + 38, 0xFF777777);

        // Prawy blok (Następny poziom)
        graphics.fill(centerX + 30, panelY + 15, centerX + 70, panelY + 55, 0xFF333322); // Lekko złotawy odcień
        graphics.outline(centerX + 30, panelY + 15, 40, 40, 0xFFD700);
        graphics.centeredText(this.font, toRoman(nextTier), centerX + 50, panelY + 31, 0xFFFFFF);
        graphics.centeredText(this.font, "Następny", centerX + 50, panelY + 60, 0xFFD700);

        // --- KOSZT I PRZYCISK ULEPSZENIA ---
        UpgradeCost cost = getUpgradeCost(nextTier);

        // Wyświetlanie kosztów pod panelem
        int costY = panelY + panelHeight + 15;
        graphics.centeredText(this.font, "Wymagane zasoby:", centerX, costY, 0xFFDDDDDD);

        // TODO: Logika sprawdzająca czy gracz ma przedmioty w eq
        boolean hasItems = false; // Zakładamy false dla placeholdera, dopóki nie dopiszemy sprawdzania

        // Rysujemy pasek XP
        String xpText = "Koszt XP: " + cost.xpRequired + " punktów";
        graphics.centeredText(this.font, xpText, centerX, costY + 12, hasItems ? 0xFF55FF55 : 0xFFFF5555);

        // Rysujemy koszt w itemach
        String itemText = "Przedmioty: " + cost.itemAmount + "x " + cost.itemName;
        graphics.centeredText(this.font, itemText, centerX, costY + 24, hasItems ? 0xFFFFFFFF : 0xFFFF5555);

        // --- PRZYCISK "ULEPSZ" ---
        int btnWidth = 100;
        int btnHeight = 20;
        int btnX = centerX - (btnWidth / 2);
        int btnY = costY + 40;

        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;
        int btnColor = isBtnHovered ? 0xFF2A8B2A : 0xFF1B5E1B; // Zielony przycisk
        int btnOutline = isBtnHovered ? 0xFF3CDA3C : 0xFF288B28;

        if (!hasItems) {
            btnColor = 0xFF4A4A4A; // Szary, jeśli nas nie stać
            btnOutline = 0xFF6A6A6A;
        }

        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
        graphics.outline(btnX, btnY, btnWidth, btnHeight, btnOutline);
        graphics.centeredText(this.font, "ROZPOCZNIJ ULEPSZENIE", centerX, btnY + 6, hasItems ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private static class UpgradeCost {
        final int xpRequired;
        final int itemAmount;
        final String itemName; // Placeholderowa nazwa
        // TODO: W przyszłości zamienimy String itemName na prawdziwe Ingredient/Item

        UpgradeCost(int xpRequired, int itemAmount, String itemName) {
            this.xpRequired = xpRequired;
            this.itemAmount = itemAmount;
            this.itemName = itemName;
        }
    }

    private UpgradeCost getUpgradeCost(int targetTier) {
        return switch (targetTier) {
            case 1 -> new UpgradeCost(100, 10, "Blok Żelaza");
            case 2 -> new UpgradeCost(250, 15, "Blok Złota");
            case 3 -> new UpgradeCost(500, 5, "Diament");
            case 4 -> new UpgradeCost(1000, 1, "Sztabka Netherytu");
            case 5 -> new UpgradeCost(2000, 1, "Gwiazda Netheru");
            default -> new UpgradeCost(targetTier * 1000, targetTier, "Tajemniczy Artefakt");
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
}