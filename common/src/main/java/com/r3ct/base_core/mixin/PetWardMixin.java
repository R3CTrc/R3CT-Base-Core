package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class PetWardMixin {

    @Unique
    private boolean baseCore$isTamedPet() {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            return tamable.isTame();
        }
        if (entity instanceof net.minecraft.world.entity.animal.equine.AbstractHorse horse) {
            return horse.isTamed();
        }
        if (entity instanceof net.minecraft.world.entity.animal.happyghast.HappyGhast) {
            return true;
        }
        return false;
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float reducePetDamage(float damage) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (this.baseCore$isTamedPet() && damage > 0.0F) {
                if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, entity.blockPosition(), "pet_ward")) {
                    return damage * 0.75F;
                }
            }
        }
        return damage;
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServerPetImmunity(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (this.baseCore$isTamedPet()) {
            if (BaseCoreEventLogic.isEffectActiveAt(level, entity.blockPosition(), "pet_ward")) {
                if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}