package nl.gstweaks;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

final class ToolSmithingRecipe extends SmithingTransformRecipe {
    static final Identifier ID = Identifier.fromNamespaceAndPath(GSTweaks.MOD_ID, "tool_smithing");
    private static final RecipeSerializer<SmithingTransformRecipe> SERIALIZER = new RecipeSerializer<>(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> ((ToolSmithingRecipe) recipe).commonInfo),
                    Ingredient.CODEC.optionalFieldOf("template").forGetter(SmithingTransformRecipe::templateIngredient),
                    Ingredient.CODEC.fieldOf("base").forGetter(SmithingTransformRecipe::baseIngredient),
                    Ingredient.CODEC.optionalFieldOf("addition").forGetter(SmithingTransformRecipe::additionIngredient),
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> ((ToolSmithingRecipe) recipe).output)
            ).apply(instance, ToolSmithingRecipe::new)),
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, recipe -> ((ToolSmithingRecipe) recipe).commonInfo,
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, SmithingTransformRecipe::templateIngredient,
                    Ingredient.CONTENTS_STREAM_CODEC, SmithingTransformRecipe::baseIngredient,
                    Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, SmithingTransformRecipe::additionIngredient,
                    ItemStackTemplate.STREAM_CODEC, recipe -> ((ToolSmithingRecipe) recipe).output,
                    ToolSmithingRecipe::new));
    private final ItemStackTemplate output;

    static void register(IEventBus bus) {
        var serializers = DeferredRegister.create(Registries.RECIPE_SERIALIZER, GSTweaks.MOD_ID);
        serializers.register(ID.getPath(), () -> SERIALIZER);
        serializers.register(bus);
    }

    private ToolSmithingRecipe(Recipe.CommonInfo info, Optional<Ingredient> template, Ingredient base,
                               Optional<Ingredient> addition, ItemStackTemplate output) {
        super(info, template, base, addition, output);
        this.output = output;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        // Tinker tools store their material stats in components that do not belong on the smithing result.
        return output.create();
    }

    @Override
    public RecipeSerializer<SmithingTransformRecipe> getSerializer() {
        return SERIALIZER;
    }
}
