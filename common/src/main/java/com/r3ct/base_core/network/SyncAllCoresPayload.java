package com.r3ct.base_core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.r3ct.base_core.Constants;

import java.util.ArrayList;
import java.util.List;

public record SyncAllCoresPayload(List<CoreData> cores) implements CustomPacketPayload {
    public static final Type<SyncAllCoresPayload> TYPE = new Type<>(Identifier.parse(Constants.MOD_ID + ":sync_all_cores"));

    public record CoreData(String dimension, BlockPos pos) {}

    public static final StreamCodec<FriendlyByteBuf, SyncAllCoresPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeCollection(payload.cores(), (b, core) -> {
                    b.writeUtf(core.dimension());
                    b.writeBlockPos(core.pos());
                });
            },
            buf -> {
                List<CoreData> list = buf.readList(b -> new CoreData(b.readUtf(), b.readBlockPos()));
                return new SyncAllCoresPayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}