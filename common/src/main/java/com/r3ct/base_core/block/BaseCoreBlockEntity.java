package com.r3ct.base_core.block;

import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public class BaseCoreBlockEntity extends BlockEntity {

    private String ownerUUID = "";
    private int tickCounter = 0;

    public BaseCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setOwnerUUID(String uuid) {
        this.ownerUUID = uuid;
        this.setChanged();
        syncToClient();
    }

    public String getOwnerUUID() {
        return this.ownerUUID;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BaseCoreBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.tickCounter++;

        if (entity.tickCounter % 20 != 0) return;

        if (entity.ownerUUID == null || entity.ownerUUID.isEmpty()) return;

        UUID ownerId;
        try {
            ownerId = UUID.fromString(entity.ownerUUID);
        } catch (Exception e) { return; }

        PlayerData data = ModState.getPlayerData(level.getServer(), ownerId);

        if (!data.hasPlacedCore || data.coreX != pos.getX() || data.coreY != pos.getY() || data.coreZ != pos.getZ()) {
            return;
        }

        int radius = BaseCoreServerConfig.calculateRangeUpToTier(data.baseCoreTier);
        AABB boundingBox = new AABB(pos).inflate(radius);

        List<ServerPlayer> playersInRange = level.getEntitiesOfClass(ServerPlayer.class, boundingBox);
        if (playersInRange.isEmpty()) return;

        for (String effectId : data.activeSlots) {
            if (effectId.equals("empty")) continue;

            switch (effectId) {
                case "fire_immunity":
                    applyAuraToPlayers(playersInRange, MobEffects.FIRE_RESISTANCE, 220);
                    break;
                case "night_vision":
                    applyAuraToPlayers(playersInRange, MobEffects.NIGHT_VISION, 220);
                    break;
                case "slow_falling":
                    applyAuraToPlayers(playersInRange, MobEffects.SLOW_FALLING, 220);
                    break;
                case "satiated":
                    for (ServerPlayer player : playersInRange) {
                        float currentSat = player.getFoodData().getSaturationLevel();
                        if (currentSat < 1.0f) {
                            player.getFoodData().setSaturation(Math.min(1.0f, currentSat + 0.05f));
                        }
                    }
                    break;
                case "growth_aura":
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double volume = Math.pow((radius * 2) + 1, 3);

                        int attempts = (int) (volume / 273);

                        for (int i = 0; i < attempts; i++) {
                            int rx = pos.getX() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            int ry = pos.getY() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            int rz = pos.getZ() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            BlockPos targetPos = new BlockPos(rx, ry, rz);

                            BlockState targetState = level.getBlockState(targetPos);

                            if (targetState.isRandomlyTicking()) {
                                targetState.randomTick(serverLevel, targetPos, level.getRandom());

                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                        rx + 0.5, ry + 0.5, rz + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
                            }
                        }
                    }
                    break;
                case "mending_pulse":
                    if (entity.tickCounter % 100 == 0) {
                        applyMendingPulse(level, playersInRange);
                    }
                    break;
            }
        }
    }

    private static void applyAuraToPlayers(List<ServerPlayer> players, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, true, false, true));
        }
    }

    private static void applyMendingPulse(Level level, List<ServerPlayer> players) {
        var enchRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var mendingEnch = enchRegistry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);

        for (ServerPlayer player : players) {
            List<ItemStack> repairableItems = new java.util.ArrayList<>();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);

                if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {

                    if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(mendingEnch, stack) > 0) {
                        repairableItems.add(stack);
                    }
                }
            }

            if (!repairableItems.isEmpty()) {
                ItemStack itemToRepair = repairableItems.get(level.getRandom().nextInt(repairableItems.size()));

                int currentDamage = itemToRepair.getDamageValue();
                int newDamage = Math.max(0, currentDamage - 5);
                itemToRepair.setDamageValue(newDamage);
            }
        }
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.ownerUUID != null && !this.ownerUUID.isEmpty()) {
            output.putString("OwnerUUID", this.ownerUUID);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("OwnerUUID").ifPresent(uuid -> this.ownerUUID = uuid);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);

        CustomData customData = input.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            String owner = tag.getString("OwnerUUID").orElse("");
            if (!owner.isEmpty()) {
                this.ownerUUID = owner;
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);

        if (this.ownerUUID != null && !this.ownerUUID.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("OwnerUUID", this.ownerUUID);
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}