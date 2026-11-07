package nl.gstweaks;

import com.google.gson.Gson;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record UnificationConfig(boolean enabled, boolean overrideOreData, List<Group> groups,
                         Map<String, String> blockReplacements, List<String> hiddenItems, List<String> hiddenOres) {
    record Group(String tag, List<String> items, boolean convertBlocks) {}

    static UnificationConfig get() {
        return ConfigHolder.INSTANCE;
    }

    private static class ConfigHolder {
        private static final UnificationConfig INSTANCE = load();
    }

    private static UnificationConfig load() {
        var path = FMLPaths.CONFIGDIR.get().resolve("gstweaks/unification.json");
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                try (var defaults = Objects.requireNonNull(UnificationConfig.class.getResourceAsStream("/default-unification.json"))) {
                    Files.copy(defaults, path);
                }
            }
            try (var reader = Files.newBufferedReader(path)) {
                var config = new Gson().fromJson(reader, UnificationConfig.class);
                config.validate();
                return config;
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Invalid GSTweaks unification configuration at " + path, exception);
        }
    }

    private void validate() {
        var members = new HashSet<Identifier>();
        for (Group group : groups) {
            Identifier.parse(group.tag());
            if (group.items().size() < 2) {
                throw new IllegalArgumentException("Unification group needs at least two items: " + group.tag());
            }
            for (String item : group.items()) {
                if (!members.add(Identifier.parse(item))) {
                    throw new IllegalArgumentException("Item appears more than once: " + item);
                }
            }
        }
        blockReplacements.forEach((from, to) -> {
            Identifier.parse(from);
            Identifier.parse(to);
        });
        hiddenItems.forEach(Identifier::parse);
        hiddenOres.forEach(Identifier::parse);
    }
}
