package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ApplyEffectsPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ApplyEffectsPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":apply_effects"));

    public static final StreamCodec<FriendlyByteBuf, ApplyEffectsPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ApplyEffectsPayload::pos,
            ApplyEffectsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}