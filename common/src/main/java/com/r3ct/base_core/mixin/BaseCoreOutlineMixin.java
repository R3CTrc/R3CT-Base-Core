package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreClientLogic;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class BaseCoreOutlineMixin {

    // Wpinamy się na sam koniec zbierania linii (kształtów bloków) do głównego węzła gry
    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void r3ct_injectBaseCoreBorders(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, boolean renderOutline, CallbackInfo ci) {
        // Przekazujemy stan kamery i silnik zbierający prosto do Twojej logiki
        BaseCoreClientLogic.renderBorders(levelRenderState.cameraRenderState, submitNodeCollector);
    }
}