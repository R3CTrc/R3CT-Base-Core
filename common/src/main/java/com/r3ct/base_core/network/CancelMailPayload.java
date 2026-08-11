package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CancelMailPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CancelMailPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_base_core:cancel_mail"));

    public static final StreamCodec<FriendlyByteBuf, CancelMailPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CancelMailPayload::pos,
            CancelMailPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}