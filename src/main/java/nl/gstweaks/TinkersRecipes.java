package nl.gstweaks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

final class TinkersRecipes {
    private static final Set<String> MELTING_TYPES =
            Set.of("tconstruct:melting", "tconstruct:ore_melting", "tconstruct:damageable_melting");
    private static final com.mojang.serialization.Codec<java.util.Optional<Ingredient>> MELTING_INPUT =
            ConditionalOps.createConditionalCodec(Ingredient.CODEC.fieldOf("ingredient").codec());
    private static final JsonObject NATIVE_TEMPLATES = loadTemplates();

    static void addRecipes(ModifyRecipeJsonsEvent event) {
        var settings = GSTweaksConfig.get();
        var context = ConditionalOps.retrieveContext().codec().parse(event.getOps(), new JsonObject()).getOrThrow();
        var existing = event.getRecipeJsons().values().stream()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .filter(json -> json.has("type") && json.get("type").isJsonPrimitive()
                        && MELTING_TYPES.contains(json.get("type").getAsString()))
                .flatMap(json -> MELTING_INPUT.parse(event.getOps(), json).result().flatMap(value -> value).stream())
                .map(ingredient -> matcher(context, ingredient))
                .toList();
        var added = new HashSet<Item>();

        if (settings.tinkersOreRecipes) {
            NATIVE_TEMPLATES.entrySet().forEach(entry -> {
                var template = entry.getValue().getAsJsonObject();
                if (hasFluid(context, template.getAsJsonObject("result"))) {
                    addOres(event, context, entry.getKey(), template, existing, added);
                }
            });
        }

        for (var material : MoltenMaterials.materials()) {
            boolean toolParts = settings.tinkersToolMaterials && material.toolMaterial();
            var output = output(context, material);
            if (output == Items.AIR) {
                continue;
            }
            // Refined-only materials can be melted and recast without a corresponding ore.
            if (material.oreAmount() == 0) {
                if (settings.tinkersOreRecipes) {
                    addCasting(event, material, output);
                }
                if (toolParts) {
                    addToolParts(event, material, output, existing);
                } else if (settings.tinkersOreRecipes) {
                    addProductMelting(event, material, output, existing, "melting/");
                }
                continue;
            }
            if (items(context, "c:ores/" + material.name()).findAny().isEmpty()) {
                continue;
            }

            var id = material.name();
            var fluid = material.fluidId();
            var temperature = material.temperature();
            var ingot = material.cast().equals("ingot");
            if (settings.tinkersOreRecipes) {
                addCasting(event, material, output);

                var ore = json("tconstruct:ore_melting");
                ore.add("result", fluid(fluid, material.oreAmount()));
                ore.addProperty("rate", ingot ? "metal" : "gem");
                ore.addProperty("temperature", temperature);
                ore.addProperty("time", 160);
                addOres(event, context, id, ore, existing, added);
            }

            if (toolParts) {
                addToolParts(event, material, output, existing);
            }
        }
        LogUtils.getLogger().info("GSTweaks added {} Tinkers ore melting recipes", added.size());
    }

    private static void addCasting(ModifyRecipeJsonsEvent event, MoltenMaterials.Material material, Item output) {
        var casting = json("tconstruct:casting_table");
        casting.add("fluid", fluid(material.fluidId(), material.amount()));
        casting.addProperty("result", itemId(output));
        casting.addProperty("cooling_time", 60);
        if (!material.cast().isEmpty()) {
            casting.addProperty("cast", "#tconstruct:casts/multi_use/" + material.cast());
        }
        put(event, "casting/" + material.name(), casting);
    }

    private static void addToolParts(ModifyRecipeJsonsEvent event, MoltenMaterials.Material material, Item output,
                                     List<Predicate<Item>> existing) {
        var name = material.name();
        var id = "gstweaks:" + name;
        var fluid = material.fluidId();
        int temperature = material.temperature();
        var repair = json("tconstruct:material");
        repair.addProperty("ingredient", itemId(output));
        repair.addProperty("material", id);
        repair.addProperty("value", 1);
        repair.addProperty("needed", 1);
        put(event, "materials/repair/" + name, repair);

        var parts = json("tconstruct:material_fluid");
        parts.add("fluid", fluid(fluid, material.amount()));
        parts.addProperty("output", id);
        parts.addProperty("temperature", temperature);
        put(event, "materials/casting/" + name, parts);

        var recycle = json("tconstruct:material_melting");
        recycle.addProperty("input", id);
        recycle.add("result", fluid(fluid, material.amount()));
        recycle.addProperty("temperature", temperature);
        put(event, "materials/recycling/" + name, recycle);

        // Refined material remains a source of fluid even when another addon handles the ore.
        addProductMelting(event, material, output, existing, "materials/melting/");
    }

