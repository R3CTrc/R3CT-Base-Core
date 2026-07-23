package com.r3ct.base_core.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class BaseCoreClientLogic {

    private static final Set<BaseCoreBlockEntity> TRACKED_CORES = Collections.newSetFromMap(new WeakHashMap<>());
    public static List<AABB> scannerBorders = new ArrayList<>();

    public static void trackCore(BaseCoreBlockEntity core) {
        TRACKED_CORES.add(core);
    }

    public static void renderBorders(PoseStack poseStack, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 cameraPos = cameraState.pos;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());

        for (BaseCoreBlockEntity core : TRACKED_CORES) {
            if (core.isRemoved() || core.getLevel() != mc.level) continue;

            if (core.getShowBorder()) {
                int radius = BaseCoreServerConfig.calculateRangeUpToTier(core.getTier());

                AABB localAabb = new AABB(-radius, -radius, -radius, 1 + radius, 1 + radius, 1 + radius);

                ShapeRenderer.renderShape(
                        poseStack,
                        vertexConsumer,
                        Shapes.create(localAabb),
                        core.getBlockPos().getX() - cameraPos.x,
                        core.getBlockPos().getY() - cameraPos.y,
                        core.getBlockPos().getZ() - cameraPos.z,
                        ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F),
                        2.0F
                );
            }
        }

        for (AABB scannerBox : scannerBorders) {
            ShapeRenderer.renderShape(
                    poseStack,
                    vertexConsumer,
                    Shapes.create(scannerBox),
                    -cameraPos.x, -cameraPos.y, -cameraPos.z,
                    ARGB.colorFromFloat(1.0F, 0.0F, 0.5F, 1.0F),
                    2.0F
            );
        }

        bufferSource.endBatch(RenderTypes.lines());
    }
}