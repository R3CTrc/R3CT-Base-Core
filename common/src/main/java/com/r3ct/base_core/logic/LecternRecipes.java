package com.r3ct.base_core.logic;

import com.r3ct.base_core.config.EffectDef;
import java.util.List;
import java.util.Optional;

public class LecternRecipes {

    public static final List<EffectDef> RECIPES = List.of(
            new EffectDef("furnace_overclock", "r3ct_base_core.effect.furnace_overclock.name", "r3ct_base_core.effect.furnace_overclock.desc",
                    150, "r3ct_base_core:magic_tome", "minecraft:blast_furnace", 3, 1),

            new EffectDef("anti_trample", "r3ct_base_core.effect.anti_trample.name", "r3ct_base_core.effect.anti_trample.desc",
                    100, "r3ct_base_core:magic_tome", "minecraft:slime_ball", 16, 1),

            new EffectDef("crop_growth", "r3ct_base_core.effect.crop_growth.name", "r3ct_base_core.effect.crop_growth.desc",
                    250, "r3ct_base_core:alchemy_tome", "minecraft:bone_meal", 64, 1),

            new EffectDef("fall_resistance", "r3ct_base_core.effect.fall_resistance.name", "r3ct_base_core.effect.fall_resistance.desc",
                    350, "r3ct_base_core:alchemy_tome", "minecraft:hay_block", 16, 2),

            new EffectDef("satiation", "r3ct_base_core.effect.satiation.name", "r3ct_base_core.effect.satiation.desc",
                    400, "r3ct_base_core:alchemy_tome", "minecraft:golden_carrot", 16, 2),

            new EffectDef("anti_spawn", "r3ct_base_core.effect.anti_spawn.name", "r3ct_base_core.effect.anti_spawn.desc",
                    500, "r3ct_base_core:dark_magic_tome", "minecraft:carved_pumpkin", 16, 2),

            new EffectDef("night_vision", "r3ct_base_core.effect.night_vision.name", "r3ct_base_core.effect.night_vision.desc",
                    600, "r3ct_base_core:alchemy_tome", "minecraft:glowstone", 32, 3),

            new EffectDef("animal_growth", "r3ct_base_core.effect.animal_growth.name", "r3ct_base_core.effect.animal_growth.desc",
                    650, "r3ct_base_core:alchemy_tome", "minecraft:golden_apple", 4, 3),

            new EffectDef("extended_reach", "r3ct_base_core.effect.extended_reach.name", "r3ct_base_core.effect.extended_reach.desc",
                    800, "r3ct_base_core:magic_tome", "minecraft:ender_pearl", 16, 3),

            new EffectDef("fire_immunity", "r3ct_base_core.effect.fire_immunity.name", "r3ct_base_core.effect.fire_immunity.desc",
                    900, "r3ct_base_core:alchemy_tome", "minecraft:magma_cream", 32, 4),

            new EffectDef("anti_explosion", "r3ct_base_core.effect.anti_explosion.name", "r3ct_base_core.effect.anti_explosion.desc",
                    1000, "r3ct_base_core:dark_magic_tome", "minecraft:crying_obsidian", 8, 4),

            new EffectDef("pet_protection", "r3ct_base_core.effect.pet_protection.name", "r3ct_base_core.effect.pet_protection.desc",
                    1100, "r3ct_base_core:magic_tome", "minecraft:diamond_horse_armor", 1, 4),

            new EffectDef("slow_falling", "r3ct_base_core.effect.slow_falling.name", "r3ct_base_core.effect.slow_falling.desc",
                    1200, "r3ct_base_core:alchemy_tome", "minecraft:phantom_membrane", 16, 5),

            new EffectDef("mending_pulse", "r3ct_base_core.effect.mending_pulse.name", "r3ct_base_core.effect.mending_pulse.desc",
                    1500, "r3ct_base_core:magic_tome", "minecraft:nether_star", 1, 5)
    );

    public static Optional<EffectDef> getRecipeById(String id) {
        return RECIPES.stream().filter(r -> r.id().equals(id)).findFirst();
    }
}