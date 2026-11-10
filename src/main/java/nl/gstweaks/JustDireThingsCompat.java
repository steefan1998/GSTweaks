package nl.gstweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

final class JustDireThingsCompat {
    static void addModifiers(ModifyRecipeJsonsEvent event) {
        addModifier(event, "smelter", "autosmelt");
        addModifier(event, "glowing", "glowing");
        addModifier(event, "hammer", "expanded");
    }

    private static void addModifier(ModifyRecipeJsonsEvent event, String upgrade, String modifier) {
        var recipes = event.getRecipeJsons();
        var original = recipes.get(Identifier.parse("tconstruct:tools/modifiers/ability/" + modifier));
        if (!(original instanceof JsonObject template)) {
            return;
        }
        var recipe = template.deepCopy();
        var id = Identifier.fromNamespaceAndPath(GSTweaks.MOD_ID, "justdirethings/" + upgrade);
        var input = new JsonObject();
        input.addProperty("ingredient", "justdirethings:upgrade_" + upgrade);
        var inputs = new JsonArray();
        inputs.add(input);
        recipe.add("inputs", inputs);
        recipe.addProperty("id", id.toString());
        recipe.addProperty("allow_crystal", false);
        recipes.putIfAbsent(id, recipe);
    }

    private JustDireThingsCompat() {}
}
