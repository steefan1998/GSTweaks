package nl.gstweaks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

final class EndgameTooltips {
    static void add(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!GSTweaks.CONTENT_NAMESPACE.equals(id.getNamespace())) {
            return;
        }
        int lines = switch (id.getPath()) {
            case "tech_core", "creative_singularity", "infinity_trophy" -> 3;
            case "arcane_core", "verdant_core", "dimensional_core", "cosmic_core" -> 2;
            default -> 0;
        };
        for (int line = 1; line <= lines; line++) {
            var color = id.getPath().equals("infinity_trophy") && line == 3 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
            event.getToolTip().add(Component.translatable("tooltip.gspack." + id.getPath() + "." + line).withStyle(color));
        }
    }

    private EndgameTooltips() {}
}
