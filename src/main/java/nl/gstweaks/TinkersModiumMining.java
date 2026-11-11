package nl.gstweaks;

import net.allthemods.allthemodium.core.registry.ATMTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningTierToolHook;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.HarvestTiers;

/**
 * AllTheModium only accepts tools with a vanilla tool component on its ores, which excludes Tinker tools.
 * A Tinker tool of netherite tier or higher (for example after the netherite gem upgrade) may mine them,
 * provided Tinkers itself considers the tool effective, so vibranium and unobtainium keep their higher tiers.
 * GSTweaks loads after AllTheModium, so these LOWEST listeners run after its own handlers.
 */
final class TinkersModiumMining {
    private static final Identifier MINIMUM = Identifier.parse("minecraft:netherite");

    static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, TinkersModiumMining::breakSpeed);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TinkersModiumMining::harvestCheck);
    }

    private static void breakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled() && canMine(event.getEntity(), event.getState())) {
            event.setCanceled(false);
        }
    }

    private static void harvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.canHarvest() && canMine(event.getEntity(), event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    private static boolean canMine(Player player, BlockState state) {
        if (!state.is(ATMTags.Blocks.ORES_ALLTHEMODIUM) && !state.is(ATMTags.Blocks.ORES_VIBRANIUM)
                && !state.is(ATMTags.Blocks.ORES_UNOBTANIUM)) {
            return false;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IModifiable)) {
            return false;
        }
        var tool = ToolStack.from(stack);
        ToolMaterial required = HarvestTiers.byName(MINIMUM);
        return required != null && !tool.isBroken()
                && MiningTierToolHook.getTier(tool) instanceof ToolMaterial actual
                && HarvestTiers.max(actual, required).equals(actual)
                && IsEffectiveToolHook.isEffective(tool, state);
    }

    private TinkersModiumMining() {}
}
