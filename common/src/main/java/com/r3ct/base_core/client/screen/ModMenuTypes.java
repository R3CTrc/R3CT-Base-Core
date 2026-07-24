package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.platform.Services;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static final MenuType<ArcaneLecternMenu> ARCANE_LECTERN_MENU = Services.PLATFORM.createMenuType(ArcaneLecternMenu::new);
}