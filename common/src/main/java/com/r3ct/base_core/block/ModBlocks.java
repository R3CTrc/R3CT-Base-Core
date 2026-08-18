package com.r3ct.base_core.block;

import com.r3ct.base_core.Constants;
import com.r3ct.base_core.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final ResourceKey<Block> BASE_CORE_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.parse(Constants.MOD_ID + ":base_core")
    );

    public static final Block BASE_CORE = new BaseCoreBlock(BlockBehaviour.Properties.of()
            .setId(BASE_CORE_KEY)
            .strength(5.0f, 1200.0f)
            .noOcclusion()
            .isViewBlocking((state, getter, pos) -> false)
            .sound(SoundType.WOOD)
    );

    public static final ResourceKey<BlockEntityType<?>> BASE_CORE_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":base_core_be")
    );

    public static final BlockEntityType<BaseCoreBlockEntity> BASE_CORE_BE_TYPE = Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new BaseCoreBlockEntity(ModBlocks.BASE_CORE_BE_TYPE, pos, state),
            BASE_CORE
    );

    public static final ResourceKey<Block> ARCANE_LECTERN_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.parse(Constants.MOD_ID + ":arcane_lectern")
    );

    public static final Block ARCANE_LECTERN = new ArcaneLecternBlock(Block.Properties.of()
            .setId(ARCANE_LECTERN_KEY)
            .strength(2.5F)
            .noOcclusion()
            .isViewBlocking((state, getter, pos) -> false)
            .sound(SoundType.WOOD)
            .lightLevel(state -> 7)
    );

    public static final ResourceKey<BlockEntityType<?>> ARCANE_LECTERN_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":arcane_lectern_be")
    );

    public static final BlockEntityType<ArcaneLecternBlockEntity> ARCANE_LECTERN_BE_TYPE = Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new ArcaneLecternBlockEntity(pos, state),
            ARCANE_LECTERN
    );

    public static final ResourceKey<Block> MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":mailbox"));
    public static final Block MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER).randomTicks());

    public static final ResourceKey<Block> EXPOSED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":exposed_mailbox"));
    public static final Block EXPOSED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(EXPOSED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER).randomTicks());

    public static final ResourceKey<Block> WEATHERED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":weathered_mailbox"));
    public static final Block WEATHERED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(WEATHERED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER).randomTicks());

    public static final ResourceKey<Block> OXIDIZED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":oxidized_mailbox"));
    public static final Block OXIDIZED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(OXIDIZED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER).randomTicks());

    public static final ResourceKey<Block> WAXED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":waxed_mailbox"));
    public static final Block WAXED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(WAXED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER));

    public static final ResourceKey<Block> WAXED_EXPOSED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":waxed_exposed_mailbox"));
    public static final Block WAXED_EXPOSED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(WAXED_EXPOSED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER));

    public static final ResourceKey<Block> WAXED_WEATHERED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":waxed_weathered_mailbox"));
    public static final Block WAXED_WEATHERED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(WAXED_WEATHERED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER));

    public static final ResourceKey<Block> WAXED_OXIDIZED_MAILBOX_KEY = ResourceKey.create(Registries.BLOCK, Identifier.parse(Constants.MOD_ID + ":waxed_oxidized_mailbox"));
    public static final Block WAXED_OXIDIZED_MAILBOX = new MailboxBlock(BlockBehaviour.Properties.of().setId(WAXED_OXIDIZED_MAILBOX_KEY).strength(3.0f, 1200.0f).noOcclusion().sound(SoundType.COPPER));

    public static final ResourceKey<BlockEntityType<?>> MAILBOX_BE_KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Identifier.parse(Constants.MOD_ID + ":mailbox_be"));
    public static final BlockEntityType<MailboxBlockEntity> MAILBOX_BE_TYPE = Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new MailboxBlockEntity(pos, state),
            MAILBOX, EXPOSED_MAILBOX, WEATHERED_MAILBOX, OXIDIZED_MAILBOX,
            WAXED_MAILBOX, WAXED_EXPOSED_MAILBOX, WAXED_WEATHERED_MAILBOX, WAXED_OXIDIZED_MAILBOX
    );

    public static Block getOxidized(Block current) {
        if (current == MAILBOX) return EXPOSED_MAILBOX;
        if (current == EXPOSED_MAILBOX) return WEATHERED_MAILBOX;
        if (current == WEATHERED_MAILBOX) return OXIDIZED_MAILBOX;
        return null;
    }

    public static Block getUnoxidized(Block current) {
        if (current == OXIDIZED_MAILBOX) return WEATHERED_MAILBOX;
        if (current == WEATHERED_MAILBOX) return EXPOSED_MAILBOX;
        if (current == EXPOSED_MAILBOX) return MAILBOX;
        return null;
    }

    public static Block getWaxed(Block current) {
        if (current == MAILBOX) return WAXED_MAILBOX;
        if (current == EXPOSED_MAILBOX) return WAXED_EXPOSED_MAILBOX;
        if (current == WEATHERED_MAILBOX) return WAXED_WEATHERED_MAILBOX;
        if (current == OXIDIZED_MAILBOX) return WAXED_OXIDIZED_MAILBOX;
        return null;
    }

    public static Block getUnwaxed(Block current) {
        if (current == WAXED_MAILBOX) return MAILBOX;
        if (current == WAXED_EXPOSED_MAILBOX) return EXPOSED_MAILBOX;
        if (current == WAXED_WEATHERED_MAILBOX) return WEATHERED_MAILBOX;
        if (current == WAXED_OXIDIZED_MAILBOX) return OXIDIZED_MAILBOX;
        return null;
    }
}