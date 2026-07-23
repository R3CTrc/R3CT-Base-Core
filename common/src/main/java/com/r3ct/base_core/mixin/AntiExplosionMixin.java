package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerExplosion.class)
public class AntiExplosionMixin {

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void onCalculateExplodedPositions(CallbackInfoReturnable<List<BlockPos>> cir) {
        ServerExplosion explosion = (ServerExplosion) (Object) this;
        ServerLevel level = explosion.level();

        List<BlockPos> originalList = cir.getReturnValue();
        if (originalList == null || originalList.isEmpty()) {
            return;
        }

        List<BlockPos> filteredList = new ArrayList<>();

        for (BlockPos pos : originalList) {
            if (!BaseCoreEventLogic.shouldCancelExplosion(level, pos)) {
                filteredList.add(pos);
            }
        }

        if (filteredList.size() != originalList.size()) {
            cir.setReturnValue(filteredList);
        }
    }
}