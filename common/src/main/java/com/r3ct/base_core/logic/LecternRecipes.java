package com.r3ct.base_core.logic;

import com.r3ct.base_core.config.LecternRecipeDef;
import java.util.List;
import java.util.Optional;

public class LecternRecipes {

    public static final List<LecternRecipeDef> RECIPES = List.of(
            new LecternRecipeDef("base_magic", "r3ct_base_core.recipe.base_magic", "Tworzy Podstawową Księgę Magii",
                    50, "minecraft:book", "minecraft:lapis_lazuli", 4, "r3ct_base_core:magic_tome", null),

            new LecternRecipeDef("base_dark_magic", "r3ct_base_core.recipe.base_dark_magic", "Tworzy Podstawową Księgę Czarnej Magii",
                    50, "minecraft:book", "minecraft:bone", 4, "r3ct_base_core:dark_magic_tome", null),

            new LecternRecipeDef("base_alchemy", "r3ct_base_core.recipe.base_alchemy", "Tworzy Podstawową Księgę Alchemii",
                    50, "minecraft:book", "minecraft:nether_wart", 4, "r3ct_base_core:alchemy_tome", null),

            new LecternRecipeDef("furnace_overclock", "r3ct_base_core.effect.furnace_overclock.name", "r3ct_base_core.effect.furnace_overclock.desc",
                    150, "r3ct_base_core:magic_tome", "minecraft:blast_furnace", 3, "r3ct_base_core:empowered_tome", "furnace_overclock"),

            new LecternRecipeDef("anti_trample", "r3ct_base_core.effect.anti_trample.name", "r3ct_base_core.effect.anti_trample.desc",
                    100, "r3ct_base_core:magic_tome", "minecraft:slime_ball", 16, "r3ct_base_core:empowered_tome", "anti_trample"),

            new LecternRecipeDef("crop_growth", "r3ct_base_core.effect.crop_growth.name", "r3ct_base_core.effect.crop_growth.desc",
                    250, "r3ct_base_core:alchemy_tome", "minecraft:bone_meal", 64, "r3ct_base_core:empowered_tome", "crop_growth"),

            new LecternRecipeDef("fall_resistance", "r3ct_base_core.effect.fall_resistance.name", "r3ct_base_core.effect.fall_resistance.desc",
                    350, "r3ct_base_core:alchemy_tome", "minecraft:hay_block", 16, "r3ct_base_core:empowered_tome", "fall_resistance"),

            new LecternRecipeDef("satiation", "r3ct_base_core.effect.satiation.name", "r3ct_base_core.effect.satiation.desc",
                    400, "r3ct_base_core:alchemy_tome", "minecraft:golden_carrot", 16, "r3ct_base_core:empowered_tome", "satiation"),

            new LecternRecipeDef("anti_spawn", "r3ct_base_core.effect.anti_spawn.name", "r3ct_base_core.effect.anti_spawn.desc",
                    500, "r3ct_base_core:dark_magic_tome", "minecraft:carved_pumpkin", 16, "r3ct_base_core:empowered_tome", "anti_spawn"),

            new LecternRecipeDef("night_vision", "r3ct_base_core.effect.night_vision.name", "r3ct_base_core.effect.night_vision.desc",
                    600, "r3ct_base_core:alchemy_tome", "minecraft:glowstone", 32, "r3ct_base_core:empowered_tome", "night_vision"),

            new LecternRecipeDef("animal_growth", "r3ct_base_core.effect.animal_growth.name", "r3ct_base_core.effect.animal_growth.desc",
                    650, "r3ct_base_core:alchemy_tome", "minecraft:golden_apple", 4, "r3ct_base_core:empowered_tome", "animal_growth"),

            new LecternRecipeDef("extended_reach", "r3ct_base_core.effect.extended_reach.name", "r3ct_base_core.effect.extended_reach.desc",
                    800, "r3ct_base_core:magic_tome", "minecraft:ender_pearl", 16, "r3ct_base_core:empowered_tome", "extended_reach"),

            new LecternRecipeDef("fire_immunity", "r3ct_base_core.effect.fire_immunity.name", "r3ct_base_core.effect.fire_immunity.desc",
                    900, "r3ct_base_core:alchemy_tome", "minecraft:magma_cream", 32, "r3ct_base_core:empowered_tome", "fire_immunity"),

            new LecternRecipeDef("anti_explosion", "r3ct_base_core.effect.anti_explosion.name", "r3ct_base_core.effect.anti_explosion.desc",
                    1000, "r3ct_base_core:dark_magic_tome", "minecraft:crying_obsidian", 8, "r3ct_base_core:empowered_tome", "anti_explosion"),

            new LecternRecipeDef("pet_protection", "r3ct_base_core.effect.pet_protection.name", "r3ct_base_core.effect.pet_protection.desc",
                    1100, "r3ct_base_core:magic_tome", "minecraft:diamond_horse_armor", 1, "r3ct_base_core:empowered_tome", "pet_protection"),

            new LecternRecipeDef("slow_falling", "r3ct_base_core.effect.slow_falling.name", "r3ct_base_core.effect.slow_falling.desc",
                    1200, "r3ct_base_core:alchemy_tome", "minecraft:phantom_membrane", 16, "r3ct_base_core:empowered_tome", "slow_falling"),

            new LecternRecipeDef("mending_pulse", "r3ct_base_core.effect.mending_pulse.name", "r3ct_base_core.effect.mending_pulse.desc",
                    1500, "r3ct_base_core:magic_tome", "minecraft:nether_star", 1, "r3ct_base_core:empowered_tome", "mending_pulse")
    );

    public static Optional<LecternRecipeDef> getRecipeById(String id) {
        return RECIPES.stream().filter(r -> r.id().equals(id)).findFirst();
    }
}