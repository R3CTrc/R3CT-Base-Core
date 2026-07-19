package com.r3ct.base_core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.r3ct.base_core.platform.Services;
import com.r3ct.base_core.Constants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class BaseCoreServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDir().resolve("r3ct_base_core/r3ct_base_core_server.json");
    private static final File CONFIG_FILE = CONFIG_PATH.toFile();
    private static final int CONFIG_VERSION = 1;

    public static class TierUpgrade {
        public int tierLevel;
        public String title;
        public String mainItem;
        public int mainAmount;
        public String bulkItem;
        public int bulkAmount;
        public int bonusRadius;
        public int bonusSlots;
        public int unlocksPool;
    }

    public static class EffectConfig {
        public String id;
        public String name;
        public String description;
        public int xpCost;
        public String itemCost;
        public int itemAmount;
        public int pool;
    }

    public static class EffectSettings {
        public boolean allowBeaconSynergy = true;
    }

    public int version = CONFIG_VERSION;
    public EffectSettings settings = new EffectSettings();
    public List<TierUpgrade> tiers = new ArrayList<>();

    public List<EffectConfig> effects = new ArrayList<>();

    private static BaseCoreServerConfig instance = new BaseCoreServerConfig();

    public static BaseCoreServerConfig getInstance() {
        return instance;
    }

    private static void copyDefaultConfig() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (InputStream is = BaseCoreServerConfig.class.getResourceAsStream("/assets/r3ct_base_core/configs/r3ct_base_core_server.json")) {
                if (is != null) {
                    Files.copy(is, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    save();
                }
            }
        } catch (IOException e) {
            Constants.LOG.error("Error copying default r3ct_base_core_server.json!", e);
        }
    }

    private static void checkAndMigrate() {
        if (!CONFIG_FILE.exists()) {
            copyDefaultConfig();
            return;
        }

        boolean needsUpdate = false;
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int version = json.has("version") ? json.get("version").getAsInt() : 0;
            if (version < CONFIG_VERSION) {
                needsUpdate = true;
            }
        } catch (Exception e) {
            needsUpdate = true;
        }

        if (needsUpdate) {
            try {
                String oldName = CONFIG_PATH.getFileName().toString().replace(".json", "_OLD.json");
                Path backupPath = CONFIG_PATH.resolveSibling(oldName);
                Files.move(CONFIG_PATH, backupPath, StandardCopyOption.REPLACE_EXISTING);
                Constants.LOG.info("Outdated server config detected! Backed up to: " + oldName);
                copyDefaultConfig();
            } catch (Exception e) {
                Constants.LOG.error("Failed to migrate server config", e);
            }
        }
    }

    public static void load() {
        checkAndMigrate();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                BaseCoreServerConfig loaded = GSON.fromJson(reader, BaseCoreServerConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                Constants.LOG.error("Error loading r3ct_base_core_server.json!", e);
            }
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            Constants.LOG.error("Error saving r3ct_base_core_server.json!", e);
        }
    }

    public static TierUpgrade getTier(int level) {
        return instance.tiers.stream().filter(t -> t.tierLevel == level).findFirst().orElse(null);
    }

    public static EffectConfig getEffect(String effectId) {
        return instance.effects.stream().filter(e -> e.id.equals(effectId)).findFirst().orElse(null);
    }

    public static int calculateTotalSlots(int currentTier) {
        int totalSlots = 0;
        for (int i = 1; i <= currentTier; i++) {
            TierUpgrade tier = getTier(i);
            if (tier != null) {
                totalSlots += tier.bonusSlots;
            }
        }
        return totalSlots;
    }

    public static String getServerConfigString() {
        if (!CONFIG_FILE.exists()) return "{}";
        try {
            return new String(Files.readAllBytes(CONFIG_FILE.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Constants.LOG.error("Failed to read server config file", e);
            return "{}";
        }
    }

    public static void syncFromServer(String serverJson) {
        try {
            BaseCoreServerConfig synced = GSON.fromJson(serverJson, BaseCoreServerConfig.class);
            if (synced != null) {
                instance = synced;
                Constants.LOG.info("Successfully synced Base Core config from Server RAM!");
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse synced config from server!", e);
        }
    }
}