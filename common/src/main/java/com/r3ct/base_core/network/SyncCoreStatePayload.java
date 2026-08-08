package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncCoreStatePayload(boolean hasCore, BlockPos pos, String dimension) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncCoreStatePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_base_core:sync_core_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncCoreStatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncCoreStatePayload::hasCore,
            BlockPos.STREAM_CODEC, SyncCoreStatePayload::pos,
            ByteBufCodecs.STRING_UTF8, SyncCoreStatePayload::dimension,
            SyncCoreStatePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}