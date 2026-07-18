package com.r3ct.base_core.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public int baseCoreTier = 0;

    public List<String> activeEffects = new ArrayList<>();

    public boolean hasPlacedCore = false;
    public String coreDimension = "";
    public int coreX = 0;
    public int coreY = 0;
    public int coreZ = 0;

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("baseCoreTier", baseCoreTier);

        nbt.putBoolean("hasPlacedCore", hasPlacedCore);
        nbt.putString("coreDimension", coreDimension != null ? coreDimension : "");
        nbt.putInt("coreX", coreX);
        nbt.putInt("coreY", coreY);
        nbt.putInt("coreZ", coreZ);

        ListTag effectsList = new ListTag();
        for (String effect : activeEffects) {
            effectsList.add(StringTag.valueOf(effect != null ? effect : ""));
        }
        nbt.put("activeEffects", effectsList);

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        data.baseCoreTier = nbt.getInt("baseCoreTier").orElse(0);

        data.hasPlacedCore = nbt.getBoolean("hasPlacedCore").orElse(false);
        data.coreDimension = nbt.getString("coreDimension").orElse("");
        data.coreX = nbt.getInt("coreX").orElse(0);
        data.coreY = nbt.getInt("coreY").orElse(0);
        data.coreZ = nbt.getInt("coreZ").orElse(0);

        if (nbt.contains("activeEffects")) {
            data.activeEffects.clear();
            if (nbt.get("activeEffects") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.activeEffects.add(list.getString(i).orElse(""));
                }
            }
        }

        return data;
    }
}