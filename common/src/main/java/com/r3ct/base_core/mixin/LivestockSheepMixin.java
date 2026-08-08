package com.r3ct.base_core.mixin;

import com.r3ct.base_core.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
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
        if (!sheep.level().isClientSide() && this.isSheared() && !sheep.isBaby()) {
            if (sheep.getRandom().nextInt(4000) == 0) {
                if (sheep.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.LIVESTOCK_BOOST))) {
                    this.setSheared(false);
                }
            }
        }
    }
}