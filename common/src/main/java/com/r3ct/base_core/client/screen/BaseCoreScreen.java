package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.network.ApplyEffectsPayload;
import com.r3ct.base_core.network.ToggleBorderPayload;
import com.r3ct.base_core.network.UpgradeBaseCorePayload;
import com.r3ct.base_core.platform.Services;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

public class BaseCoreScreen extends AbstractContainerScreen<BaseCoreMenu> {

    private static final int CUSTOM_IMAGE_WIDTH = 176;
    private static final int CUSTOM_IMAGE_HEIGHT = 224;

    private Tab currentTab = Tab.OVERVIEW;

    private final int tabWidth = 80;
    private final int tabHeight = 20;
    private final int tabSpacing = 5;

    private final int toggleButtonSize = 20;
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
        this.menu.isOverviewTab = (this.currentTab == Tab.OVERVIEW);
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

    private boolean isBorderEnabled() {
        com.r3ct.base_core.block.BaseCoreBlockEntity core = getCoreEntity();
        if (core != null) return core.getShowBorder();
        return this.menu.isBorderVisible();
    }

    @Override
    protected void slotClicked(net.minecraft.world.inventory.Slot slot, int slotId, int mouseButton, ContainerInput type) {
        if (slot != null && slotId >= 12 && type == ContainerInput.QUICK_MOVE) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                boolean isEffect = stack.has(ModDataComponents.EFFECT_ID);
                boolean isUpgrade = false;

                BaseCoreServerConfig.TierUpgrade nextTier = BaseCoreServerConfig.getTier(this.menu.getTier() + 1);
                if (nextTier != null) {
                    Item reqMain = BuiltInRegistries.ITEM.get(Identifier.parse(nextTier.mainItem)).map(Holder::value).orElse(Items.AIR);
                    Item reqBulk = BuiltInRegistries.ITEM.get(Identifier.parse(nextTier.bulkItem)).map(Holder::value).orElse(Items.AIR);
                    if (stack.is(reqMain) || stack.is(reqBulk)) isUpgrade = true;
                }

                if (this.currentTab == Tab.OVERVIEW && !isEffect) {
                    return;
                }
                if (this.currentTab == Tab.UPGRADES && !isUpgrade) {
                    return;
                }
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
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

        drawPlanks(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        drawThickOutline(graphics, this.leftPos, this.topPos + 134, this.imageWidth, 90, 2, 0xFF2E1F14);

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
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(tier);
        Component titleComp = currentTierConfig != null ? Component.translatable(currentTierConfig.title) : Component.translatable("r3ct_base_core.gui.tier.0");
        String romanTier = tier == 0 ? "0" : toRoman(tier);

        Component titleDisplay = Component.translatable("block.r3ct_base_core.base_core");
        Component topDisplay = Component.empty().append(titleComp).append(" (").append(romanTier).append(")");

        graphics.text(this.font, titleDisplay, this.leftPos + 12, this.topPos + 6, 0xFFEFEBE9, true);
        graphics.text(this.font, topDisplay, this.leftPos + this.imageWidth - this.font.width(topDisplay) - 12, this.topPos + 6, 0xFFEFEBE9, true);

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
        int bgColor = isSelected ? 0xFF4A3424 : (isHovered ? 0xFF634631 : 0xFF2E1F14);
        int borderColor = 0xFF2E1F14;
        int textColor = isSelected ? 0xFFEFEBE9 : (isHovered ? 0xFFFFFFFF : 0xFFBBBBBB);

        graphics.fill(x, y, x + width, y + height, bgColor);
        graphics.fill(x - 2, y - 2, x + width + 2, y, borderColor);
        graphics.fill(x - 2, y, x, y + height, borderColor);
        graphics.fill(x + width, y, x + width + 2, y + height, borderColor);

        if (!isSelected) {
            graphics.fill(x, y + height, x + width, y + height + 2, borderColor);
        } else {
            graphics.fill(x, y + height, x + width, y + height + 2, 0xFF4A3424);
        }

        int textX = x + (width - this.font.width(text)) / 2;
        graphics.text(this.font, text, textX, y + (height - 8) / 2, textColor, true);
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
                Tab tab = Tab.values()[i];
                int currentTabX = startX + (i * (tabWidth + tabSpacing));
                if (mouseX >= currentTabX && mouseX < currentTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
                    if (this.currentTab != tab) {
                        this.currentTab = tab;
                        this.menu.isOverviewTab = (this.currentTab == Tab.OVERVIEW);
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }

            if (this.currentTab == Tab.OVERVIEW) {
                if (mouseX >= this.toggleButtonX && mouseX < this.toggleButtonX + this.toggleButtonSize &&
                        mouseY >= this.toggleButtonY && mouseY < this.toggleButtonY + this.toggleButtonSize) {

                    Services.PLATFORM.sendToServer(new ToggleBorderPayload(this.menu.getCorePos()));

                    com.r3ct.base_core.block.BaseCoreBlockEntity core = getCoreEntity();
                    if (core != null) core.toggleShowBorder();

                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }

                com.r3ct.base_core.block.BaseCoreBlockEntity coreBE = getCoreEntity();
                if (coreBE != null) {
                    int maxSlots = BaseCoreServerConfig.calculateTotalSlots(this.menu.getTier());
                    for (int i = 0; i < maxSlots; i++) {
                        int sx = this.leftPos + this.menu.getSlot(i).x;
                        int sy = this.topPos + this.menu.getSlot(i).y;
                        if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                            if (!coreBE.getItem(i).isEmpty()) {
                                Services.PLATFORM.sendToServer(new com.r3ct.base_core.network.RemoveEffectPayload(this.menu.getCorePos(), i));
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                                return true;
                            }
                        }
                    }
                }

                int requiredXp = 0;
                boolean hasStagedEffects = false;
                for(int i = 0; i < 4; i++) {
                    ItemStack staged = this.menu.getSlot(i).getItem();
                    if (!staged.isEmpty()) {
                        hasStagedEffects = true;
                        String effectId = staged.get(ModDataComponents.EFFECT_ID);
                        if (effectId != null) {
                            var recipeOpt = com.r3ct.base_core.logic.LecternRecipes.getRecipeById(effectId);
                            if (recipeOpt.isPresent()) {
                                requiredXp += recipeOpt.get().xpCost();
                            }
                        }
                    }
                }

                int btnX = this.leftPos + 100;
                int btnY = this.topPos + 105;
                if (mouseX >= btnX && mouseX < btnX + 60 && mouseY >= btnY && mouseY < btnY + 16) {
                    if (hasStagedEffects) {
                        boolean hasEnoughXp = this.minecraft.player.isCreative() || com.r3ct.base_core.client.screen.ArcaneLecternMenu.getTotalExperience(this.minecraft.player) >= requiredXp;
                        if (hasEnoughXp) {
                            Services.PLATFORM.sendToServer(new ApplyEffectsPayload(this.menu.getCorePos()));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
                        } else {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENDERMAN_TELEPORT, 1.0F));
                        }
                    }
                    return true;
                }
            }

