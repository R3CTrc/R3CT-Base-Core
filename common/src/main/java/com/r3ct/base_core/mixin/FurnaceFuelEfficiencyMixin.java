package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class FurnaceFuelEfficiencyMixin {

    @Shadow private int litTimeRemaining;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity entity, CallbackInfo ci) {
        FurnaceFuelEfficiencyMixin mixin = (FurnaceFuelEfficiencyMixin) (Object) entity;

        if (mixin.litTimeRemaining > 0) {
            if (level.getGameTime() % 5 == 0) {
                if (BaseCoreEventLogic.isEffectActiveAt(level, pos, "fuel_efficiency")) {
                    mixin.litTimeRemaining++;
                }
            }
        }
    }
}