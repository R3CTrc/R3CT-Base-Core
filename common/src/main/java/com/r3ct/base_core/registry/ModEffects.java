package com.r3ct.base_core.registry;

import com.r3ct.base_core.effect.BaseCoreEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ModEffects {

    public static final MobEffect PVP_PROTECTION = new BaseCoreEffect(0xAAAAAA);
    public static final MobEffect FALL_RESISTANCE = new BaseCoreEffect(0xFFFFFF);
    public static final MobEffect SATIATION = new BaseCoreEffect(0xFFD700);
    public static final MobEffect FIRE_IMMUNITY = new BaseCoreEffect(0xFFA500);
    public static final MobEffect NIGHT_VISION = new BaseCoreEffect(0x00008B);
    public static final MobEffect EXTENDED_REACH = new BaseCoreEffect(0x8B4513);
    public static final MobEffect MENDING_PULSE = new BaseCoreEffect(0x22FF22);
    public static final MobEffect PET_PROTECTION = new BaseCoreEffect(0x55FFFF);
    public static final MobEffect LIVESTOCK_BOOST = new BaseCoreEffect(0xFF66B2);
    public static final MobEffect TWIN_BREEDING = new BaseCoreEffect(0xFF3333);
    public static final MobEffect HOSTILE_SLOWNESS = new BaseCoreEffect(MobEffectCategory.HARMFUL, 0xDDDDDD);

}