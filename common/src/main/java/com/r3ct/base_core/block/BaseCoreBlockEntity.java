package com.r3ct.base_core.block;

import com.r3ct.base_core.client.screen.BaseCoreMenu;
import com.r3ct.base_core.config.BaseCoreServerConfig;
import com.r3ct.base_core.data.ModState;
import com.r3ct.base_core.data.PlayerData;
import com.r3ct.base_core.logic.BaseCoreClientLogic;
import com.r3ct.base_core.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseCoreBlockEntity extends BlockEntity implements Container, MenuProvider {

    private String ownerUUID = "";
    private int tickCounter = 0;
    private int tier = 0;
    private boolean showBorder = false;

    private final NonNullList<ItemStack> items = NonNullList.withSize(16, ItemStack.EMPTY);

    protected final net.minecraft.world.inventory.ContainerData dataAccess = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BaseCoreBlockEntity.this.tier;
                case 1 -> BaseCoreBlockEntity.this.showBorder ? 1 : 0;
                case 2 -> BaseCoreBlockEntity.this.getBlockPos().getX();
                case 3 -> BaseCoreBlockEntity.this.getBlockPos().getY();
                case 4 -> BaseCoreBlockEntity.this.getBlockPos().getZ();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) BaseCoreBlockEntity.this.tier = value;
            else if (index == 1) BaseCoreBlockEntity.this.showBorder = (value != 0);
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public BaseCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setOwnerUUID(String uuid) {
        this.ownerUUID = uuid;
        this.setChanged();
        syncToClient();
    }

    public String getOwnerUUID() { return this.ownerUUID; }
    public int getTier() { return this.tier; }

    public void setTier(int tier) {
        this.tier = tier;
        this.setChanged();
        syncToClient();
    }

    public boolean getShowBorder() { return this.showBorder; }

    public void toggleShowBorder() {
        this.showBorder = !this.showBorder;
        this.setChanged();
        syncToClient();
    }

    public void forceSync() {
        this.setChanged();
        syncToClient();
    }

    public List<String> getActiveEffectsFromTomes() {
        List<String> effects = new ArrayList<>();
        int maxSlots = BaseCoreServerConfig.calculateTotalSlots(this.tier);

        for (int i = 0; i < maxSlots; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.has(ModDataComponents.EFFECT_ID)) {
                String effectId = stack.get(ModDataComponents.EFFECT_ID);
                if (effectId != null && !effectId.isEmpty()) {
                    effects.add(effectId);
                }
            }
        }
        return effects;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BaseCoreBlockEntity entity) {
        if (level.isClientSide()) {
            BaseCoreClientLogic.trackCore(entity);
            return;
        }

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

        int radius = BaseCoreServerConfig.calculateRangeUpToTier(entity.tier);
        AABB boundingBox = new AABB(pos).inflate(radius);
        List<ServerPlayer> playersInRange = level.getEntitiesOfClass(ServerPlayer.class, boundingBox);

        List<String> activeEffects = entity.getActiveEffectsFromTomes();

        for (String effectId : activeEffects) {
            switch (effectId) {
                case "pvp_protection":
                    if (!playersInRange.isEmpty()) applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.PVP_PROTECTION);
                    break;
                case "fall_resistance":
                    if (!playersInRange.isEmpty()) applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.FALL_RESISTANCE);
                    break;
                case "extended_reach":
                    if (!playersInRange.isEmpty()) applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.EXTENDED_REACH);
                    break;
                case "fire_immunity":
                    if (!playersInRange.isEmpty()) {
                        applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.FIRE_IMMUNITY);
                        applyAuraToPlayers(playersInRange, MobEffects.FIRE_RESISTANCE, 240);
                    }
                    break;
                case "night_vision":
                    if (!playersInRange.isEmpty()) {
                        applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.NIGHT_VISION);
                        applyAuraToPlayers(playersInRange, MobEffects.NIGHT_VISION, 240);
                    }
                    break;
                case "satiation":
                    if (!playersInRange.isEmpty()) {
                        applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.SATIATION);
                        for (ServerPlayer player : playersInRange) {
                            float currentSat = player.getFoodData().getSaturationLevel();
                            if (currentSat < 1.0f) {
                                player.getFoodData().setSaturation(Math.min(1.0f, currentSat + 0.05f));
                            }
                        }
                    }
                    break;
                case "mending_pulse":
                    if (!playersInRange.isEmpty()) {
                        applyFakeEffectToPlayers(playersInRange, com.r3ct.base_core.registry.ModEffects.MENDING_PULSE);
                        if (entity.tickCounter % 100 == 0) {
                            applyMendingPulse(level, playersInRange);
                        }
                    }
                    break;

                case "hostile_slowness":
                    List<net.minecraft.world.entity.monster.Monster> monsters = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, boundingBox);
                    if (!monsters.isEmpty()) {
                        for (net.minecraft.world.entity.monster.Monster monster : monsters) {
                            monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 240, 1, true, false, true));
                        }
                    }
                    break;
                case "crop_growth":
                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        double volume = Math.pow((radius * 2) + 1, 3);
                        int attempts = (int) (volume / 273);

                        for (int i = 0; i < attempts; i++) {
                            int rx = pos.getX() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            int ry = pos.getY() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            int rz = pos.getZ() + level.getRandom().nextInt(radius * 2 + 1) - radius;
                            BlockPos targetPos = new BlockPos(rx, ry, rz);
                            BlockState targetState = level.getBlockState(targetPos);

                            if (targetState.isRandomlyTicking() && isCrop(targetState)) {
                                targetState.randomTick(serverLevel, targetPos, level.getRandom());
                                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                        rx + 0.5, ry + 0.5, rz + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private static boolean isCrop(BlockState state) {
        var block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.CropBlock
                || block instanceof net.minecraft.world.level.block.StemBlock
                || block instanceof net.minecraft.world.level.block.SweetBerryBushBlock
                || block instanceof net.minecraft.world.level.block.SaplingBlock
                || block instanceof net.minecraft.world.level.block.SugarCaneBlock
                || block instanceof net.minecraft.world.level.block.CactusBlock
                || block instanceof net.minecraft.world.level.block.BambooStalkBlock
                || block instanceof net.minecraft.world.level.block.BambooSaplingBlock
                || block instanceof net.minecraft.world.level.block.PitcherCropBlock
                || block instanceof net.minecraft.world.level.block.NetherWartBlock
                || block instanceof net.minecraft.world.level.block.CocoaBlock
                || block instanceof net.minecraft.world.level.block.ChorusFlowerBlock
                || block instanceof net.minecraft.world.level.block.GrowingPlantHeadBlock;
    }

    private static void applyAuraToPlayers(List<ServerPlayer> players, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration) {
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(effect, duration, 0, false, false, false));
        }
    }

    private static void applyFakeEffectToPlayers(List<ServerPlayer> players, net.minecraft.world.effect.MobEffect effect) {
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> registeredHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        for (ServerPlayer player : players) {
            player.addEffect(new MobEffectInstance(registeredHolder, 240, 0, false, false, true));
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
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) this.setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.r3ct_base_core.base_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BaseCoreMenu(containerId, playerInventory, this, this.dataAccess);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.ownerUUID != null && !this.ownerUUID.isEmpty()) {
            output.putString("OwnerUUID", this.ownerUUID);
        }
        output.putInt("baseCoreTier", this.tier);
        output.putBoolean("showBorder", this.showBorder);

        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getString("OwnerUUID").ifPresent(uuid -> this.ownerUUID = uuid);
        input.getInt("baseCoreTier").ifPresent(t -> this.tier = t);
        this.showBorder = input.getBooleanOr("showBorder", false);

        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);

        CustomData customData = input.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            tag.getString("OwnerUUID").ifPresent(owner -> {
                if (!owner.isEmpty()) this.ownerUUID = owner;
            });
            tag.getInt("baseCoreTier").ifPresent(t -> this.tier = t);
            tag.getBoolean("showBorder").ifPresent(b -> this.showBorder = b);
        }

        ItemContainerContents contents = input.get(DataComponents.CONTAINER);
        if (contents != null) {
            contents.copyInto(this.items);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);

        CompoundTag tag = new CompoundTag();
        if (this.ownerUUID != null && !this.ownerUUID.isEmpty()) {
            tag.putString("OwnerUUID", this.ownerUUID);
        }
        tag.putInt("baseCoreTier", this.tier);
        tag.putBoolean("showBorder", this.showBorder);

        if (!tag.isEmpty()) {
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }

        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
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