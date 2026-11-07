package nl.gstweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class Unification {
    private record Rules(Map<Item, Item> items, Map<Block, BlockState> blocks, Set<Item> hidden) {}

    private static class RuleHolder {
        private static final Rules INSTANCE = resolve();
    }

    static void register() {
        if (UnificationConfig.get().enabled()) {
            var bus = NeoForge.EVENT_BUS;
            bus.addListener(Unification::onLogin);
            bus.addListener(Unification::onPlayerTick);
            bus.addListener(Unification::onContainerOpen);
        }
    }

    static Map<Block, BlockState> blocks() {
        return RuleHolder.INSTANCE.blocks();
    }

    static Set<Item> hiddenItems() {
        return RuleHolder.INSTANCE.hidden();
    }

    private static Rules resolve() {
        var config = UnificationConfig.get();
        if (!config.enabled()) {
            return new Rules(Map.of(), Map.of(), Set.of());
        }

        var itemReplacements = new HashMap<Item, Item>();
        var blockReplacements = new HashMap<Block, Block>();
        var hidden = new HashSet<Item>();
        for (var group : config.groups()) {
            var items = group.items().stream().map(Unification::item).filter(value -> value != Items.AIR).distinct().toList();
            if (!items.isEmpty()) {
                var preferred = items.getFirst();
                items.stream().skip(1).forEach(value -> itemReplacements.put(value, preferred));
            }
            if (group.convertBlocks()) {
                var blocks = group.items().stream().map(Unification::block).filter(value -> value != Blocks.AIR).distinct().toList();
                if (!blocks.isEmpty()) {
                    var preferred = blocks.getFirst();
                    blocks.stream().skip(1).forEach(value -> addBlock(blockReplacements, value, preferred));
                }
            }
        }
        config.hiddenItems().stream().map(Unification::item).filter(itemReplacements::containsKey).forEach(hidden::add);

        config.blockReplacements().forEach((from, to) -> addBlock(blockReplacements, block(from), block(to)));
        var resolvedBlocks = new HashMap<Block, BlockState>();
        blockReplacements.forEach((from, to) -> resolvedBlocks.put(from, terminalBlock(from, blockReplacements).defaultBlockState()));
        if (config.overrideOreData() && GSTweaks.loaded("alltheores")) {
            for (String id : config.hiddenOres()) {
                if (resolvedBlocks.containsKey(block(id)) && item(id) != Items.AIR) {
                    hidden.add(item(id));
                }
            }
        }
        return new Rules(Map.copyOf(itemReplacements), Map.copyOf(resolvedBlocks), Set.copyOf(hidden));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    private static void addBlock(Map<Block, Block> replacements, Block from, Block to) {
        if (from != to && from != Blocks.AIR && to != Blocks.AIR
                && !from.defaultBlockState().hasBlockEntity() && !to.defaultBlockState().hasBlockEntity()) {
            replacements.put(from, to);
        }
    }

    private static Block terminalBlock(Block from, Map<Block, Block> replacements) {
        var visited = new HashSet<Block>();
        while (replacements.containsKey(from)) {
            if (!visited.add(from)) {
                throw new IllegalArgumentException("Cyclic block replacement in GSTweaks unification: "
                        + BuiltInRegistries.BLOCK.getKey(from));
            }
            from = replacements.get(from);
        }
        return from;
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            convert(player.getInventory());
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            convert(player.getInventory());
        }
    }

    private static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer && event.getContainer() instanceof ChestMenu menu
                && convert(menu.getContainer())) {
            menu.broadcastChanges();
        }
    }

    private static boolean convert(Container inventory) {
        var replacements = RuleHolder.INSTANCE.items();
        if (replacements.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            var stack = inventory.getItem(index);
            var preferred = replacements.get(stack.getItem());
            if (stack.isEmpty() || preferred == null) {
                continue;
            }
            var replacement = stack.transmuteCopy(preferred);
            if (replacement.getCount() <= inventory.getMaxStackSize(replacement)) {
                inventory.setItem(index, replacement);
                changed = true;
            }
        }
        if (changed) {
            inventory.setChanged();
        }
        return changed;
    }

    private Unification() {}
}
