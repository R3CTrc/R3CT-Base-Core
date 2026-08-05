package com.r3ct.base_core.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

public class ArcaneLecternBlock extends BaseEntityBlock {

    public static final MapCodec<ArcaneLecternBlock> CODEC = simpleCodec(ArcaneLecternBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_BASE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    private static final VoxelShape SHAPE_POST = Block.box(4.0D, 2.0D, 4.0D, 12.0D, 11.5D, 12.0D);
    private static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(2.0D, 13.25D, 2.25D,  14.0D, 16.25D, 5.5D),
            Block.box(2.0D, 12.25D, 5.25D,  14.0D, 15.25D, 8.5D),
            Block.box(2.0D, 11.25D, 8.25D,  14.0D, 14.25D, 11.5D),
            Block.box(2.0D, 10.25D, 11.25D, 14.0D, 13.25D, 14.5D),
            SHAPE_COMMON
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(2.0D, 13.25D, 10.5D, 14.0D, 16.25D, 13.75D),
            Block.box(2.0D, 12.25D, 7.5D,  14.0D, 15.25D, 10.75D),
            Block.box(2.0D, 11.25D, 4.5D,  14.0D, 14.25D, 7.75D),
            Block.box(2.0D, 10.25D, 1.5D,  14.0D, 13.25D, 4.75D),
            SHAPE_COMMON
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(10.5D, 13.25D, 2.0D, 13.75D, 16.25D, 14.0D),
            Block.box(7.5D,  12.25D, 2.0D, 10.75D, 15.25D, 14.0D),
            Block.box(4.5D,  11.25D, 2.0D, 7.75D,  14.25D, 14.0D),
            Block.box(1.5D,  10.25D, 2.0D, 4.75D,  13.25D, 14.0D),
            SHAPE_COMMON
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(2.25D,  13.25D, 2.0D, 5.5D,   16.25D, 14.0D),
            Block.box(5.25D,  12.25D, 2.0D, 8.5D,   15.25D, 14.0D),
            Block.box(8.25D,  11.25D, 2.0D, 11.5D,  14.25D, 14.0D),
            Block.box(11.25D, 10.25D, 2.0D, 14.5D,  13.25D, 14.0D),
            SHAPE_COMMON
    );

    public ArcaneLecternBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_COMMON;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneLecternBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    private void dropContents(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ArcaneLecternBlockEntity arcaneLectern) {
                Containers.dropContents(level, pos, arcaneLectern);
                arcaneLectern.clearContent();
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropContents(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level fullLevel) {
            dropContents(fullLevel, pos);
        }
        super.destroy(level, pos, state);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        dropContents(level, pos);
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);

        spawnRotatedCandleFlame(level, pos, random, facing, 1.5, 19.5, 12.0);

        spawnRotatedCandleFlame(level, pos, random, facing, 14.5, 19.5, 9.0);
    }

    private void spawnRotatedCandleFlame(Level level, BlockPos pos, RandomSource random, Direction facing, double pixelX, double pixelY, double pixelZ) {
        double offsetX = pixelX / 16.0;
        double offsetY = pixelY / 16.0;
        double offsetZ = pixelZ / 16.0;

        double dx = offsetX - 0.5;
        double dz = offsetZ - 0.5;

        double finalX = 0.5;
        double finalZ = 0.5;

        switch (facing) {
            case EAST -> { finalX = 0.5 - dz; finalZ = 0.5 + dx; }
            case SOUTH -> { finalX = 0.5 - dx; finalZ = 0.5 - dz; }
            case WEST -> { finalX = 0.5 + dz; finalZ = 0.5 - dx; }
            default -> { finalX = 0.5 + dx; finalZ = 0.5 + dz; }
        }

        level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + finalX, pos.getY() + offsetY, pos.getZ() + finalZ, 0.0, 0.0, 0.0);

        if (random.nextFloat() < 0.3F) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + finalX, pos.getY() + offsetY, pos.getZ() + finalZ, 0.0, 0.0, 0.0);
        }
    }
}