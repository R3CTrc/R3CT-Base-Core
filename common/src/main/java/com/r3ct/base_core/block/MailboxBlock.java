package com.r3ct.base_core.block;

import com.r3ct.base_core.data.MailMessage;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class MailboxBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> ATTACH_FACE = BlockStateProperties.ATTACH_FACE;
    public static final BooleanProperty HAS_MAIL = BooleanProperty.create("has_mail");

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public MailboxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ATTACH_FACE, AttachFace.FLOOR)
                .setValue(HAS_MAIL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ATTACH_FACE, HAS_MAIL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        if (clickedFace == Direction.DOWN) {
            return null;
        }

        if (clickedFace == Direction.UP) {
            return this.defaultBlockState()
                    .setValue(ATTACH_FACE, AttachFace.FLOOR)
                    .setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        else {
            return this.defaultBlockState()
                    .setValue(ATTACH_FACE, AttachFace.WALL)
                    .setValue(FACING, clickedFace);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MailboxBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            PlayerData data = ModState.getPlayerData(level.getServer(), player.getUUID());
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof MailboxBlockEntity mailboxBE) {
                mailboxBE.setOwnerUUID(player.getUUID().toString());

                data.hasPlacedMailbox = true;
                data.mailboxDimension = level.dimension().identifier().toString();
                data.mailboxX = pos.getX();
                data.mailboxY = pos.getY();
                data.mailboxZ = pos.getZ();

                ModState.get(level.getServer()).setDirty();

                player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                        new com.r3ct.base_core.network.SyncMailboxStatePayload(true, pos, data.mailboxDimension)
                ));
            }
        }
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MailboxBlockEntity mailboxBE) {
            String owner = mailboxBE.getOwnerUUID();
            if (owner != null && !owner.isEmpty()) {
                if (!owner.equals(player.getUUID().toString())) {
                    return 0.0F;
                } else if (!mailboxBE.isCompletelyEmpty()) {
                    return 0.0F;
                }
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MailboxBlockEntity mailboxBE) {
                if (mailboxBE.getOwnerUUID().equals(player.getUUID().toString()) && !mailboxBE.isCompletelyEmpty()) {
                    player.sendSystemMessage(Component.translatable("r3ct_base_core.message.mailbox_not_empty").withStyle(ChatFormatting.RED));
                }
            }
        }

        clearMailboxLimit(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (level instanceof Level fullLevel) {
            clearMailboxLimit(fullLevel, pos);
        }
        super.destroy(level, pos, state);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        clearMailboxLimit(level, pos);
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    private void clearMailboxLimit(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MailboxBlockEntity mailboxBE) {
                for (com.r3ct.base_core.data.MailMessage msg : mailboxBE.getMessages()) {
                    for (ItemStack stack : msg.getAttachedItems()) {
                        if (!stack.isEmpty()) {
                            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                }

                String uuidStr = mailboxBE.getOwnerUUID();
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    try {
                        UUID ownerId = UUID.fromString(uuidStr);
                        PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);

                        if (data.hasPlacedMailbox && data.mailboxX == pos.getX() && data.mailboxY == pos.getY() && data.mailboxZ == pos.getZ()) {
                            data.hasPlacedMailbox = false;
                            ModState.get(level.getServer()).setDirty();

                            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(ownerId);
                            if (ownerPlayer != null) {
                                ownerPlayer.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
                                        new com.r3ct.base_core.network.SyncMailboxStatePayload(false, BlockPos.ZERO, "")
                                ));
                            }
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
            if (blockEntity instanceof MailboxBlockEntity mailboxBE) {

                if (mailboxBE.getOwnerUUID().equals(player.getUUID().toString())) {
                    serverPlayer.openMenu(mailboxBE);
                } else {
                    serverPlayer.openMenu(mailboxBE.getVisitorMenuProvider());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}