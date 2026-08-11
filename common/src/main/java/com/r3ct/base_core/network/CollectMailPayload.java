package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CollectMailPayload(BlockPos pos, int messageIndex) implements CustomPacketPayload {
    public static final Type<CollectMailPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":collect_mail"));

    public static final StreamCodec<FriendlyByteBuf, CollectMailPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            CollectMailPayload::pos,
            ByteBufCodecs.VAR_INT,
            CollectMailPayload::messageIndex,
            CollectMailPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}