package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class FeatherFallMixin {

    @Inject(method = "calculateFallDamage", at = @At("RETURN"), cancellable = true)
    private void onCalculateFallDamage(double fallDistance, float damageModifier, CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, entity.blockPosition(), "feather_fall")) {
                int originalDamage = cir.getReturnValue();
                if (originalDamage > 0) {
                    int reducedDamage = Math.max(1, (int) (originalDamage * 0.75f));
                    cir.setReturnValue(reducedDamage);
                }
            }
        }
    }
}