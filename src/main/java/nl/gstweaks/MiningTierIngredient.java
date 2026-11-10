package nl.gstweaks;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningTierToolHook;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.HarvestTiers;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

record MiningTierIngredient(Identifier minimum) implements ICustomIngredient {
    static final String ID = "gstweaks:tinker_pickaxe_tier";
    private static final Identifier PICKAXE = Identifier.parse("tconstruct:pickaxe");
    private static final IngredientType<MiningTierIngredient> TYPE = new IngredientType<>(
            Identifier.CODEC.fieldOf("minimum").xmap(MiningTierIngredient::new, MiningTierIngredient::minimum));

    static void register(IEventBus bus) {
        var types = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, GSTweaks.MOD_ID);
        types.register("tinker_pickaxe_tier", () -> TYPE);
        types.register(bus);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (!stack.is(ItemTags.PICKAXES) || !(stack.getItem() instanceof IModifiable)) {
            return false;
        }
        var tool = ToolStack.from(stack);
        ToolMaterial required = HarvestTiers.byName(minimum);
        return required != null && !tool.getMaterials().isEmpty() && !tool.isBroken()
                && MiningTierToolHook.getTier(tool) instanceof ToolMaterial actual
                && HarvestTiers.max(actual, required).equals(actual);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.PICKAXES).spliterator(), false)
                .filter(item -> item.value() instanceof IModifiable);
    }

    @Override
    public SlotDisplay display() {
        String material = switch (minimum.getPath()) {
            case "stone" -> "rock";
            case "copper" -> "copper";
            case "iron" -> "iron";
            case "diamond" -> "cobalt";
            case "netherite" -> "hepatizon";
            default -> "wood";
        };
        var head = MaterialVariant.of(MaterialVariantId.parse("tconstruct:" + material));
        var wood = MaterialVariant.of(MaterialVariantId.parse("tconstruct:wood"));
        var pickaxe = (IModifiable) BuiltInRegistries.ITEM.getValue(PICKAXE);
        var example = ToolBuildHandler.buildItemFromMaterials(pickaxe, MaterialNBT.of(head, wood, wood));
        return new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(example));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }
}