            if (this.currentTab == Tab.UPGRADES) {
                int currentTier = this.menu.getTier();
                BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);

                if (nextTierConfig != null) {
                    int btnWidth = 80;
                    int btnHeight = 20;
                    int btnX = this.leftPos + 48;
                    int btnY = this.topPos + 106;

                    if (mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight) {

                        Item mainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
                        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

                        int stagedMain = 0;
                        int stagedBulk = 0;
                        for(int i = 4; i <= 7; i++) {
                            ItemStack stack = this.menu.getSlot(i).getItem();
                            if (stack.is(mainItem)) stagedMain += stack.getCount();
                        }
                        for(int i = 8; i <= 11; i++) {
                            ItemStack stack = this.menu.getSlot(i).getItem();
                            if (stack.is(bulkItem)) stagedBulk += stack.getCount();
                        }

                        if (stagedMain >= nextTierConfig.mainAmount && stagedBulk >= nextTierConfig.bulkAmount) {
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
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(tier);
        int currentRange = calculateRangeUpToTier(tier);
        int diameterNum = currentRange == 0 ? 0 : (currentRange * 2 + 1);

        int infoX = this.leftPos + 12;
        int infoY = this.topPos + 22;
        graphics.text(this.font, Component.literal("Obszar:"), infoX, infoY, 0xFFEFEBE9, true);

        int size = 32;
        int boxX = this.leftPos + 20;
        int boxY = this.topPos + 38;
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

        graphics.text(this.font, currentRange + " blocks", boxX + 58, boxY + 12, 0xFF55FF55, true);
        graphics.text(this.font, diameterNum + " blocks", boxX + 58, boxY + 28, 0xFFFF5555, true);

        this.toggleButtonX = this.leftPos + 140;
        this.toggleButtonY = this.topPos + 52;
        boolean isHoveringToggle = mouseX >= toggleButtonX && mouseX < toggleButtonX + toggleButtonSize && mouseY >= toggleButtonY && mouseY < toggleButtonY + toggleButtonSize;
        boolean currentBorderEnabled = isBorderEnabled();

        graphics.fill(toggleButtonX, toggleButtonY, toggleButtonX + toggleButtonSize, toggleButtonY + toggleButtonSize, isHoveringToggle ? 0xFF634631 : 0xFF4A3424);
        drawThickOutline(graphics, toggleButtonX, toggleButtonY, toggleButtonSize, toggleButtonSize, 1, 0xFF2E1F14);
        graphics.fakeItem(new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS), toggleButtonX + 2, toggleButtonY + 2);

        if (currentBorderEnabled) {
            centeredText(graphics, "✔", toggleButtonX + toggleButtonSize - 3, toggleButtonY + toggleButtonSize - 7, 0xFF55FF55);
        } else {
            centeredText(graphics, "X", toggleButtonX + toggleButtonSize - 3, toggleButtonY + toggleButtonSize - 7, 0xFFFF5555);
        }

        int effectsY = this.topPos + 93;
        graphics.text(this.font, Component.literal("Efekty:"), infoX, effectsY, 0xFFEFEBE9, true);

        com.r3ct.base_core.block.BaseCoreBlockEntity coreBE = getCoreEntity();
        int requiredXp = 0;
        boolean hasStagedEffects = false;

        for (int i = 0; i < 4; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(i);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            boolean isLocked = i >= maxSlots;

            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
            graphics.fill(sx, sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
            graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xFFFFFFFF);

            ItemStack activeStack = ItemStack.EMPTY;
            if (coreBE != null) activeStack = coreBE.getItem(i);

            if (isLocked) {
                String ghostId = (i == 0 || i == 1) ? "r3ct_base_core:magic_tome" : (i == 2 ? "r3ct_base_core:alchemy_tome" : "r3ct_base_core:dark_magic_tome");
                Item ghostItem = BuiltInRegistries.ITEM.get(Identifier.parse(ghostId)).map(Holder::value).orElse(Items.BOOK);
                graphics.fakeItem(new ItemStack(ghostItem), sx, sy);

                graphics.fill(sx, sy, sx + 16, sy + 16, 0x66FFFFFF);
                centeredText(graphics, "X", sx + 8, sy + 4, 0xFFFF5555);
            } else if (!activeStack.isEmpty()) {
                graphics.fakeItem(activeStack, sx, sy);
                boolean isHoveringSlot = mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16;

                if (isHoveringSlot) {
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
                    graphics.text(this.font, "X", sx + 10, sy + 1, 0xFFFF5555, true);
                    graphics.setTooltipForNextFrame(this.font, activeStack, mouseX, mouseY);
                }
            } else if (!slot.hasItem()) {
                String ghostId = (i == 0 || i == 1) ? "r3ct_base_core:magic_tome" : (i == 2 ? "r3ct_base_core:alchemy_tome" : "r3ct_base_core:dark_magic_tome");
                Item ghostItem = BuiltInRegistries.ITEM.get(Identifier.parse(ghostId)).map(Holder::value).orElse(Items.BOOK);
                graphics.fakeItem(new ItemStack(ghostItem), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x66FFFFFF);
            } else {
                hasStagedEffects = true;
                graphics.fakeItem(slot.getItem(), sx, sy);

                String effectId = slot.getItem().get(ModDataComponents.EFFECT_ID);
                if (effectId != null) {
                    var recipeOpt = com.r3ct.base_core.logic.LecternRecipes.getRecipeById(effectId);
                    if (recipeOpt.isPresent()) {
                        requiredXp += recipeOpt.get().xpCost();
                    }
                }
            }
        }

        int btnX = this.leftPos + 100;
        int btnY = this.topPos + 105;
        boolean hasEnoughXp = this.minecraft.player.isCreative() || com.r3ct.base_core.client.screen.ArcaneLecternMenu.getTotalExperience(this.minecraft.player) >= requiredXp;
        boolean canApply = hasStagedEffects && hasEnoughXp;
        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + 60 && mouseY >= btnY && mouseY < btnY + 16;

        if (hasStagedEffects) {
            Component xpText = Component.literal("Koszt: ").withStyle(net.minecraft.ChatFormatting.WHITE)
                    .append(Component.literal(requiredXp + " XP").withStyle(hasEnoughXp ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));

            centeredText(graphics, xpText, btnX + 30, btnY - 10, 0xFFFFFFFF);
        }

        int btnColor = 0xFF4A3424;
        if (canApply) {
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
            int g = (int)(100 + 50 * pulse);
            btnColor = 0xFF000000 | (30 << 16) | (g << 8) | 30;
        } else if (isBtnHovered) {
            btnColor = 0xFF634631;
        }

        graphics.fill(btnX, btnY, btnX + 60, btnY + 16, btnColor);
        drawThickOutline(graphics, btnX, btnY, 60, 16, 1, 0xFF2E1F14);
        centeredText(graphics, Component.translatable("r3ct_base_core.gui.apply"), btnX + 30, btnY + 4, canApply ? 0xFFFFFFFF : 0xFFBBBBBB);
    }

    private void renderUpgradesTab(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int currentTier = this.menu.getTier();
        BaseCoreServerConfig.TierUpgrade currentTierConfig = BaseCoreServerConfig.getTier(currentTier);
        BaseCoreServerConfig.TierUpgrade nextTierConfig = BaseCoreServerConfig.getTier(currentTier + 1);

        if (nextTierConfig == null) {
            centeredText(graphics, Component.translatable("r3ct_base_core.gui.upgrades.max_limit"), this.leftPos + (this.imageWidth / 2), this.topPos + 50, 0xFFEFEBE9);
            return;
        }

        int topY = this.topPos + 18;
        int leftBoxX = this.leftPos + 35;
        int rightBoxX = this.leftPos + 117;

        Item currentMain = Items.STICK;
        Component currentNameComp = Component.translatable("r3ct_base_core.gui.tier.0");
        if (currentTierConfig != null) {
            currentMain = BuiltInRegistries.ITEM.get(Identifier.parse(currentTierConfig.mainItem)).map(Holder::value).orElse(Items.STICK);
            if (currentMain == Items.AIR) currentMain = Items.STICK;
            currentNameComp = Component.translatable(currentTierConfig.title);
        }

        Item nextMainItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.mainItem)).map(Holder::value).orElse(Items.AIR);
        Item bulkItem = BuiltInRegistries.ITEM.get(Identifier.parse(nextTierConfig.bulkItem)).map(Holder::value).orElse(Items.AIR);

