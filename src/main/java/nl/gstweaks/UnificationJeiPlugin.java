package nl.gstweaks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

@JeiPlugin
public final class UnificationJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(GSTweaks.MOD_ID, "unification");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        var hidden = Unification.hiddenItems();
        if (!hidden.isEmpty()) {
            var ingredients = runtime.getIngredientManager();
            var duplicates = ingredients.getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                    .filter(stack -> hidden.contains(stack.getItem())).toList();
            ingredients.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, duplicates);
        }
    }
}
