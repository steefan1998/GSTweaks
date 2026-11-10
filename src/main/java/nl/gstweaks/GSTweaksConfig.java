package nl.gstweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

final class GSTweaksConfig {
    boolean autoMigrateKubeJs = true;
    boolean endgameTooltips = true;
    boolean migrateMarsOres = true;
    boolean marsZombieSuit = true;
    boolean marsRaidPillagers = true;
    boolean marsVillageLifeSupport = true;
    boolean thaumaturgeCompat = true;
    boolean tinkersOreRecipes = true;
    boolean tinkersToolMaterials = true;
    boolean unifyOil = true;
    volatile boolean woodenToolsOnly = false;

    static GSTweaksConfig get() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final GSTweaksConfig INSTANCE = load();
    }

    private static GSTweaksConfig load() {
        var path = path();
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(new GSTweaksConfig()) + System.lineSeparator(),
                        StandardOpenOption.CREATE_NEW);
            }
            try (var reader = Files.newBufferedReader(path)) {
                var config = new Gson().fromJson(reader, GSTweaksConfig.class);
                if (config == null) {
                    throw new IllegalArgumentException("Expected a JSON object");
                }
                return config;
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Invalid GSTweaks configuration at " + path, exception);
        }
    }

    void setWoodenToolsOnly(boolean enabled) throws IOException {
        var path = path();
        var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        var gson = new GsonBuilder().setPrettyPrinting().create();
        json.addProperty("woodenToolsOnly", enabled);
        var temporary = Files.createTempFile(path.getParent(), "gstweaks-", ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(json) + System.lineSeparator());
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            woodenToolsOnly = enabled;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("gstweaks/gstweaks.json");
    }
}
