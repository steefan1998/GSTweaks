package nl.gstweaks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class ToolProgression {
    private static final Map<String, String> TINKER_TOOLS = Map.of(
            "pickaxe", "tconstruct:pickaxe", "axe", "tconstruct:hand_axe", "shovel", "tconstruct:mattock",
            "hoe", "tconstruct:kama", "sword", "tconstruct:sword", "spear", "tconstruct:javelin");
    private static final Pattern VANILLA_TOOL = Pattern.compile(
            "(?:minecraft:)?(wooden|stone|copper|iron|golden|diamond|netherite)_(pickaxe|axe|shovel|hoe|sword|spear)");
    private static final Set<String> INPUT_FIELDS = Set.of(
            "key", "ingredient", "ingredients", "input", "inputs", "base", "addition", "template",
            "catalyst", "catalysts", "incenses", "activation_item", "baseinput", "addedinput", "transformInput");
    private static final Set<String> PROCESSING_TYPES = Set.of(
            "apotheosis:salvaging", "railcraft:blasting", "productivebees:bee_nbt_changer",
            "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking");
    private static final Set<String> INGREDIENT_PAYLOAD_FIELDS = Set.of(
            "id", "item", "items", "ingredient", "ingredients", "children", "base", "subtracted");

    static void apply(ModifyRecipeJsonsEvent event) {
        if (GSTweaksConfig.get().woodenToolsOnly) {
            event.getRecipeJsons().entrySet().removeIf(entry -> producesBlockedTool(entry.getValue()));
            event.getRecipeJsons().forEach(ToolProgression::replaceRecipeInputs);
        }
    }

    private static boolean producesBlockedTool(JsonElement recipe) {
        if (!recipe.isJsonObject()) {
            return false;
        }
        var object = recipe.getAsJsonObject();
        if ("mekanismtools:paxel".equals(string(object.get("type")))) {
            return true;
        }
        var result = object.get("result");
        if (result != null && result.isJsonObject()) {
            result = result.getAsJsonObject().get("id");
        }
        String id = string(result);
        if (id == null) {
            return false;
        }
        var matcher = VANILLA_TOOL.matcher(id);
        return (matcher.matches() && !matcher.group(1).equals("wooden"))
                || (id.endsWith("_paxel") && (id.startsWith("mekanismtools:") || id.startsWith("allthemodium:")));
    }

    private static void replaceRecipeInputs(Identifier id, JsonElement recipe) {
        String namespace = id.getNamespace();
        if (!recipe.isJsonObject() || namespace.equals("minecraft") || namespace.equals("tconstruct")) {
            return;
        }
        replaceRecipeInputs(recipe.getAsJsonObject());
    }

    private static void replaceRecipeInputs(JsonObject recipe) {
        String type = string(recipe.get("type"));
        if (type != null && (type.startsWith("tconstruct:") || PROCESSING_TYPES.contains(type))) {
            return;
        }
        boolean changed = false;
        boolean changedBase = false;
        for (var entry : recipe.entrySet()) {
            String field = entry.getKey();
            JsonElement value = entry.getValue();
            if (field.equals("recipe") && value.isJsonObject()) {
                replaceRecipeInputs(value.getAsJsonObject());
            } else if (INPUT_FIELDS.contains(field) || (field.equals("components") && "thaumaturge:infusion".equals(type))) {
                boolean replaced;
                if (field.equals("key") && value.isJsonObject()) {
                    replaced = replaceObjectInputs(value.getAsJsonObject(), value.getAsJsonObject().keySet());
                } else {
                    var replacement = replaceTool(value);
                    replaced = replacement != value;
                    if (replaced) {
                        entry.setValue(replacement);
                    } else {
                        replaced = replaceNestedInputs(value);
                    }
                }
                changed |= replaced;
                changedBase |= replaced && field.equals("base");
            }
        }
        if (changedBase && ("minecraft:smithing_transform".equals(type) || "smithing_transform".equals(type))) {
            recipe.addProperty("type", ToolSmithingRecipe.ID.toString());
        }
        if (changed && recipe.has("transfer_components")) {
            recipe.addProperty("transfer_components", false);
        }
    }

    private static boolean replaceObjectInputs(JsonObject object, Set<String> fields) {
        boolean changed = false;
        for (var entry : object.entrySet()) {
            if (!fields.contains(entry.getKey())) {
                continue;
            }
            var value = entry.getValue();
            var replacement = replaceTool(value);
            if (replacement != value) {
                entry.setValue(replacement);
                changed = true;
            } else {
                changed |= replaceNestedInputs(value);
            }
        }
        return changed;
    }

    private static boolean replaceNestedInputs(JsonElement value) {
        if (value.isJsonObject()) {
            return replaceObjectInputs(value.getAsJsonObject(), INGREDIENT_PAYLOAD_FIELDS);
        }
        boolean changed = false;
        if (value.isJsonArray()) {
            var array = value.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                var element = array.get(index);
                var replacement = replaceTool(element);
                if (replacement != element) {
                    array.set(index, replacement);
                    changed = true;
                } else {
                    changed |= replaceNestedInputs(element);
                }
            }
        }
        return changed;
    }

    private static JsonElement replaceTool(JsonElement element) {
        String id = string(element);
        if (id != null) {
            var matcher = VANILLA_TOOL.matcher(id);
            if (matcher.matches()) {
                return new JsonPrimitive(TINKER_TOOLS.get(matcher.group(2)));
            }
        }
        return element;
    }

    private static String string(JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : null;
    }

    private ToolProgression() {}
}
