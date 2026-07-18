package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UnlockEffectPayload(BlockPos pos, String effectId, int slotIndex) implements CustomPacketPayload {
    public static final Type<UnlockEffectPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":unlock_effect"));

    public static final StreamCodec<FriendlyByteBuf, UnlockEffectPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UnlockEffectPayload::pos,
            ByteBufCodecs.STRING_UTF8, UnlockEffectPayload::effectId,
            ByteBufCodecs.INT, UnlockEffectPayload::slotIndex,
            UnlockEffectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}