    private static void addProductMelting(ModifyRecipeJsonsEvent event, MoltenMaterials.Material material, Item output,
                                          List<Predicate<Item>> existing, String recipePath) {
        if (existing.stream().noneMatch(ingredient -> ingredient.test(output))) {
            var melt = json("tconstruct:melting");
            melt.addProperty("ingredient", itemId(output));
            melt.add("result", fluid(material.fluidId(), material.amount()));
            melt.addProperty("temperature", material.temperature());
            melt.addProperty("time", 110);
            put(event, recipePath + material.name(), melt);
        }
    }

    private static void addOres(ModifyRecipeJsonsEvent event, ICondition.IContext context, String material,
                                JsonObject template, List<Predicate<Item>> existing, Set<Item> added) {
        items(context, "c:ores/" + material).forEach(holder -> {
            var item = holder.value();
            if (item == Items.AIR || added.contains(item)) {
                return;
            }
            if (existing.stream().anyMatch(ingredient -> ingredient.test(item))) {
                return;
            }
            var ore = BuiltInRegistries.ITEM.getKey(item);
            var recipe = template.deepCopy();
            recipe.addProperty("type", "tconstruct:ore_melting");
            recipe.addProperty("ingredient", ore.toString());
            put(event, "ore/" + ore.getNamespace() + "/" + ore.getPath(), recipe);
            added.add(item);
        });
    }

    private static Item output(ICondition.IContext context, MoltenMaterials.Material material) {
        var config = UnificationConfig.get();
        if (config.enabled()) {
            var configured = config.groups().stream().filter(group -> group.tag().equals(material.productTag()))
                    .flatMap(group -> group.items().stream())
                    .map(id -> BuiltInRegistries.ITEM.getValue(Identifier.parse(id)))
                    .filter(item -> item != Items.AIR).findFirst();
            if (configured.isPresent()) {
                return configured.get();
            }
        }
        if (material.fallbackItem() != null) {
            var fallback = BuiltInRegistries.ITEM.getValue(Identifier.parse(material.fallbackItem()));
            if (fallback != Items.AIR) {
                return fallback;
            }
        }
        return items(context, material.productTag()).map(Holder::value).findFirst().orElse(Items.AIR);
    }

    private static Predicate<Item> matcher(ICondition.IContext context, Ingredient ingredient) {
        // Recipe preparation precedes both tag application and item component binding on a dedicated server.
        if (!ingredient.isCustom()) {
            var tag = ingredient.getValues().unwrapKey();
            if (tag.isPresent()) {
                var contents = context.getTag(tag.get()).stream().map(Holder::value).collect(java.util.stream.Collectors.toSet());
                return contents::contains;
            }
            return item -> ingredient.getValues().contains(item.builtInRegistryHolder());
        }
        return switch (ingredient.getCustomIngredient()) {
            case CompoundIngredient compound -> {
                var children = compound.children().stream().map(child -> matcher(context, child)).toList();
                yield stack -> children.stream().anyMatch(child -> child.test(stack));
            }
            case IntersectionIngredient intersection -> {
                var children = intersection.children().stream().map(child -> matcher(context, child)).toList();
                yield stack -> children.stream().allMatch(child -> child.test(stack));
            }
            case DifferenceIngredient difference -> {
                var base = matcher(context, difference.base());
                var excluded = matcher(context, difference.subtracted());
                yield stack -> base.test(stack) && !excluded.test(stack);
            }
            default -> item -> {
                try {
                    return ingredient.acceptsItem(item.builtInRegistryHolder());
                } catch (IllegalStateException unboundTag) {
                    return false;
                }
            };
        };
    }

    private static Stream<Holder<Item>> items(ICondition.IContext context, String tag) {
        return context.getTag(TagKey.create(Registries.ITEM, Identifier.parse(tag))).stream();
    }

    private static boolean hasFluid(ICondition.IContext context, JsonObject fluid) {
        if (fluid.has("tag")) {
            var tag = TagKey.create(Registries.FLUID, Identifier.parse(fluid.get("tag").getAsString()));
            return !context.getTag(tag).isEmpty();
        }
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(fluid.get("fluid").getAsString())) != Fluids.EMPTY;
    }

    private static JsonObject fluid(String id, int amount) {
        var fluid = new JsonObject();
        fluid.addProperty("fluid", id);
        fluid.addProperty("amount", amount);
        return fluid;
    }

    private static JsonObject json(String type) {
        var json = new JsonObject();
        json.addProperty("type", type);
        return json;
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void put(ModifyRecipeJsonsEvent event, String path, JsonObject recipe) {
        var id = Identifier.fromNamespaceAndPath("gspack", "tinkers/" + path);
        recipe.addProperty("id", id.toString());
        event.getRecipeJsons().putIfAbsent(id, recipe);
    }

    private static JsonObject loadTemplates() {
        try (var reader = new InputStreamReader(
                Objects.requireNonNull(TinkersRecipes.class.getResourceAsStream("/tinkers-native-melting.json")),
                StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load Tinkers melting templates", exception);
        }
    }

    private TinkersRecipes() {}
}
