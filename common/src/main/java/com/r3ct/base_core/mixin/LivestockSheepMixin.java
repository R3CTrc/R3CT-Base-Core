package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.sheep.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sheep.class)
public abstract class LivestockSheepMixin {

    @Shadow public abstract boolean isSheared();
    @Shadow public abstract void setSheared(boolean sheared);

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;
        if (sheep.level() instanceof ServerLevel serverLevel) {
            if (this.isSheared() && !sheep.isBaby()) {
                if (serverLevel.getRandom().nextInt(4000) == 0) {
                    if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, sheep.blockPosition(), "livestock_boost")) {
                        this.setSheared(false);
                    }
                }
            }
        }
    }
}