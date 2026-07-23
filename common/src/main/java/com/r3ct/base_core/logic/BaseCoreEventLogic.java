package com.r3ct.base_core.logic;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;

public class BaseCoreEventLogic {

    public static boolean shouldCancelExplosion(ServerLevel level, BlockPos explosionPos) {
        return isEffectActiveAt(level, explosionPos, "anti_explosion");
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

            if (data.coreTier == 0) continue;

            if (data.activeSlots == null || !data.activeSlots.contains(effectId)) continue;

            int radius = BaseCoreServerConfig.calculateRangeUpToTier(data.coreTier);

            if (Math.abs(eventPos.getX() - data.coreX) <= radius &&
                    Math.abs(eventPos.getY() - data.coreY) <= radius &&
                    Math.abs(eventPos.getZ() - data.coreZ) <= radius) {

                return true;
            }
        }

        return false;
    }
}