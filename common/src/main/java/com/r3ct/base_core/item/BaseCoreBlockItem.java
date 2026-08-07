package com.r3ct.base_core.item;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.logic.LecternRecipes;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BaseCoreBlockItem extends BlockItem {

    public BaseCoreBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        int tier = 0;

        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            tier = tag.getInt("baseCoreTier").orElse(0);
        }

        int range = BaseCoreServerConfig.calculateRangeUpToTier(tier);
        int slots = BaseCoreServerConfig.calculateTotalSlots(tier);

        String diameterStr = range == 0 ? "0x0x0" : (range * 2 + 1) + "x" + (range * 2 + 1) + "x" + (range * 2 + 1);
        String displayTier = tier == 0 ? "0" : String.valueOf(tier);

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.tier")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(displayTier).withStyle(ChatFormatting.AQUA)));

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.area")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(range + " (" + diameterStr + ")").withStyle(ChatFormatting.AQUA)));

        builder.accept(Component.translatable("r3ct_base_core.gui.stats.slots")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(slots)).withStyle(ChatFormatting.DARK_GREEN)));

        ItemContainerContents contents = itemStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<String> effects = new ArrayList<>();

        for (ItemStackTemplate template : contents.nonEmptyItems()) {
            String effectId = template.get(ModDataComponents.EFFECT_ID);

            if (effectId != null && !effectId.isEmpty()) {
                effects.add(effectId);
            }
        }

        if (!effects.isEmpty()) {
            builder.accept(Component.empty());
            builder.accept(Component.translatable("r3ct_base_core.gui.stats.active_effects").withStyle(ChatFormatting.GRAY));

            for (String effectId : effects) {
                LecternRecipes.getRecipeById(effectId).ifPresent(recipe -> {
                    builder.accept(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.translatable(recipe.nameKey()).withStyle(ChatFormatting.GOLD)));
                });
            }
        }
    }
}