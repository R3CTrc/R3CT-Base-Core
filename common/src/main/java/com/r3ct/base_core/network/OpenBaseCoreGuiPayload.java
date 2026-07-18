package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenBaseCoreGuiPayload(BlockPos pos, int tier, List<String> unlockedEffects, List<String> activeSlots) implements CustomPacketPayload {
    public static final Type<OpenBaseCoreGuiPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":open_base_core_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenBaseCoreGuiPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenBaseCoreGuiPayload::pos,
            ByteBufCodecs.INT, OpenBaseCoreGuiPayload::tier,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenBaseCoreGuiPayload::unlockedEffects,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenBaseCoreGuiPayload::activeSlots,
            OpenBaseCoreGuiPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}