        graphics.fill(leftBoxX, topY, leftBoxX + 24, topY + 24, 0xFF4A3424);
        drawThickOutline(graphics, leftBoxX, topY, 24, 24, 2, 0xFF2E1F14);
        graphics.fakeItem(new ItemStack(currentMain), leftBoxX + 4, topY + 4);

        String romanCurrent = currentTier == 0 ? "0" : toRoman(currentTier);
        Component currentDisplay = Component.empty().append(currentNameComp).append(" (").append(romanCurrent).append(")");
        centeredText(graphics, currentDisplay, leftBoxX + 12, topY + 28, 0xFFEFEBE9);

        int arrowX = this.leftPos + 77;
        int arrowY = topY + 4;
        Identifier VANILLA_ARROW = Identifier.parse("container/villager/trade_arrow");

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, VANILLA_ARROW, arrowX, arrowY, 24, 16, 0xFFEFEBE9);

        graphics.fill(rightBoxX, topY, rightBoxX + 24, topY + 24, 0xFF4A3424);
        drawThickOutline(graphics, rightBoxX, topY, 24, 24, 2, 0xFF2E1F14);
        graphics.fakeItem(new ItemStack(nextMainItem), rightBoxX + 4, topY + 4);

        Component nextDisplay = Component.empty().append(Component.translatable(nextTierConfig.title)).append(" (").append(toRoman(nextTierConfig.tierLevel)).append(")");
        centeredText(graphics, nextDisplay, rightBoxX + 12, topY + 28, 0xFFEFEBE9);

        int mainSlotsNeeded = (int) Math.ceil(nextTierConfig.mainAmount / 64.0);
        int bulkSlotsNeeded = (int) Math.ceil(nextTierConfig.bulkAmount / 64.0);

        int startX = this.leftPos + 24;

        int row1Y = this.topPos + 61;
        graphics.fakeItem(new ItemStack(nextMainItem), startX, row1Y - 1);
        graphics.text(this.font, "x", startX + 20, row1Y + 4, 0xFFEFEBE9, true);
        graphics.text(this.font, String.valueOf(nextTierConfig.mainAmount), startX + 30, row1Y + 4, 0xFFEFEBE9, true);

        for (int i = 0; i < mainSlotsNeeded; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(4 + i);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (slot.isActive() && !slot.hasItem()) {
                graphics.fakeItem(new ItemStack(nextMainItem), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x888B8B8B);
            }
        }

        int row2Y = this.topPos + 83;
        graphics.fakeItem(new ItemStack(bulkItem), startX, row2Y - 1);
        graphics.text(this.font, "x", startX + 20, row2Y + 4, 0xFFEFEBE9, true);
        graphics.text(this.font, String.valueOf(nextTierConfig.bulkAmount), startX + 30, row2Y + 4, 0xFFEFEBE9, true);

        for (int i = 0; i < bulkSlotsNeeded; i++) {
            net.minecraft.world.inventory.Slot slot = this.menu.getSlot(8 + i);
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (slot.isActive() && !slot.hasItem()) {
                graphics.fakeItem(new ItemStack(bulkItem), sx, sy);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0x888B8B8B);
            }
        }

        int btnWidth = 80;
        int btnHeight = 20;
        int btnX = this.leftPos + 48;
        int btnY = this.topPos + 106;

        int stagedMain = 0;
        int stagedBulk = 0;
        for(int i = 4; i <= 7; i++) {
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (stack.is(nextMainItem)) stagedMain += stack.getCount();
        }
        for(int i = 8; i <= 11; i++) {
            ItemStack stack = this.menu.getSlot(i).getItem();
            if (stack.is(bulkItem)) stagedBulk += stack.getCount();
        }

        boolean canAfford = stagedMain >= nextTierConfig.mainAmount && stagedBulk >= nextTierConfig.bulkAmount;
        boolean isBtnHovered = mouseX >= btnX && mouseX < btnX + btnWidth && mouseY >= btnY && mouseY < btnY + btnHeight;

        int boxColor = 0xFF4A3424;
        if (canAfford) {
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 150.0) + 1.0) / 2.0f;
            int g = (int)(100 + 50 * pulse);
            boxColor = 0xFF000000 | (30 << 16) | (g << 8) | 30;
        } else if (isBtnHovered) {
            boxColor = 0xFF634631;
        }

        graphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, boxColor);
        drawThickOutline(graphics, btnX, btnY, btnWidth, btnHeight, 1, 0xFF2E1F14);
        centeredText(graphics, Component.translatable("r3ct_base_core.gui.upgrades.start_upgrade"), btnX + (btnWidth / 2), btnY + 6, canAfford ? 0xFFFFFFFF : 0xFFBBBBBB);

        boolean isRightBoxHovered = mouseX >= rightBoxX && mouseX < rightBoxX + 24 && mouseY >= topY && mouseY < topY + 24;
        if (isBtnHovered || isRightBoxHovered) {
            renderTierTooltip(graphics, nextTierConfig, true, mouseX, mouseY);
        }
        if (mouseX >= startX && mouseX < startX + 16 && mouseY >= row1Y - 1 && mouseY < row1Y + 15) {
            graphics.setTooltipForNextFrame(this.font, new ItemStack(nextMainItem), mouseX, mouseY);
        }

        if (mouseX >= startX && mouseX < startX + 16 && mouseY >= row2Y - 1 && mouseY < row2Y + 15) {
            graphics.setTooltipForNextFrame(this.font, new ItemStack(bulkItem), mouseX, mouseY);
        }
    }

    private void renderTierTooltip(GuiGraphicsExtractor graphics, BaseCoreServerConfig.TierUpgrade tierConfig, boolean isNextTier, int mouseX, int mouseY) {
        java.util.List<Component> tooltipLines = new java.util.ArrayList<>();
        tooltipLines.add(Component.translatable("r3ct_base_core.gui.tier.format", Component.translatable(tierConfig.title), tierConfig.tierLevel).withStyle(net.minecraft.ChatFormatting.GOLD));

        if (tierConfig.bonusRadius > 0) {
            tooltipLines.add(Component.literal("+ " + tierConfig.bonusRadius + " ").append(Component.translatable("r3ct_base_core.gui.stats.area")).withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        if (tierConfig.bonusSlots > 0) {
            tooltipLines.add(Component.literal("+ " + tierConfig.bonusSlots + " ").append(Component.translatable("r3ct_base_core.gui.stats.slots")).withStyle(net.minecraft.ChatFormatting.GREEN));
        }

        if (isNextTier) {
            tooltipLines.add(Component.translatable("r3ct_base_core.gui.upgrades.click_to_upgrade").withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
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