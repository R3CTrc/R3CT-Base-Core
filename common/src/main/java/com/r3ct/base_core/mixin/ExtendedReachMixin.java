package com.r3ct.base_core.mixin;

import com.r3ct.base_core.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ExtendedReachMixin {

    @Unique
    private static final Identifier BASE_CORE_REACH_ID = Identifier.parse("r3ct_base_core:extended_reach");

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.tickCount % 20 != 0 || player.isSpectator()) {
            return;
        }

        AttributeInstance reachAttribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reachAttribute != null) {
            boolean hasAura = player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.EXTENDED_REACH));
            boolean hasModifier = reachAttribute.getModifier(BASE_CORE_REACH_ID) != null;

            if (hasAura && !hasModifier) {
                reachAttribute.addTransientModifier(new AttributeModifier(BASE_CORE_REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!hasAura && hasModifier) {
                reachAttribute.removeModifier(BASE_CORE_REACH_ID);
            }
        }
    }
}