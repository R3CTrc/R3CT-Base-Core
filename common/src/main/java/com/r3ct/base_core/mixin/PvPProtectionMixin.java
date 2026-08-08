package com.r3ct.base_core.mixin;

import com.r3ct.base_core.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PvPProtectionMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity target, CallbackInfo ci) {
        Player attacker = (Player) (Object) this;
        if (target instanceof Player targetPlayer) {
            if (attacker.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PVP_PROTECTION)) ||
                    targetPlayer.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PVP_PROTECTION))) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Player targetPlayer = (Player) (Object) this;
        Entity attacker = source.getEntity();
        if (attacker instanceof Player attackerPlayer && attackerPlayer != targetPlayer) {
            if (attackerPlayer.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PVP_PROTECTION)) ||
                    targetPlayer.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PVP_PROTECTION))) {
                cir.setReturnValue(false);
            }
        }
    }
}