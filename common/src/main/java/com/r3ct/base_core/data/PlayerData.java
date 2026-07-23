package com.r3ct.base_core.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerData {
    public boolean hasPlacedCore = false;
    public String coreDimension = "";
    public int coreX = 0;
    public int coreY = 0;
    public int coreZ = 0;

    public int coreTier = 0;
    public List<String> activeSlots = new ArrayList<>();

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putBoolean("hasPlacedCore", hasPlacedCore);
        nbt.putString("coreDimension", coreDimension != null ? coreDimension : "");
        nbt.putInt("coreX", coreX);
        nbt.putInt("coreY", coreY);
        nbt.putInt("coreZ", coreZ);

        nbt.putInt("coreTier", coreTier);
        nbt.putString("activeSlotsStr", String.join(",", activeSlots));

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        data.hasPlacedCore = nbt.getBoolean("hasPlacedCore").orElse(false);
        data.coreDimension = nbt.getString("coreDimension").orElse("");
        data.coreX = nbt.getInt("coreX").orElse(0);
        data.coreY = nbt.getInt("coreY").orElse(0);
        data.coreZ = nbt.getInt("coreZ").orElse(0);

        data.coreTier = nbt.getInt("coreTier").orElse(0);

        String slotsStr = nbt.getString("activeSlotsStr").orElse("");
        if (!slotsStr.isEmpty()) {
            data.activeSlots = new ArrayList<>(Arrays.asList(slotsStr.split(",")));
        }

        return data;
    }
}