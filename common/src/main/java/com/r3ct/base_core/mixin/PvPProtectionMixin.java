package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PvPProtectionMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player attacker = (Player) (Object) this;

        if (attacker.level() instanceof ServerLevel serverLevel && target instanceof Player targetPlayer) {
            if (BaseCoreEventLogic.shouldCancelPvP(serverLevel, attacker, targetPlayer)) {
                ci.cancel();
            }
        }
    }
}