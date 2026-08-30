package com.r3ct.base_core.logic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.r3ct.base_core.block.BaseCoreBlockEntity;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector; // WAŻNA ZMIANA
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class BaseCoreClientLogic {

    private static final Set<BaseCoreBlockEntity> TRACKED_CORES = Collections.newSetFromMap(new WeakHashMap<>());
    public static List<AABB> scannerBorders = new ArrayList<>();

    public static boolean clientHasCore = false;
    public static net.minecraft.core.BlockPos clientCorePos = net.minecraft.core.BlockPos.ZERO;
    public static String clientCoreDim = "";

    public static void handleCoreStateSync(com.r3ct.base_core.network.SyncCoreStatePayload payload) {
        clientHasCore = payload.hasCore();
        clientCorePos = payload.pos();
        clientCoreDim = payload.dimension();
    }

    public static java.util.List<com.r3ct.base_core.network.SyncAllCoresPayload.CoreData> allServerCores = new java.util.ArrayList<>();

    public static void handleSyncAllCores(com.r3ct.base_core.network.SyncAllCoresPayload payload) {
        allServerCores = payload.cores();
    }

    public static void trackCore(BaseCoreBlockEntity core) {
        TRACKED_CORES.add(core);
    }

    public static void renderBorders(CameraRenderState cameraState, SubmitNodeCollector storage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cameraPos = cameraState.pos;

        boolean holdsBlueprint = mc.player.getMainHandItem().getItem() instanceof com.r3ct.base_core.item.BlueprintItem ||
                mc.player.getOffhandItem().getItem() instanceof com.r3ct.base_core.item.BlueprintItem;

        PoseStack poseStack = new PoseStack();

        for (BaseCoreBlockEntity core : TRACKED_CORES) {
            if (core.isRemoved() || core.getLevel() != mc.level) continue;

            int radius = BaseCoreServerConfig.calculateRangeUpToTier(core.getTier());
            AABB localAabb = new AABB(-radius, -radius, -radius, 1 + radius, 1 + radius, 1 + radius);

            boolean isOwner = core.getOwnerUUID() != null && core.getOwnerUUID().equals(mc.player.getUUID().toString());

            double ox = core.getBlockPos().getX() - cameraPos.x;
            double oy = core.getBlockPos().getY() - cameraPos.y;
            double oz = core.getBlockPos().getZ() - cameraPos.z;

            if (core.getShowBorder() && isOwner) {
                VoxelShape shape = Shapes.create(localAabb).move(ox, oy, oz);
                storage.submitShapeOutline(poseStack, shape, RenderTypes.lines(), ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F), 2.0F, true);
            } else if (holdsBlueprint) {
                VoxelShape shape = Shapes.create(localAabb).move(ox, oy, oz);
                storage.submitShapeOutline(poseStack, shape, RenderTypes.lines(), ARGB.colorFromFloat(1.0F, 0.0F, 0.5F, 1.0F), 2.0F, true);
            }

            if (holdsBlueprint) {
                int maxRadius = BaseCoreServerConfig.calculateRangeUpToTier(11);
                AABB maxAabb = new AABB(-maxRadius, -maxRadius, -maxRadius, 1 + maxRadius, 1 + maxRadius, 1 + maxRadius);
                VoxelShape maxShape = Shapes.create(maxAabb).move(ox, oy, oz);
                storage.submitShapeOutline(poseStack, maxShape, RenderTypes.lines(), ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.5F), 2.0F, true);
            }
        }

        for (AABB scannerBox : scannerBorders) {
            VoxelShape shape = Shapes.create(scannerBox).move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            storage.submitShapeOutline(poseStack, shape, RenderTypes.lines(), ARGB.colorFromFloat(1.0F, 0.0F, 0.5F, 1.0F), 2.0F, true);
        }
    }
}