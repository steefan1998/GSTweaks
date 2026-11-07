package nl.gstweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

final class BlockMigration {
    static final ResourceKey<Level> MARS =
            ResourceKey.create(Registries.DIMENSION, Identifier.parse("stellaris:mars"));

    private static final Map<String, String> MARS_ORES = Map.of(
            "minecraft:iron_ore", "iron",
            "minecraft:copper_ore", "copper",
            "minecraft:gold_ore", "gold",
            "alltheores:osmium_ore", "osmium",
            "alltheores:uranium_ore", "uranium",
            "allthemodium:unobtainium_ore", "unobtainium");

    private record PendingChunk(ServerLevel level, ChunkPos pos) {}

    private final ConcurrentLinkedQueue<PendingChunk> pendingChunks = new ConcurrentLinkedQueue<>();
    private final boolean marsEnabled = GSTweaks.loaded("stellaris") && GSTweaksConfig.get().migrateMarsOres;
    private final boolean unifyEnabled = UnificationConfig.get().enabled();
    private Map<Block, BlockState> marsReplacements;

    void register() {
        if (marsEnabled || unifyEnabled) {
            var bus = NeoForge.EVENT_BUS;
            bus.addListener(this::onChunkLoad);
            bus.addListener(this::onTick);
            bus.addListener(this::onServerStopped);
        }
    }

    private void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && (unifyEnabled || marsEnabled && MARS.equals(level.dimension()))) {
            pendingChunks.add(new PendingChunk(level, event.getChunk().getPos()));
        }
    }

    private void onTick(ServerTickEvent.Post event) {
        long deadline = System.nanoTime() + 5_000_000;
        PendingChunk pending;
        while (System.nanoTime() < deadline && (pending = pendingChunks.poll()) != null) {
            var level = pending.level();
            if (event.getServer().getLevel(level.dimension()) != level) {
                continue;
            }
            var replacements = marsEnabled && MARS.equals(level.dimension()) ? marsReplacements() : Unification.blocks();
            if (!replacements.isEmpty()) {
                var chunk = level.getChunkSource().getChunk(pending.pos().x(), pending.pos().z(), ChunkStatus.FULL, false);
                if (chunk instanceof LevelChunk loaded) {
                    replaceBlocks(level, loaded, replacements);
                }
            }
        }
    }

    private Map<Block, BlockState> marsReplacements() {
        if (marsReplacements == null) {
            var ores = new HashMap<Block, BlockState>();
            MARS_ORES.forEach((original, material) -> BuiltInRegistries.BLOCK.getOptional(Identifier.parse(original))
                    .ifPresent(block -> ores.put(block, BuiltInRegistries.BLOCK.getValue(
                            Identifier.fromNamespaceAndPath("gspack", "mars_" + material + "_ore")).defaultBlockState())));
            var combined = new HashMap<Block, BlockState>();
            Unification.blocks().forEach((from, to) -> combined.put(from, ores.getOrDefault(to.getBlock(), to)));
            combined.putAll(ores);
            marsReplacements = Map.copyOf(combined);
        }
        return marsReplacements;
    }

    private void onServerStopped(ServerStoppedEvent event) {
        pendingChunks.clear();
    }

    private static void replaceBlocks(ServerLevel level, LevelChunk chunk, Map<Block, BlockState> replacements) {
        var sections = chunk.getSections();
        var pos = chunk.getPos();
        var targetPos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (section.hasOnlyAir() || !section.maybeHas(state -> replacements.containsKey(state.getBlock()))) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(index) * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        var target = replacements.get(section.getBlockState(x, y, z).getBlock());
                        if (target != null) {
                            targetPos.set(pos.getMinBlockX() + x, baseY + y, pos.getMinBlockZ() + z);
                            level.setBlock(targetPos, target, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }
}
