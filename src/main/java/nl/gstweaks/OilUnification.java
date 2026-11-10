package nl.gstweaks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

import java.util.stream.Stream;

final class OilUnification {
    private static final String ORITECH_OIL = "oritech:still_oil";
    private static final String BUILDCRAFT_OIL = "buildcraftenergy:oil";
    private static final String BUILDCRAFT_SPOUT_OIL = "buildcraftenergy:spout_oil";
    private static final String STELLARIS_OIL = "stellaris:oil";

    static void addRecipes(ModifyRecipeJsonsEvent event) {
        boolean oritech = hasFluid(ORITECH_OIL);
        String base = oritech ? ORITECH_OIL : Stream.of(BUILDCRAFT_OIL, STELLARIS_OIL, BUILDCRAFT_SPOUT_OIL)
                .filter(OilUnification::hasFluid).findFirst().orElse(null);
        if (base == null || (!oritech && !GSTweaks.loaded("mekanism"))) {
            return;
        }
        addConversion(event, "buildcraft_oil", BUILDCRAFT_OIL, base, oritech);
        addConversion(event, "buildcraft_spout_oil", BUILDCRAFT_SPOUT_OIL, base, oritech);
        addConversion(event, "stellaris_oil", STELLARIS_OIL, base, oritech);

        addBaseOilRecipe(event, "buildcraftfactory:distiller/buildcraftenergy/oil",
                "input", BUILDCRAFT_OIL, base, "buildcraft_distillation");
        addBaseOilRecipe(event, "buildcraftfactory:heat_exchanger/heating/buildcraftenergy/oil_to_oil_heat_1",
                "input", BUILDCRAFT_OIL, base, "buildcraft_heating");
        addBaseOilRecipe(event, "stellaris:fuel_refining", "ingredient", STELLARIS_OIL, base, "stellaris_fuel_refining");
    }

    private static void addConversion(ModifyRecipeJsonsEvent event, String name, String source, String base, boolean oritech) {
        if (source.equals(base) || !hasFluid(source)) {
            return;
        }
        var input = new JsonObject();
        input.addProperty("ingredient", source);
        input.addProperty("amount", 1000);
        var output = new JsonObject();
        output.addProperty("id", base);
        output.addProperty("amount", 1000);
        var recipe = new JsonObject();
        if (oritech) {
            var outputs = new JsonArray();
            outputs.add(output);
            recipe.addProperty("type", "oritech:centrifuge_fluid");
            recipe.add("fluidInput", input);
            recipe.add("fluidOutputs", outputs);
            recipe.add("itemInputs", new JsonArray());
            recipe.add("itemResults", new JsonArray());
            recipe.addProperty("time", 80);
        } else {
            recipe.addProperty("type", "mekanism:evaporating");
            recipe.add("input", input);
            recipe.add("output", output);
        }
        put(event, name, recipe);
    }

    private static void addBaseOilRecipe(ModifyRecipeJsonsEvent event, String original, String inputField,
                                         String originalOil, String base, String name) {
        if (originalOil.equals(base)) {
            return;
        }
        var source = event.getRecipeJsons().get(Identifier.parse(original));
        if (!(source instanceof JsonObject recipe) || !(recipe.get(inputField) instanceof JsonObject input)
                || !new JsonPrimitive(originalOil).equals(input.get("fluid"))) {
            return;
        }
        var copy = recipe.deepCopy();
        var copyInput = copy.getAsJsonObject(inputField);
        copyInput.addProperty("fluid", base);
        if (copyInput.has("id")) {
            copyInput.addProperty("id", base);
        }
        put(event, name, copy);
    }

    private static boolean hasFluid(String fluid) {
        return BuiltInRegistries.FLUID.containsKey(Identifier.parse(fluid));
    }

    private static void put(ModifyRecipeJsonsEvent event, String name, JsonObject recipe) {
        var id = Identifier.fromNamespaceAndPath(GSTweaks.MOD_ID, "oil/" + name);
        if (recipe.has("id")) {
            recipe.addProperty("id", id.toString());
        }
        event.getRecipeJsons().putIfAbsent(id, recipe);
    }

    private OilUnification() {}
}
