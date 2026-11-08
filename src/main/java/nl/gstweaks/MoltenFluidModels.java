package nl.gstweaks;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;

@EventBusSubscriber(modid = GSTweaks.MOD_ID, value = Dist.CLIENT)
final class MoltenFluidModels {
    private static final Material STILL = new Material(Identifier.fromNamespaceAndPath("tconstruct", "fluid/molten/still"));
    private static final Material FLOWING = new Material(Identifier.fromNamespaceAndPath("tconstruct", "fluid/molten/flowing"));

    @SubscribeEvent
    static void register(RegisterFluidModelsEvent event) {
        MoltenMaterials.fluids().forEach((material, fluid) -> event.register(
                new FluidModel.Unbaked(STILL, FLOWING, null, FluidTintSources.constant(0xFF000000 | material.color())),
                fluid.source(), fluid.flowing()));
    }

    private MoltenFluidModels() {}
}
