package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceOverclockMixin {

    @Shadow
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity) {
    }

    @Unique
    private static final ThreadLocal<Boolean> baseCore$isExtraTick = ThreadLocal.withInitial(() -> false);

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void onServerTickTail(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        if (baseCore$isExtraTick.get()) return;
        if (level.getGameTime() % 4 != 0) return;
        BlockState currentState = level.getBlockState(pos);
        if (!currentState.hasProperty(BlockStateProperties.LIT) || !currentState.getValue(BlockStateProperties.LIT)) {
            return;
        }
        if (BaseCoreEventLogic.isEffectActiveAt(level, pos, "furnace_overclock")) {
            baseCore$isExtraTick.set(true);
            try {
                serverTick(level, pos, currentState, entity);
            } finally {
                baseCore$isExtraTick.set(false);
            }
        }
    }
}