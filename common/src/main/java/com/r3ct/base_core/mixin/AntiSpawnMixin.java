package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
public class AntiSpawnMixin {

    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static <T extends net.minecraft.world.entity.Entity> void onCheckSpawnRules(
            EntityType<T> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir) {
        if (spawnReason == EntitySpawnReason.NATURAL ||
                spawnReason == EntitySpawnReason.CHUNK_GENERATION ||
                spawnReason == EntitySpawnReason.STRUCTURE ||
                spawnReason == EntitySpawnReason.PATROL) {
            if (BaseCoreEventLogic.shouldCancelMobSpawn(level, type, spawnReason, pos)) {
                cir.setReturnValue(false);
            }
        }
    }
}