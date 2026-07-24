package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LecternAutoFillPayload(String effectId) implements CustomPacketPayload {
    public static final Type<LecternAutoFillPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":lectern_autofill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LecternAutoFillPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LecternAutoFillPayload::effectId,
            LecternAutoFillPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}