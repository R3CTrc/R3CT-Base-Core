package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public class TwinBreedingMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("TAIL"))
    private void onSpawnChildFromBreeding(ServerLevel level, Animal mate, CallbackInfo ci) {
        Animal parent = (Animal) (Object) this;

        if (parent.getRandom().nextFloat() < 0.25f) {

            if (BaseCoreEventLogic.isEffectActiveAt(level, parent.blockPosition(), "twin_breeding")) {

                AgeableMob twin = parent.getBreedOffspring(level, mate);

                if (twin != null) {
                    twin.setBaby(true);

                    twin.snapTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);

                    level.addFreshEntityWithPassengers(twin);
                }
            }
        }
    }
}