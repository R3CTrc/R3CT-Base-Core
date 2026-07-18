package com.r3ct.base_core.block;

import com.r3ct.base_core.Constants;
import com.r3ct.base_core.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final ResourceKey<Block> BASE_CORE_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.parse(Constants.MOD_ID + ":base_core")
    );

    public static final Block BASE_CORE = new BaseCoreBlock(BlockBehaviour.Properties.of()
            .setId(BASE_CORE_KEY)
            .strength(20.0f, 1200.0f)
            .noOcclusion()
            .isViewBlocking((state, getter, pos) -> false)
    );

    public static final ResourceKey<BlockEntityType<?>> BASE_CORE_BE_KEY = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.parse(Constants.MOD_ID + ":base_core_be")
    );

    public static final BlockEntityType<BaseCoreBlockEntity> BASE_CORE_BE_TYPE = Services.PLATFORM.createBlockEntityType(
            (pos, state) -> new BaseCoreBlockEntity(ModBlocks.BASE_CORE_BE_TYPE, pos, state),
            BASE_CORE
    );
}