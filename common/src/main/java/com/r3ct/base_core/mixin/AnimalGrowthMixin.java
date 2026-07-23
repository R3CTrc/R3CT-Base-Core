package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableMob.class)
public abstract class AnimalGrowthMixin {

    @Shadow public abstract int getAge();
    @Shadow public abstract void setAge(int age);
    @Shadow public abstract boolean canAgeUp();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        AgeableMob entity = (AgeableMob) (Object) this;
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (this.canAgeUp()) {
                if (serverLevel.getGameTime() % 4 == 0) {
                    if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, entity.blockPosition(), "animal_growth")) {
                        int currentAge = this.getAge();
                        this.setAge(currentAge + 1);
                    }
                }
            }
        }
    }
}