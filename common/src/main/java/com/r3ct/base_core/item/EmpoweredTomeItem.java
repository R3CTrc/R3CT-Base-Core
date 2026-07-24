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
                    .map(recipe -> Component.translatable(recipe.name()).withStyle(ChatFormatting.GOLD))
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
                builder.accept(Component.translatable(recipe.description()).withStyle(ChatFormatting.GRAY));
                builder.accept(Component.empty());
                builder.accept(Component.literal("Moduł Rdzenia Bazy").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            });
        } else {
            builder.accept(Component.literal("Brak mocy. Pusty moduł.").withStyle(ChatFormatting.DARK_RED));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.EFFECT_ID);
    }
}