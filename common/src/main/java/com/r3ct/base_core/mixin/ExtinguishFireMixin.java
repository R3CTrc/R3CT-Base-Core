package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ExtinguishFireMixin extends Entity {

    public ExtinguishFireMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    private boolean baseCore$isTamedPet() {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame();
        }
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed();
        }
        if (entity instanceof net.minecraft.world.entity.animal.happyghast.HappyGhast) {
            return true;
        }
        return false;
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    private void onBaseTick(CallbackInfo ci) {
        if (!this.level().isClientSide() && this.isOnFire() && this.level() instanceof ServerLevel serverLevel) {
            LivingEntity entity = (LivingEntity) (Object) this;

            if (entity instanceof Player) {
                if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, entity.blockPosition(), "fire_immunity")) {
                    this.clearFire();
                    return;
                }
            }

            if (this.baseCore$isTamedPet()) {
                if (BaseCoreEventLogic.isEffectActiveAt(serverLevel, entity.blockPosition(), "pet_protection")) {
                    this.clearFire();
                }
            }
        }
    }
}