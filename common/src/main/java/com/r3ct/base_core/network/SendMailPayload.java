package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendMailPayload(BlockPos pos, int slotIndex, String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SendMailPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_base_core:send_mail"));

    public static final StreamCodec<FriendlyByteBuf, SendMailPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SendMailPayload::pos,
            ByteBufCodecs.INT, SendMailPayload::slotIndex,
            ByteBufCodecs.stringUtf8(1024), SendMailPayload::message,
            SendMailPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}