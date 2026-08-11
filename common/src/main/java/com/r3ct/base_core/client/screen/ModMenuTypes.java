package com.r3ct.base_core.client.screen;

import com.r3ct.base_core.platform.Services;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static final MenuType<ArcaneLecternMenu> ARCANE_LECTERN_MENU = Services.PLATFORM.createMenuType(ArcaneLecternMenu::new);
    public static final MenuType<BaseCoreMenu> BASE_CORE_MENU = Services.PLATFORM.createMenuType(BaseCoreMenu::new);
    public static final MenuType<BaseCoreVisitorMenu> BASE_CORE_VISITOR_MENU = Services.PLATFORM.createMenuType(BaseCoreVisitorMenu::new);
    public static final MenuType<MailboxVisitorMenu> MAILBOX_VISITOR_MENU = Services.PLATFORM.createMenuType(MailboxVisitorMenu::new);
    public static final MenuType<MailboxOwnerMenu> MAILBOX_OWNER_MENU = Services.PLATFORM.createMenuType(MailboxOwnerMenu::new);
}