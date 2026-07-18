package com.r3ct.base_core.network;

import com.r3ct.base_core.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpgradeBaseCorePayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<UpgradeBaseCorePayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":upgrade_base_core"));

    public static final StreamCodec<FriendlyByteBuf, UpgradeBaseCorePayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpgradeBaseCorePayload::pos,
            UpgradeBaseCorePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}