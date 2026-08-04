package com.r3ct.base_core.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record LecternRecipeDef(
        String id,
        String nameKey,
        String descKey,
        int xpCost,
        String inputItem,
        String ingredientItem,
        int ingredientAmount,
        String outputItem,
        String effectId
) {
    public Item getInputItem() {
        return BuiltInRegistries.ITEM.get(Identifier.parse(this.inputItem)).map(Holder::value).orElse(Items.AIR);
    }

    public Item getIngredientItem() {
        return BuiltInRegistries.ITEM.get(Identifier.parse(this.ingredientItem)).map(Holder::value).orElse(Items.AIR);
    }

    public Item getOutputItem() {
        return BuiltInRegistries.ITEM.get(Identifier.parse(this.outputItem)).map(Holder::value).orElse(Items.AIR);
    }
}