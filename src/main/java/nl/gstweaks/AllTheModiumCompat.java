package nl.gstweaks;

import net.allthemods.allthemodium.common.blocks.ModiumOreBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Optional;

final class AllTheModiumCompat {
    static Block createOre(Identifier id) {
        Block original = BuiltInRegistries.BLOCK.getValue(Identifier.parse("allthemodium:unobtainium_ore"));
        var properties = BlockBehaviour.Properties.ofFullCopy(original)
                .setId(ResourceKey.create(Registries.BLOCK, id))
                .overrideDescription("block.gspack.mars_unobtainium_ore")
                .overrideLootTable(Optional.of(ResourceKey.create(Registries.LOOT_TABLE,
                        Identifier.parse("gspack:blocks/mars_unobtainium_ore"))));
        return new ModiumOreBlock(UniformInt.of(30, 50), properties);
    }

    private AllTheModiumCompat() {}
}
