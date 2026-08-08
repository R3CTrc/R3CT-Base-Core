package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreClientLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

    @Inject(method = "extract", at = @At("RETURN"))
    private void onExtract(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player != null && BaseCoreClientLogic.hasNightVisionAura(player)) {
            renderState.nightVisionEffectIntensity = 1.0F;
        }
    }
}