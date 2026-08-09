package com.r3ct.base_core.item;

import com.r3ct.base_core.logic.LecternRecipes;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class EmpoweredTomeItem extends Item {

    public EmpoweredTomeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        String effectId = stack.get(ModDataComponents.EFFECT_ID);

        if (effectId != null) {
            return LecternRecipes.getRecipeById(effectId)
                    .map(recipe -> {
                        ChatFormatting color = ChatFormatting.GOLD;

                        if ("r3ct_base_core:magic_tome".equals(recipe.inputItem())) {
                            color = ChatFormatting.AQUA;
                        } else if ("r3ct_base_core:alchemy_tome".equals(recipe.inputItem())) {
                            color = ChatFormatting.GREEN;
                        } else if ("r3ct_base_core:dark_magic_tome".equals(recipe.inputItem())) {
                            color = ChatFormatting.LIGHT_PURPLE;
                        }

                        return Component.translatable("r3ct_base_core.tome." + recipe.effectId()).withStyle(color);
                    })
                    .orElse(Component.translatable("item.r3ct_base_core.empowered_tome"));
        }

        return super.getName(stack).copy().withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);

        String effectId = itemStack.get(ModDataComponents.EFFECT_ID);

        if (effectId != null) {
            LecternRecipes.getRecipeById(effectId).ifPresent(recipe -> {
                builder.accept(Component.translatable(recipe.descKey() + ".1").withStyle(ChatFormatting.GRAY));
                builder.accept(Component.translatable(recipe.descKey() + ".2").withStyle(ChatFormatting.GRAY));
                builder.accept(Component.empty());
                builder.accept(Component.translatable("item.r3ct_base_core.empowered_tome.module").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            });
        } else {
            builder.accept(Component.translatable("item.r3ct_base_core.empowered_tome.empty").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static ItemStack createFromRecipe(Item item, com.r3ct.base_core.config.LecternRecipeDef recipe) {
        ItemStack stack = new ItemStack(item);
        if (recipe.effectId() != null && !recipe.effectId().isEmpty()) {
            stack.set(ModDataComponents.EFFECT_ID, recipe.effectId());

            String tomeType = "0";
            if ("r3ct_base_core:magic_tome".equals(recipe.inputItem())) tomeType = "1";
            else if ("r3ct_base_core:alchemy_tome".equals(recipe.inputItem())) tomeType = "2";
            else if ("r3ct_base_core:dark_magic_tome".equals(recipe.inputItem())) tomeType = "3";

            int tintColor = -1;
            if (recipe.colorHex() != null && !recipe.colorHex().isEmpty()) {
                try {
                    String hex = recipe.colorHex().replace("#", "");
                    tintColor = (0xFF << 24) | Integer.parseInt(hex, 16);
                } catch (NumberFormatException ignored) {}
            }

            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of(tomeType),
                            java.util.List.of(tintColor)
                    )
            );
        }
        return stack;
    }
}