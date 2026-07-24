package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LecternCraftPayload(String effectId) implements CustomPacketPayload {
    public static final Type<LecternCraftPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":lectern_craft"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LecternCraftPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LecternCraftPayload::effectId,
            LecternCraftPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}