package com.r3ct.base_core.mixin;

import com.r3ct.base_core.logic.BaseCoreEventLogic;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
public class BuildersReachMixin {

    @Unique
    private static final Identifier BASE_CORE_REACH_ID = Identifier.parse("r3ct_base_core:builders_reach");

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel serverLevel = (ServerLevel) player.level();
        AttributeInstance reachAttribute = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reachAttribute != null) {
            boolean inBase = BaseCoreEventLogic.isEffectActiveAt(serverLevel, player.blockPosition(), "builders_reach");
            boolean hasModifier = reachAttribute.getModifier(BASE_CORE_REACH_ID) != null;
            if (inBase && !hasModifier) {
                reachAttribute.addTransientModifier(new AttributeModifier(BASE_CORE_REACH_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!inBase && hasModifier) {
                reachAttribute.removeModifier(BASE_CORE_REACH_ID);
            }
        }
    }
}