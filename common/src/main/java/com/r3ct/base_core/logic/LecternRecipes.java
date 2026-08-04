package com.r3ct.base_core.logic;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.config.LecternRecipeDef;
import java.util.List;
import java.util.Optional;

public class LecternRecipes {

    public static List<LecternRecipeDef> getRecipes() {
        return BaseCoreServerConfig.getInstance().lecternRecipes;
    }

    public static Optional<LecternRecipeDef> getRecipeById(String id) {
        return getRecipes().stream().filter(r -> r.id().equals(id)).findFirst();
    }
}