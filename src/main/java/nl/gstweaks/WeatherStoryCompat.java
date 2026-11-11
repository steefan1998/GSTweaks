package nl.gstweaks;

import dev.weatherstory.WeatherAccess;
import dev.weatherstory.WeatherConditions;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;

final class WeatherStoryCompat {
    static void register() {
        NeoForge.EVENT_BUS.addListener(WeatherStoryCompat::redirectRainToggle);
    }

    private static void redirectRainToggle(CommandEvent event) {
        var parsed = event.getParseResults();
        var nodes = parsed.getContext().getNodes();
        if (parsed.getReader().canRead() || nodes.size() != 2
                || !nodes.get(0).getNode().getName().equals("ftblibrary")
                || !nodes.get(1).getNode().getName().equals("rain")) {
            return;
        }

        var source = parsed.getContext().getSource();
        var level = source.getLevel();
        if (!WeatherAccess.active(level)) {
            return;
        }
        var dispatcher = source.getServer().getCommands().getDispatcher();
        var conditions = WeatherConditions.at(level, BlockPos.containing(source.getPosition()));
        var replacement = dispatcher.parse(
                "weatherstory weather spawn " + (conditions.precipitating() ? "clear" : "rain"), source);
        if (!replacement.getReader().canRead() && replacement.getContext().getCommand() != null) {
            event.setParseResults(replacement);
        }
    }

    private WeatherStoryCompat() {}
}
