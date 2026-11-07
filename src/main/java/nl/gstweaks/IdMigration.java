package nl.gstweaks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.IOException;
import java.nio.file.Files;

final class IdMigration {
    static void registerAliases() {
        var path = FMLPaths.CONFIGDIR.get().resolve("gstweaks-id-aliases.json");
        if (Files.notExists(path)) {
            return;
        }
        try (var reader = Files.newBufferedReader(path)) {
            JsonObject aliases = JsonParser.parseReader(reader).getAsJsonObject();
            register(aliases.getAsJsonObject("items"), GSTweaks.ITEMS);
            register(aliases.getAsJsonObject("blocks"), GSTweaks.BLOCKS);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load registry aliases from " + path, exception);
        }
    }

    private static void register(JsonObject aliases, DeferredRegister<?> registry) {
        if (aliases == null) {
            return;
        }
        for (var entry : aliases.entrySet()) {
            var oldId = Identifier.parse(entry.getKey());
            var newId = Identifier.parse(entry.getValue().getAsString());
            if (oldId.equals(newId)) {
                continue;
            }
            if (registry.getEntries().stream().noneMatch(holder -> holder.getId().equals(newId))
                    || registry.getEntries().stream().anyMatch(holder -> holder.getId().equals(oldId))) {
                throw new IllegalArgumentException("Invalid GSTweaks registry alias: " + oldId + " -> " + newId);
            }
            registry.addAlias(oldId, newId);
        }
    }

    private IdMigration() {}
}
