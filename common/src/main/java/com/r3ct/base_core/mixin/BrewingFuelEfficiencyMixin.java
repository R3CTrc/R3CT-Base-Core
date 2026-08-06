package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingFuelEfficiencyMixin {

    @Shadow private int brewTime;
    @Shadow private int fuel;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void onServerTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity entity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            BrewingFuelEfficiencyMixin mixin = (BrewingFuelEfficiencyMixin) (Object) entity;

            if (mixin.brewTime == 400) {
                if (serverLevel.getRandom().nextFloat() < 0.20f) {
                    if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, pos, "fuel_efficiency")) {
                        mixin.fuel++;
                    }
                }
            }
        }
    }
}