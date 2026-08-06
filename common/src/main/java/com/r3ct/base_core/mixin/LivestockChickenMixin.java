package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public class LivestockChickenMixin {

    @Shadow public int eggTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (chicken.level() instanceof ServerLevel serverLevel && !chicken.isBaby()) {
            if (serverLevel.getGameTime() % 4 == 0) {
                if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, chicken.blockPosition(), "livestock_boost")) {
                    if (this.eggTime > 1) {
                        this.eggTime--;
                    }
                }
            }
        }
    }
}