package nl.gstweaks;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Objects;

final class KubeJsMigration {
    private record Entry(String path, String original, String previous, String replacement, boolean install) {}
    private record Change(Entry entry, Path target, byte[] replacement, boolean existed) {}

    static void run() {
        var root = FMLPaths.GAMEDIR.get();
        var kube = root.resolve("kubejs");
        if (!GSTweaks.loaded("kubejs") || !Files.isDirectory(kube)) {
            return;
        }
        try {
            var gameDirectory = root.toRealPath();
            if (!kube.toRealPath().equals(gameDirectory.resolve("kubejs"))) {
                throw new IOException("The KubeJS directory points outside the instance: " + kube);
            }
            migrate(gameDirectory, gameDirectory.resolve("kubejs"));
        } catch (IOException exception) {
            throw new IllegalStateException("GSTweaks could not migrate the GSPack KubeJS files. See the cause for details.", exception);
        }
    }

    private static void migrate(Path root, Path kube) throws IOException {
        Entry[] entries;
        try (var reader = new InputStreamReader(resource("/migration/files.json"), StandardCharsets.UTF_8)) {
            entries = new Gson().fromJson(reader, Entry[].class);
        }
        var changes = new ArrayList<Change>();
        var conflicts = new ArrayList<String>();
        boolean recognizedPack = false;
        for (var entry : entries) {
            var target = kube.resolve(entry.path()).normalize();
            if (!target.startsWith(kube) || !existingParent(target).toRealPath().startsWith(kube)) {
                throw new IOException("Invalid migration path: " + entry.path());
            }
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (entry.install()) {
                    changes.add(new Change(entry, target, replacement(entry), false));
                }
                continue;
            }
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || !target.toRealPath().startsWith(kube)) {
                conflicts.add(entry.path());
                continue;
            }
            var hash = hash(Files.readAllBytes(target));
            if (hash.equals(entry.replacement())) {
                recognizedPack = true;
            } else if (hash.equals(entry.original()) || hash.equals(entry.previous())) {
                recognizedPack = true;
                changes.add(new Change(entry, target, replacement(entry), true));
            } else {
                conflicts.add(entry.path());
            }
        }
        if (!recognizedPack) {
            if (!conflicts.isEmpty()) {
                throw new IOException("Found modified GSPack files: " + String.join(", ", conflicts));
            }
            return;
        }
        if (!conflicts.isEmpty()) {
            throw new IOException("Found modified GSPack files: " + String.join(", ", conflicts)
                    + ". Review them before starting GSTweaks. No files have been migrated.");
        }
        if (changes.isEmpty()) {
            return;
        }

        var backupRoot = root.resolve("gstweaks-backup");
        Files.createDirectories(backupRoot);
        if (!backupRoot.toRealPath().equals(backupRoot)) {
            throw new IOException("The backup directory points outside the instance: " + backupRoot);
        }
        var backup = Files.createTempDirectory(backupRoot, "kubejs-");
        var created = new ArrayList<String>();
        for (var change : changes) {
            if (change.existed()) {
                var copy = backup.resolve("kubejs").resolve(change.entry().path());
                Files.createDirectories(copy.getParent());
                Files.copy(change.target(), copy);
            } else {
                created.add(change.entry().path());
            }
        }
        Files.writeString(backup.resolve("created-files.json"), new Gson().toJson(created));

        var processed = new ArrayList<Change>();
        try {
            for (var change : changes) {
                processed.add(change);
                if (change.replacement() == null) {
                    Files.delete(change.target());
                } else {
                    Files.createDirectories(change.target().getParent());
                    Files.write(change.target(), change.replacement());
                }
            }
        } catch (IOException exception) {
            for (var change : processed) {
                try {
                    if (change.existed()) {
                        Files.copy(backup.resolve("kubejs").resolve(change.entry().path()), change.target(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(change.target());
                    }
                } catch (IOException rollbackError) {
                    exception.addSuppressed(rollbackError);
                }
            }
            throw exception;
        }
        LogUtils.getLogger().info("GSTweaks migrated {} GSPack KubeJS files. Originals: {}", changes.size(), backup);
    }

    private static byte[] replacement(Entry entry) throws IOException {
        if (entry.replacement() == null) {
            return null;
        }
        try (var input = resource("/migration/kubejs/" + entry.path())) {
            var bytes = input.readAllBytes();
            if (!hash(bytes).equals(entry.replacement())) {
                throw new IOException("Invalid GSTweaks migration resource: " + entry.path());
            }
            return bytes;
        }
    }

    private static Path existingParent(Path path) {
        var parent = path.getParent();
        while (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
            parent = parent.getParent();
        }
        return parent;
    }

    private static InputStream resource(String path) {
        return Objects.requireNonNull(KubeJsMigration.class.getResourceAsStream(path), path);
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private KubeJsMigration() {}
}
