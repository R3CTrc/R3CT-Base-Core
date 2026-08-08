package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreClientLogic;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Redirect(
            method = "computeFogColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z")
    )
    private boolean redirectHasNightVision(LivingEntity instance, Holder<MobEffect> effect) {
        if (effect == MobEffects.NIGHT_VISION && instance instanceof Player player) {
            if (BaseCoreClientLogic.hasNightVisionAura(player)) {
                return true;
            }
        }
        return instance.hasEffect(effect);
    }

    @Redirect(
            method = "computeFogColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F")
    )
    private float redirectNightVisionScale(LivingEntity instance, float partialTicks) {
        if (instance instanceof Player player && BaseCoreClientLogic.hasNightVisionAura(player)) {
            return 1.0F;
        }
        return GameRenderer.getNightVisionScale(instance, partialTicks);
    }
}