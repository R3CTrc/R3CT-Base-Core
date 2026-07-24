package com.r3ct.base_core.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record EffectDef(
        String id,
        String name,
        String description,
        int xpCost,
        String requiredTome,
        String itemCost,
        int itemAmount,
        int pool
) {
    public Item getCostItem() {
        return BuiltInRegistries.ITEM.get(Identifier.parse(this.itemCost)).map(Holder::value).orElse(Items.AIR);
    }

    public Item getTomeItem() {
        return BuiltInRegistries.ITEM.get(Identifier.parse(this.requiredTome)).map(Holder::value).orElse(Items.AIR);
    }
}