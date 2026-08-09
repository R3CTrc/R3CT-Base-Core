package com.r3ct.base_core.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class BlueprintItem extends Item {

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("item.r3ct_base_core.blueprint.desc.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.r3ct_base_core.blueprint.desc.2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
    }
}