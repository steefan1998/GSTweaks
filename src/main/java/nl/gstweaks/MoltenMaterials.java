package nl.gstweaks;

import com.google.gson.Gson;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MoltenMaterials {
    public record Material(String name, List<String> mods, String productTag, String fallbackItem,
                           String cast, int amount, int oreAmount, int temperature, int color, boolean toolMaterial) {
        public String fluidId() {
            return GSTweaks.MOD_ID + ":molten_" + name;
        }
    }

    record FluidPair(DeferredHolder<Fluid, FlowingFluid> source, DeferredHolder<Fluid, FlowingFluid> flowing) {}

    private static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, GSTweaks.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, GSTweaks.MOD_ID);
    private static final Map<Material, FluidPair> REGISTERED = new LinkedHashMap<>();

    static void register(IEventBus bus) {
        if (!GSTweaks.loaded("tconstruct")) {
            return;
        }
        try (var reader = new InputStreamReader(
                Objects.requireNonNull(MoltenMaterials.class.getResourceAsStream("/molten-materials.json")),
                StandardCharsets.UTF_8)) {
            for (Material material : new Gson().fromJson(reader, Material[].class)) {
                if (material.mods().stream().anyMatch(GSTweaks::loaded)) {
                    register(material);
                }
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot load GSTweaks molten materials", exception);
        }
        TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private static void register(Material material) {
        var sourceName = "molten_" + material.name();
        var flowingName = "flowing_" + sourceName;
        var type = TYPES.register(sourceName, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type.gstweaks." + sourceName).temperature(material.temperature() + 273)
                .density(3000).viscosity(5000).lightLevel(10)));
        var sourceRef = DeferredHolder.<Fluid, FlowingFluid>create(Registries.FLUID, Identifier.parse(material.fluidId()));
        var flowingRef = DeferredHolder.<Fluid, FlowingFluid>create(Registries.FLUID,
                Identifier.fromNamespaceAndPath(GSTweaks.MOD_ID, flowingName));
        var properties = new BaseFlowingFluid.Properties(type, sourceRef, flowingRef);
        FLUIDS.register(sourceName, () -> new BaseFlowingFluid.Source(properties));
        FLUIDS.register(flowingName, () -> new BaseFlowingFluid.Flowing(properties));
        REGISTERED.put(material, new FluidPair(sourceRef, flowingRef));
    }

    public static List<Material> materials() {
        return List.copyOf(REGISTERED.keySet());
    }

    static Map<Material, FluidPair> fluids() {
        return REGISTERED;
    }

    private MoltenMaterials() {}
}
