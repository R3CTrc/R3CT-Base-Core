package com.r3ct.base_core.logic;

import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BaseCoreEventLogic {

    /**
     * Zwraca TRUE, jeśli wybuch ma zostać ZABLOKOWANY
     */
    public static boolean shouldCancelExplosion(ServerLevel level, BlockPos explosionPos) {
        return isEffectActiveAt(level, explosionPos, "grief_ward");
    }

    public static boolean shouldCancelMobSpawn(ServerLevelAccessor level, EntityType<?> type, EntitySpawnReason reason, BlockPos pos) {

        if (type == net.minecraft.world.entity.EntityType.WITHER ||
                type == net.minecraft.world.entity.EntityType.ENDER_DRAGON ||
                type == net.minecraft.world.entity.EntityType.WARDEN) {
            return false;
        }

        if (reason == net.minecraft.world.entity.EntitySpawnReason.EVENT) {
            return false;
        }

        if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {

            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                return isEffectActiveAt(serverLevel, pos, "anti_spawn");
            }
        }

        return false;
    }

    public static boolean isEffectActiveAt(ServerLevel level, BlockPos eventPos, String effectId) {
        ModState state = ModState.get(level.getServer());
        String currentDimension = level.dimension().identifier().toString();

        for (PlayerData data : state.players.values()) {
            if (!data.hasPlacedCore) continue;
            if (!data.coreDimension.equals(currentDimension)) continue;

            BlockPos corePos = new BlockPos(data.coreX, data.coreY, data.coreZ);

            if (!level.isLoaded(corePos)) continue;

            BlockEntity be = level.getBlockEntity(corePos);
            if (be instanceof BaseCoreBlockEntity coreBE) {

                int radius = BaseCoreServerConfig.calculateRangeUpToTier(coreBE.getTier());

                if (Math.abs(eventPos.getX() - data.coreX) <= radius &&
                        Math.abs(eventPos.getY() - data.coreY) <= radius &&
                        Math.abs(eventPos.getZ() - data.coreZ) <= radius) {

                    if (coreBE.getActiveSlots().contains(effectId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}