package com.r3ct.base_core.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BaseCoreEffect extends MobEffect {
    public BaseCoreEffect(int colorHex) {
        super(MobEffectCategory.BENEFICIAL, colorHex);
    }
}