package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncMailboxStatePayload(boolean hasMailbox, BlockPos pos, String dimension) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncMailboxStatePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_base_core:sync_mailbox_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncMailboxStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncMailboxStatePayload::hasMailbox,
            BlockPos.STREAM_CODEC, SyncMailboxStatePayload::pos,
            ByteBufCodecs.STRING_UTF8, SyncMailboxStatePayload::dimension,
            SyncMailboxStatePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}