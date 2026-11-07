package nl.gstweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
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

    static GSTweaksConfig get() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final GSTweaksConfig INSTANCE = load();
    }

    private static GSTweaksConfig load() {
        var path = FMLPaths.CONFIGDIR.get().resolve("gstweaks/gstweaks.json");
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
}
