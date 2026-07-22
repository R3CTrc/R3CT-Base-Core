package com.r3ct.base_core.block;

import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import com.r3ct.base_core.network.OpenBaseCoreGuiPayload;
import com.r3ct.base_core.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class BaseCoreBlock extends Block implements EntityBlock {

    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 11);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 25.0D, 14.0D);

    public BaseCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, 0).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();

        if (player instanceof ServerPlayer serverPlayer && !level.isClientSide()) {
            PlayerData data = ModState.getPlayerData(level.getServer(), serverPlayer.getUUID());

            if (data.hasPlacedCore) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(data.coreDimension));
                ServerLevel targetLevel = level.getServer().getLevel(dimKey);
                BlockPos targetPos = new BlockPos(data.coreX, data.coreY, data.coreZ);

                boolean coreExists = false;

                if (targetLevel != null) {
                    if (targetLevel.isLoaded(targetPos)) {
                        BlockEntity targetBE = targetLevel.getBlockEntity(targetPos);
                        if (targetBE instanceof BaseCoreBlockEntity coreBE && serverPlayer.getUUID().toString().equals(coreBE.getOwnerUUID())) {
                            coreExists = true;
                        }
                    } else {
                        coreExists = true;
                    }
                }

                if (coreExists) {
                    serverPlayer.sendSystemMessage(Component.literal("§cMasz już Serce Bazy! Znajduje się na kordach: X:" + data.coreX + " Y:" + data.coreY + " Z:" + data.coreZ + " (" + data.coreDimension + ")").withStyle(ChatFormatting.RED), true);
                    if (targetLevel != null && !targetLevel.isLoaded(targetPos)) {
                        serverPlayer.sendSystemMessage(Component.literal("§7Jeśli blok uległ zniszczeniu przez błąd, udaj się na tamte kordy aby załadować teren. Gra zobaczy, że go nie ma i automatycznie zresetuje Twój limit."));
                    }
                    return null;
                } else {
                    data.hasPlacedCore = false;
                    ModState.get(level.getServer()).setDirty();
                }
            }
        }

        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BaseCoreBlockEntity(ModBlocks.BASE_CORE_BE_TYPE, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }

        if (type == ModBlocks.BASE_CORE_BE_TYPE) {
            return (lvl, pos, st, be) -> BaseCoreBlockEntity.tick(lvl, pos, st, (BaseCoreBlockEntity) be);
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof BaseCoreBlockEntity coreBE) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null && !customData.isEmpty()) {
                    net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                    tag.getString("OwnerUUID").ifPresent(coreBE::setOwnerUUID);
                }

                if (coreBE.getOwnerUUID() == null || coreBE.getOwnerUUID().isEmpty()) {
                    coreBE.setOwnerUUID(player.getUUID().toString());
                }

                if (coreBE.getOwnerUUID().equals(player.getUUID().toString())) {
                    data.hasPlacedCore = true;
                    data.coreDimension = level.dimension().identifier().toString();
                    data.coreX = pos.getX();
                    data.coreY = pos.getY();
                    data.coreZ = pos.getZ();
                    ModState.get(level.getServer()).setDirty();

                    BaseCoreServerLogic.grantAdvancement(player, "root");
                }

                level.setBlock(pos, state.setValue(TIER, coreBE.getTier()), 3);
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity coreBE) {
                if (coreBE.getTier() >= 11) {
                    BaseCoreServerLogic.grantAdvancement(serverPlayer, "moving_day");
                }
            }
        }

        clearCoreLimit(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        clearCoreLimit(level, pos);
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BaseCoreBlockEntity coreBE) {
            String owner = coreBE.getOwnerUUID();

            if (owner != null && !owner.isEmpty() && !owner.equals(player.getUUID().toString())) {
                return 0.0F;
            }
        }

        return super.getDestroyProgress(state, player, level, pos);
    }

    private void clearCoreLimit(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BaseCoreBlockEntity coreBE) {
                String uuidStr = coreBE.getOwnerUUID();
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    try {
                        UUID ownerId = UUID.fromString(uuidStr);
                        PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);

                        if (data.hasPlacedCore && data.coreX == pos.getX() && data.coreY == pos.getY() && data.coreZ == pos.getZ()) {
                            data.hasPlacedCore = false;
                            ModState.get(level.getServer()).setDirty();
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseCoreBlockEntity coreBE) {
                if (coreBE.getOwnerUUID().equals(player.getUUID().toString())) {

                    OpenBaseCoreGuiPayload payload = new OpenBaseCoreGuiPayload(
                            pos,
                            coreBE.getTier(),
                            coreBE.getActiveEffects(),
                            coreBE.getActiveSlots()
                    );

                    Services.PLATFORM.sendToPlayer(serverPlayer, payload);

                } else {
                    serverPlayer.sendSystemMessage(Component.translatable("r3ct_base_core.message.not_your_base"), true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof BaseCoreBlockEntity coreBE) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                if (coreBE.getOwnerUUID() != null && !coreBE.getOwnerUUID().isEmpty()) {
                    tag.putString("OwnerUUID", coreBE.getOwnerUUID());
                }
                tag.putInt("baseCoreTier", coreBE.getTier());
            });

            net.minecraft.world.item.component.CustomModelData customModelData =
                    new net.minecraft.world.item.component.CustomModelData(java.util.List.of((float) coreBE.getTier()), java.util.List.of(), java.util.List.of(), java.util.List.of());
            stack.set(DataComponents.CUSTOM_MODEL_DATA, customModelData);
        }
        return stack;
    }
}