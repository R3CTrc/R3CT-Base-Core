package com.r3ct.base_core.block;

import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.logic.BaseCoreServerLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
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
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiConsumer;

public class BaseCoreBlock extends Block implements EntityBlock {

    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 11);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape LEG_NW = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D);
    private static final VoxelShape LEG_NE = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
    private static final VoxelShape LEG_SW = Block.box(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D);
    private static final VoxelShape LEG_SE = Block.box(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape LOWER_SHELF = Block.box(0.0D, 5.0D, 0.0D, 16.0D, 7.0D, 16.0D);
    private static final VoxelShape TABLE_TOP = Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    private static final VoxelShape SHAPE = Shapes.or(LEG_NW, LEG_NE, LEG_SW, LEG_SE, LOWER_SHELF, TABLE_TOP);

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
                coreBE.setOwnerUUID(player.getUUID().toString());

                data.hasPlacedCore = true;
                data.coreDimension = level.dimension().identifier().toString();
                data.coreX = pos.getX();
                data.coreY = pos.getY();
                data.coreZ = pos.getZ();
                data.coreTier = coreBE.getTier();

                data.lastKnownName = player.getName().getString();

                data.activeSlots = new ArrayList<>(coreBE.getActiveEffectsFromTomes());

                ModState.get(level.getServer()).setDirty();
                BaseCoreServerLogic.grantAdvancement(player, "root");

                player.connection.send(new ClientboundCustomPayloadPacket(
                        new com.r3ct.base_core.network.SyncCoreStatePayload(true, pos, data.coreDimension)
                ));

                level.setBlock(pos, state.setValue(TIER, coreBE.getTier()), 3);
                BaseCoreServerLogic.broadcastAllCores(level.getServer());
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
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level fullLevel) {
            clearCoreLimit(fullLevel, pos);
        }
        super.destroy(level, pos, state);
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
                net.minecraft.world.Containers.dropContents(level, pos, coreBE);

                String uuidStr = coreBE.getOwnerUUID();
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    try {
                        UUID ownerId = UUID.fromString(uuidStr);
                        PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);

                        if (data.hasPlacedCore && data.coreX == pos.getX() && data.coreY == pos.getY() && data.coreZ == pos.getZ()) {
                            data.hasPlacedCore = false;
                            data.coreTier = 0;
                            data.activeSlots.clear();
                            ModState.get(level.getServer()).setDirty();

                            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(ownerId);
                            if (ownerPlayer != null) {
                                ownerPlayer.connection.send(new ClientboundCustomPayloadPacket(
                                        new com.r3ct.base_core.network.SyncCoreStatePayload(false, BlockPos.ZERO, "")
                                ));
                            }
                            BaseCoreServerLogic.broadcastAllCores(level.getServer());
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
                    serverPlayer.openMenu(coreBE);
                } else {
                    String ownerName = "Unknown";
                    try {
                        UUID ownerId = UUID.fromString(coreBE.getOwnerUUID());
                        PlayerData ownerData = ModState.getPlayerData(serverPlayer.level().getServer(), ownerId);

                        if (ownerData.lastKnownName != null && !ownerData.lastKnownName.isEmpty()) {
                            ownerName = ownerData.lastKnownName;
                        }
                    } catch (Exception ignored) {}

                    final String finalOwnerName = ownerName;

                    serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inv, p) -> {
                                net.minecraft.world.SimpleContainer visitorContainer = new net.minecraft.world.SimpleContainer(4);
                                for(int i = 0; i < 4; i++) {
                                    visitorContainer.setItem(i, coreBE.getItem(i).copy());
                                }

                                net.minecraft.world.inventory.ContainerData visitorData = new net.minecraft.world.inventory.ContainerData() {
                                    @Override
                                    public int get(int index) {
                                        return switch (index) {
                                            case 0 -> coreBE.getTier();
                                            case 1 -> coreBE.getShowBorder() ? 1 : 0;
                                            case 2 -> pos.getX();
                                            case 3 -> pos.getY();
                                            case 4 -> pos.getZ();
                                            default -> 0;
                                        };
                                    }
                                    @Override
                                    public void set(int index, int value) {}
                                    @Override
                                    public int getCount() { return 5; }
                                };

                                return new com.r3ct.base_core.client.screen.BaseCoreVisitorMenu(id, visitorContainer, visitorData);
                            },
                            Component.literal(finalOwnerName)
                    ));
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