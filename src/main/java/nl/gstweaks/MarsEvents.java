package nl.gstweaks;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.exodusstudio.stellaris.common.blocks.entities.machines.OxygenDistributorBlockEntity;
import org.exodusstudio.stellaris.common.registries.FluidsRegistry;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;

final class MarsEvents {
    private static final String SUIT_MARKER = "gspackMarsSpaceSuit";
    private static final Identifier DEFAULT_TANK = Identifier.parse("stellaris:space_suit_oxygen_module_tier_1");
    private static final Map<EquipmentSlot, String> SUIT = Map.of(
            EquipmentSlot.HEAD, "space_suit_helmet",
            EquipmentSlot.CHEST, "space_suit_chestplate",
            EquipmentSlot.LEGS, "space_suit_leggings",
            EquipmentSlot.FEET, "space_suit_boots");
    private final TankDrops tankDrops = TankDrops.read();

    void register() {
        var bus = NeoForge.EVENT_BUS;
        var settings = GSTweaksConfig.get();
        if (settings.marsZombieSuit || settings.marsRaidPillagers) {
            bus.addListener(this::onJoin);
        }
        if (settings.marsZombieSuit) {
            bus.addListener(this::onDeath);
        }
        if (settings.marsVillageLifeSupport) {
            bus.addListener(this::onTick);
            bus.addListener(MarsEvents::protectDistributor);
        }
    }

    private void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !BlockMigration.MARS.equals(level.dimension())) {
            return;
        }
        if (GSTweaksConfig.get().marsZombieSuit && event.getEntity() instanceof Zombie zombie && zombie.getType() == EntityType.ZOMBIE
                && !zombie.getPersistentData().getBooleanOr(SUIT_MARKER, false)) {
            SUIT.forEach((slot, item) -> {
                var equipment = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("stellaris", item));
                zombie.setItemSlot(slot, new ItemStack(equipment));
                zombie.setDropChance(slot, 0);
            });
            zombie.getPersistentData().putBoolean(SUIT_MARKER, true);
        }

        if (GSTweaksConfig.get().marsRaidPillagers && event.getEntity() instanceof Raider raider
                && raider.getType() != EntityType.PILLAGER) {
            var raid = raider.getCurrentRaid();
            if (raid == null) {
                return;
            }
            var replacement = EntityType.PILLAGER.create(level, EntitySpawnReason.EVENT);
            if (replacement == null) {
                return;
            }
            event.setCanceled(true);
            int wave = raider.getWave();
            raid.removeFromRaid(level, raider, true);
            if (raider.isPatrolLeader()) {
                raid.setLeader(wave, replacement);
            }
            raid.joinRaid(level, wave, replacement, raider.blockPosition(), false);
        }
    }

    private void onDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (tankDrops.enabled() && entity.level() instanceof ServerLevel level && BlockMigration.MARS.equals(level.dimension())
                && entity.getType() == EntityType.ZOMBIE
                && entity.getPersistentData().getBooleanOr(SUIT_MARKER, false)
                && level.getRandom().nextDouble() < tankDrops.chance()) {
            var item = BuiltInRegistries.ITEM.getValue(tankDrops.item());
            if (item != Items.AIR) {
                Block.popResource(level, entity.blockPosition(), new ItemStack(item));
            }
        }
    }

    private void onTick(ServerTickEvent.Post event) {
        var level = event.getServer().getLevel(BlockMigration.MARS);
        if (level != null && event.getServer().getTickCount() % 40 == 0) {
            refillDistributors(level);
        }
    }

    private static void refillDistributors(ServerLevel level) {
        var visited = new HashSet<ChunkPos>();
        for (var player : level.players()) {
            var center = player.chunkPosition();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    var pos = new ChunkPos(center.x() + dx, center.z() + dz);
                    if (!visited.add(pos)) {
                        continue;
                    }
                    var chunk = level.getChunkSource().getChunk(pos.x(), pos.z(), ChunkStatus.FULL, false);
                    if (!(chunk instanceof LevelChunk loaded)) {
                        continue;
                    }
                    for (var blockEntity : loaded.getBlockEntities().values()) {
                        if (blockEntity instanceof OxygenDistributorBlockEntity distributor
                                && isVillageDistributor(level, distributor.getBlockPos())) {
                            distributor.getEnergy(null).setEnergyStored(12_800);
                            if (distributor.getOxygenTank().getFluidValueInTank() < 9_990) {
                                distributor.getOxygenTank().setFluidInTank(
                                        FluidStack.create(FluidsRegistry.OXYGEN_STILL.get(), 10_000));
                            }
                            distributor.setChanged();
                        }
                    }
                }
            }
        }
    }

    private static void protectDistributor(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && BlockMigration.MARS.equals(level.dimension())
                && level.getBlockEntity(event.getPos()) instanceof OxygenDistributorBlockEntity
                && isVillageDistributor(level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static boolean isVillageDistributor(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(GSTweaks.VILLAGE_SUPPORT.get());
    }

    private record TankDrops(boolean enabled, double chance, Identifier item) {
        static TankDrops read() {
            var path = FMLPaths.CONFIGDIR.get().resolve("gspack-mars.json");
            if (Files.isRegularFile(path)) {
                try (var reader = Files.newBufferedReader(path)) {
                    var settings = JsonParser.parseReader(reader).getAsJsonObject();
                    boolean enabled = !settings.has("oxygenTankDropsEnabled")
                            || settings.get("oxygenTankDropsEnabled").getAsBoolean();
                    double chance = settings.has("oxygenTankDropChance")
                            ? settings.get("oxygenTankDropChance").getAsDouble() : 0.01;
                    var item = settings.has("oxygenTankDropItem")
                            ? Identifier.parse(settings.get("oxygenTankDropItem").getAsString()) : DEFAULT_TANK;
                    return new TankDrops(enabled, Math.clamp(chance, 0, 1), item);
                } catch (Exception exception) {
                    LogUtils.getLogger().warn("Invalid Mars oxygen drop settings in {}", path, exception);
                }
            }
            return new TankDrops(true, 0.01, DEFAULT_TANK);
        }
    }
}
