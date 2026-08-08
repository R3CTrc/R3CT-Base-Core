package com.r3ct.base_core.mixin;

import com.r3ct.base_core.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableMob.class)
public abstract class LivestockAgeMixin {

    @Shadow public abstract int getAge();
    @Shadow public abstract void setAge(int age);
    @Shadow public abstract boolean canAgeUp();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci) {
        AgeableMob entity = (AgeableMob) (Object) this;

        if (!entity.level().isClientSide() && this.canAgeUp() && entity.tickCount % 4 == 0) {
            if (entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.LIVESTOCK_BOOST))) {
                this.setAge(this.getAge() + 1);
            }
        }
    }
}