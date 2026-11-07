package nl.gstweaks;

import com.leclowndu93150.thaumaturge.api.research.scan.IScannable;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanKeys;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanTarget;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;

final class ThaumaturgeCompat {
    static void register() {
        ScanningManager.register(new IScannable() {
            @Override
            public boolean matches(Player player, ScanTarget target) {
                var stack = ScanningManager.stackOf(player, target);
                if (stack.isEmpty()) {
                    return false;
                }
                var item = stack.getItem();
                var namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
                return !namespace.equals("minecraft") && !namespace.equals("thaumaturge")
                        && (item instanceof BlockItem || namespace.equals("alltheores"))
                        && ScanningManager.aspectsOf(player, target).isEmpty();
            }

            @Override
            public Identifier research(Player player, ScanTarget target) {
                return ScanKeys.item(ScanningManager.stackOf(player, target).getItem());
            }

            @Override
            public boolean rescannable(Player player, ScanTarget target) {
                return false;
            }
        });
    }

    private ThaumaturgeCompat() {}
}
