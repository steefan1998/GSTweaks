package nl.gstweaks.daily;

import com.google.gson.GsonBuilder;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.List;

final class DailyConfig {
    record Reward(String item, int count) {}

    boolean enabled = true;
    String timeZone = "Europe/Amsterdam";
    int resetHour = 0;
    int minimumOnlineMinutes = 30;
    int experience = 30;
    List<Reward> rewards = List.of(new Reward("minecraft:gold_ingot", 2), new Reward("minecraft:bread", 4));

    static DailyConfig load() {
        var path = FMLPaths.CONFIGDIR.get().resolve("gstweaks/daily-rewards.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(new DailyConfig()) + System.lineSeparator(),
                        StandardOpenOption.CREATE_NEW);
            }
            try (var reader = Files.newBufferedReader(path)) {
                var config = gson.fromJson(reader, DailyConfig.class);
                config.zone();
                if (config.resetHour < 0 || config.resetHour > 23 || config.minimumOnlineMinutes < 0
                        || config.minimumOnlineMinutes > 1440 || config.experience < 0 || config.rewards.size() > 64) {
                    throw new IllegalArgumentException("Daily reward timing or reward counts are out of range");
                }
                for (var reward : config.rewards) {
                    Identifier.parse(reward.item());
                    if (reward.count() < 1 || reward.count() > 64) {
                        throw new IllegalArgumentException("Reward count must be 1 to 64: " + reward.item());
                    }
                }
                return config;
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Invalid Daily Rewards configuration at " + path, exception);
        }
    }

    ZoneId zone() {
        return ZoneId.of(timeZone);
    }
}
