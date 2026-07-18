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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BaseCoreClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDir().resolve("r3ct_base_core/r3ct_base_core_client.json");
    private static final File CONFIG_FILE = CONFIG_PATH.toFile();

    private static final int CONFIG_VERSION = 1;

    public int version = CONFIG_VERSION;

    public float guiScale = 1.0f;
    public boolean showAreaParticles = true;
    public int particleDensity = 100;

    private static BaseCoreClientConfig instance = new BaseCoreClientConfig();

    public static BaseCoreClientConfig getInstance() {
        return instance;
    }

    private static void copyDefaultConfig() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (InputStream is = BaseCoreClientConfig.class.getResourceAsStream("/assets/r3ct_base_core/configs/r3ct_base_core_client.json")) {
                if (is != null) {
                    Files.copy(is, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    save();
                }
            }
        } catch (IOException e) {
            Constants.LOG.error("Error copying default r3ct_base_core_client.json!", e);
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
                Constants.LOG.info("Outdated client config detected! Backed up to: " + oldName);
                copyDefaultConfig();
            } catch (Exception e) {
                Constants.LOG.error("Failed to migrate client config", e);
            }
        }
    }

    public static void load() {
        checkAndMigrate();

        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                BaseCoreClientConfig loaded = GSON.fromJson(reader, BaseCoreClientConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                Constants.LOG.error("Error loading r3ct_base_core_client.json!", e);
            }
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            Constants.LOG.error("Error saving r3ct_base_core_client.json!", e);
        }
    }
}