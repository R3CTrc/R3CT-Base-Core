package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleBorderPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleBorderPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_base_core:toggle_border"));

    public static final StreamCodec<FriendlyByteBuf, ToggleBorderPayload> CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf),
            ToggleBorderPayload::new
    );

    private ToggleBorderPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}