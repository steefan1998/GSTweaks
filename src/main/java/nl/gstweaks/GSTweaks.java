package nl.gstweaks;

import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.gstweaks.daily.DailyRewards;

import java.util.function.Supplier;

@Mod(GSTweaks.MOD_ID)
public final class GSTweaks {
    public static final String MOD_ID = "gstweaks";

    // The namespace is part of the save format and must survive the KubeJS handoff.
    static final String CONTENT_NAMESPACE = "gspack";
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CONTENT_NAMESPACE);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CONTENT_NAMESPACE);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    private static final Supplier<Item> TECH_CORE = epic("tech_core");
    static final Supplier<Block> VILLAGE_SUPPORT;

    static {
        epic("arcane_core");
        epic("verdant_core");
        epic("dimensional_core");
        epic("cosmic_core");
        epic("creative_singularity");
        epic("martian_research_relic");
        epic("martian_navigation_core");

        var trophy = BLOCKS.registerBlock("infinity_trophy", TrophyBlock::new,
                properties -> properties.strength(3, 1200).sound(SoundType.METAL).noOcclusion());
        ITEMS.registerSimpleBlockItem(trophy, () -> epicProperties(1));

        for (String material : new String[]{"allthemodium", "vibranium", "unobtainium"}) {
            simpleBlock(material + "_crux", BlockBehaviour.Properties.of().strength(5, 1200)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops());
        }

        simpleBlock("mars_stone", BlockBehaviour.Properties.of().strength(1.5F, 6));
        for (String name : new String[]{"mars_regolith", "mars_sand"}) {
            simpleBlock(name, BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.SAND));
        }
        for (String ore : new String[]{"iron", "copper", "gold", "osmium", "uranium"}) {
            simpleBlock("mars_" + ore + "_ore", oreProperties());
        }
        var unobtainium = BLOCKS.register("mars_unobtainium_ore", id -> {
            if (loaded("allthemodium")) {
                return AllTheModiumCompat.createOre(id);
            }
            return new RedStoneOreBlock(oreProperties().setId(ResourceKey.create(Registries.BLOCK, id)));
        });
        ITEMS.registerSimpleBlockItem(unobtainium);
        simpleBlock("mars_lumestone", BlockBehaviour.Properties.of().strength(2, 6)
                .requiresCorrectToolForDrops().lightLevel(state -> 13));
        VILLAGE_SUPPORT = simpleBlock("martian_village_life_support", BlockBehaviour.Properties.of()
                .strength(-1, 3_600_000).noLootTable());

        TABS.register("content", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.gstweaks.content"))
                .icon(() -> new ItemStack(TECH_CORE.get()))
                .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                .build());
    }

    public GSTweaks(IEventBus modBus) {
        var settings = GSTweaksConfig.get();
        boolean hasTinkers = loaded("tconstruct");
        if (settings.woodenToolsOnly && !hasTinkers) {
            throw new IllegalStateException("GSTweaks woodenToolsOnly requires Continuum Tinker (tconstruct). "
                    + "Install it or disable woodenToolsOnly in config/gstweaks/gstweaks.json.");
        }
        UnificationConfig.get();
        if (settings.autoMigrateKubeJs) {
            KubeJsMigration.run();
        }
        MoltenMaterials.register(modBus);
        DailyRewards.register(modBus);
        IdMigration.registerAliases();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(GSTweaks::addPacks);
        if (settings.endgameTooltips) {
            NeoForge.EVENT_BUS.addListener(EndgameTooltips::add);
        }
        Unification.register();
        if (settings.unifyOil && (loaded("oritech") || loaded("mekanism"))) {
            NeoForge.EVENT_BUS.addListener(OilUnification::addRecipes);
        }
        new BlockMigration().register();
        if (hasTinkers && (settings.tinkersOreRecipes || settings.tinkersToolMaterials)) {
            NeoForge.EVENT_BUS.addListener(TinkersRecipes::addRecipes);
        }
        if (hasTinkers) {
            MiningTierIngredient.register(modBus);
            ToolSmithingRecipe.register(modBus);
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ToolProgression::apply);
            GSTweaksOptions.register(modBus);
            if (loaded("allthemodium")) {
                TinkersModiumMining.register();
            }
        }
        if (loaded("stellaris")) {
            new MarsEvents().register();
        }
        if (loaded("thaumaturge") && settings.thaumaturgeCompat) {
            modBus.<net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent>addListener(
                    event -> event.enqueueWork(ThaumaturgeCompat::register));
        }
    }

    static boolean loaded(String mod) {
        return ModList.get().isLoaded(mod);
    }

    private static void addPacks(AddPackFindersEvent event) {
        if (loaded("stellaris")) {
            addPack(event, "mars", PackType.SERVER_DATA, "GSTweaks Mars");
            addPack(event, "mars_art", PackType.CLIENT_RESOURCES, "GSTweaks Mars visuals");
            if (loaded("mekanismadditions")) {
                addPack(event, "mars_mekanism", PackType.SERVER_DATA, "GSTweaks Mars Mekanism mobs");
            }
        }
        if (loaded("alltheores")) {
            addPack(event, "alltheores", PackType.SERVER_DATA, "GSTweaks AllTheOres compatibility");
            var config = UnificationConfig.get();
            if (config.enabled() && config.overrideOreData()) {
                addPack(event, "ore_unification", PackType.SERVER_DATA, "GSTweaks ore unification");
            }
        }
        if (loaded("thaumaturge") && GSTweaksConfig.get().thaumaturgeCompat) {
            addPack(event, "thaumaturge", PackType.SERVER_DATA, "GSTweaks Thaumaturge compatibility");
        }
        if (loaded("tconstruct") && GSTweaksConfig.get().tinkersToolMaterials) {
            addPack(event, "tinkers_materials", PackType.SERVER_DATA, "GSTweaks Tinkers tool materials");
        }
        if (loaded("allthemodium")) {
            addPack(event, "allthemodium", PackType.SERVER_DATA, "GSTweaks Allthemodium compatibility");
        }
    }

    private static void addPack(AddPackFindersEvent event, String path, PackType type, String name) {
        if (event.getPackType() == type) {
            event.addPackFinders(Identifier.fromNamespaceAndPath(MOD_ID, "packs/" + path), type,
                    Component.literal(name), PackSource.BUILT_IN, true, Pack.Position.TOP);
        }
    }

    private static Supplier<Item> epic(String name) {
        return ITEMS.registerSimpleItem(name, () -> epicProperties(16));
    }

    private static Item.Properties epicProperties(int stackSize) {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(stackSize).fireResistant();
    }

    private static BlockBehaviour.Properties oreProperties() {
        return BlockBehaviour.Properties.of().strength(3).requiresCorrectToolForDrops();
    }

    private static Supplier<Block> simpleBlock(String name, BlockBehaviour.Properties properties) {
        var block = BLOCKS.registerSimpleBlock(name, () -> properties);
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }

    private static final class TrophyBlock extends Block {
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

        TrophyBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
            return SHAPE;
        }
    }
}
