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
public class BrewingOverclockMixin {

    @Shadow private int brewTime;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(Level level, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            BrewingOverclockMixin mixinBE = (BrewingOverclockMixin) (Object) blockEntity;
            if (mixinBE.brewTime > 1 && serverLevel.getGameTime() % 4 == 0) {
                if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, pos, "industrial_overclock")) {
                    mixinBE.brewTime--;
                }
            }
        }
    }
}