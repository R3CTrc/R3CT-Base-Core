package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RemoveEffectPayload(BlockPos pos, int slotIndex) implements CustomPacketPayload {
    public static final Type<RemoveEffectPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":remove_effect"));

    public static final StreamCodec<FriendlyByteBuf, RemoveEffectPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RemoveEffectPayload::pos,
            ByteBufCodecs.INT, RemoveEffectPayload::slotIndex,
            RemoveEffectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}