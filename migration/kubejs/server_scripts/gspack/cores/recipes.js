// GSPack endgame: the Creative Singularity and the craftable creative items.
//
// Five cores, one per pillar of the pack, each made from the hardest items of that pillar:
//   Technological Core  - Mekanism, Oritech, Advanced AE
//   Arcane Core         - Thaumaturge, Apotheosis, Equivox, Occultism
//   Verdant Core        - Mystical Agradditions, Aquaculture, Unobtainium seeds
//   Dimensional Core    - The Bumblezone, Twilight Forest, Aether II
//   Cosmic Core         - Stellaris
// The five cores make a Creative Singularity; every creative item needs one.
// Creative items are crafted in an Extended Crafting Elite Table (7x7), the Trophy of Infinity in an
// Ultimate Table (9x9). The tables themselves are made more expensive in gspack_crossmod_recipes.js.

ServerEvents.recipes(event => {

  // Each recipe is independent. Leave upstream recipes alone when its replacement cannot load.
  const addCompatRecipe = (recipe, id, replace) => {
    if (!Platform.isLoaded(recipe.type.split(':')[0])) return
    const ingredients = Object.values(recipe.key || {}).concat(recipe.ingredients || [])
    if (recipe.input) ingredients.push(recipe.input)
    const exists = ingredient => Item.exists(typeof ingredient === 'string' ? ingredient : ingredient.items)
    if (!Item.exists(recipe.result.id) || !ingredients.every(exists)) return
    if (replace) event.remove(replace)
    event.custom(recipe).id(id)
  }

  // ----- Cores: mod-specific endgame milestones, Elite Crafting Table (7x7) -----
  const coreRecipes = [
    [
      "tech_core",
      "allthecompressed:osmium_block_2x",
      [
        "mekanism:pellet_antimatter",
        "oritech:super_ai_chip",
        "advanced_ae:quantum_processor",
        "mekanism_extras:infinite_control_circuit",
        "advanced_ae:quantum_core",
        "ae2:singularity",
        "powah:energy_cell_nitro",
        "oritech:atomic_forge",
        "mekanism:ultimate_induction_cell"
      ]
    ],
    [
      "arcane_core",
      "equivox:red_matter_block",
      [
        "thaumaturge:metal_void",
        "apotheosis:godforged_pearl",
        "equivox:red_matter_block",
        "occultism:storage_stabilizer_tier4",
        "thaumaturge:primordial_pearl",
        "rootsclassic:crystal_staff",
        "evilcraft:dark_power_gem_block",
        "equivox:klein_star_omega",
        "thaumaturge:metal_void"
      ]
    ],
    [
      "verdant_core",
      "allthecompressed:emerald_block_2x",
      [
        "mysticalagradditions:insanium_gemstone_block",
        "productivetrees:time_traveller_sapling",
        "productivefarming:farm_controller",
        "productivetrees:cured_rubber",
        "mysticalagriculture:unobtainium_seeds",
        "productivefarming:barley_seeds_bag",
        "productivetrees:maple_syrup",
        "aquaculture:neptunium_block",
        "productivefarming:onion_crate"
      ]
    ],
    [
      "dimensional_core",
      "aether_ii:gravitite_block",
      [
        "twilightforest:hydra_trophy",
        "aether_ii:gravitite_block",
        "the_bumblezone:royal_jelly_block",
        "twilightforest:ur_ghast_trophy",
        "the_bumblezone:essence_of_the_bees",
        "aether_ii:valkyrie_wings",
        "twilightforest:snow_queen_trophy",
        "aether_ii:zanite_block",
        "the_bumblezone:glistering_honey_crystal"
      ]
    ],
    [
      "cosmic_core",
      "stellaris:novite_block",
      [
        "stellaris:heart_of_luna_trophy",
        "stellaris:autopilot_module",
        "stellaris:star_crawler_boss_trophy",
        "stellaris:shield_module",
        "stellaris:novite_block",
        "stellaris:space_station_blueprint",
        "powah:energy_cell_nitro",
        "gspack:martian_navigation_core",
        "stellaris:astrum_vitreus_block"
      ]
    ]
  ]
  coreRecipes.forEach(([name, frame, ingredients]) => {
    if (name === 'cosmic_core' && !Platform.isLoaded('stellaris')) return
    if (name === 'tech_core') {
      // 24 pedestals on the outer ring of a 7x7 area, core at the same-height center.
      // One ingredient per pedestal; the central item is the Quantum Core.
      let pedestalItems = ingredients.filter(item => item !== 'advanced_ae:quantum_core')
      for (let i = 0; i < 8; i++) pedestalItems.push('extendedcrafting:elite_component')
      for (let i = 0; i < 4; i++) pedestalItems.push(frame)
      for (let i = 0; i < 4; i++) pedestalItems.push('extendedcrafting:black_iron_block')
      addCompatRecipe({
        type: 'extendedcrafting:combination',
        input: 'advanced_ae:quantum_core',
        ingredients: pedestalItems,
        power_cost: 250000000,
        power_rate: 25000,
        result: { id: 'gspack:tech_core', count: 1 }
      }, 'gspack:endgame/tech_core')
      return
    }
    let key = { A: 'extendedcrafting:elite_component', B: 'extendedcrafting:black_iron_ingot', C: frame }
    ingredients.forEach((item, i) => { key[String.fromCharCode(68 + i)] = item })
    let corePattern = ['ABBCBBA', 'B     B', 'B DEF B', 'C GHI C', 'B JKL B', 'B     B', 'ABBCBBA']
    if (name === 'verdant_core') {
      let harvest = ['minecraft:wheat', 'minecraft:carrot', 'minecraft:potato', 'minecraft:beetroot',
        'minecraft:pumpkin', 'minecraft:melon_slice', 'minecraft:sugar_cane', 'minecraft:apple',
        'productivefarming:rice', 'productivefarming:cotton', 'productivefarming:strawberry',
        'productivefarming:black_beans', 'productivefarming:sugar_beet', 'productivefarming:garlic',
        'productivefarming:cabbage', 'productivefarming:blueberry']
      harvest.forEach((item, i) => { key['123456789abcdefg'.charAt(i)] = item })
      corePattern = ['ABBCBBA', 'B12345B', 'B6DEF7B', 'C8GHI9C', 'BaJKLbB', 'BcdefgB', 'ABBCBBA']
    }
    addCompatRecipe({
      type: 'extendedcrafting:shaped_table',
      pattern: corePattern,
      key: key,
      result: { id: 'gspack:' + name, count: 1 }
    }, 'gspack:endgame/' + name)
  })

  // ----- Creative Singularity: Powah Energizing, rods supply power externally -----
  addCompatRecipe({
    type: 'powah:energizing',
    energy: 1000000000,
    ingredients: ['gspack:tech_core', 'gspack:arcane_core', 'gspack:verdant_core',
      'gspack:dimensional_core', 'gspack:cosmic_core'],
    result: { id: 'gspack:creative_singularity', count: 1 }
  }, 'gspack:endgame/creative_singularity')

  // ----- Creative items: Elite Crafting Table (7x7), a singularity in the middle, the best version of the item around it -----
  // A = corners (4), B = edges (4), Z = a compressed block from All The Compressed (4), plus Extended Crafting materials:
  // C = Black Iron Ingot (16), D = Elite Component (4), E = Crystaltine Ingot (4)
  // [output, ingredient A, ingredient B, recipe id, compressed block Z]
  const creative = [
    ['mekanism:creative_energy_cube', 'mekanism_extras:infinite_energy_cube', 'mekanism:pellet_antimatter', 'mekanism_energy_cube', 'allthecompressed:osmium_block_2x'],
    ['mekanism:creative_fluid_tank', 'mekanism_extras:infinite_fluid_tank', 'mekanism:pellet_antimatter', 'mekanism_fluid_tank', 'allthecompressed:osmium_block_2x'],
    ['mekanism:creative_chemical_tank', 'mekanism_extras:infinite_chemical_tank', 'mekanism:pellet_antimatter', 'mekanism_chemical_tank', 'allthecompressed:osmium_block_2x'],
    ['mekanism:creative_bin', 'mekanism_extras:infinite_bin', 'mekanism:pellet_antimatter', 'mekanism_bin', 'allthecompressed:osmium_block_2x'],
    ['mekanism_extras:upgrade_creative', 'mekanism_extras:upgrade_stack', 'mekanism:upgrade_speed', 'mekanism_upgrade', 'allthecompressed:redstone_block_2x'],
    ['ae2:creative_energy_cell', 'ae2:dense_energy_cell', 'advanced_ae:quantum_processor', 'ae2_energy_cell', 'allthecompressed:fluix_block_2x'],
    ['ae2:creative_storage_cell', 'ae2:item_storage_cell_256k', 'advanced_ae:quantum_storage_256', 'ae2_storage_cell', 'allthecompressed:fluix_block_2x'],
    ['oritech:creative_storage', 'oritech:large_storage', 'oritech:overcharged_crystal', 'oritech_storage', 'allthecompressed:steel_block_2x'],
    ['oritech:creative_tank', 'oritech:portable_tank', 'oritech:super_ai_chip', 'oritech_tank', 'allthecompressed:steel_block_2x'],
    ['powah:energy_cell_creative', 'powah:energy_cell_nitro', 'powah:crystal_nitro', 'powah_energy_cell', 'allthecompressed:nitro_crystal_block_2x'],
    ['mysticalagradditions:creative_essence', 'mysticalagradditions:insanium_gemstone_block', 'mysticalagriculture:awakened_supremium_ingot_block', 'mystical_essence', 'allthecompressed:emerald_block_2x'],
    ['sophisticatedstorage:infinity_upgrade', 'sophisticatedstorage:stack_upgrade_omega_tier', 'storage_allthemodium_upgrade:stack_upgrade_tier_8', 'storage_infinity', 'allthecompressed:allthemodium_block_1x'],
    ['sophisticatedbackpacks:infinity_upgrade', 'sophisticatedbackpacks:stack_upgrade_omega_tier', 'backpack_allthemodium_upgrade:stack_upgrade_tier_7', 'backpack_infinity', 'allthecompressed:allthemodium_block_1x'],
    ['storagedrawers:creative_storage_upgrade', 'storagedrawers:emerald_storage_upgrade', 'storagedrawers:netherite_storage_upgrade', 'drawers_storage', 'allthecompressed:emerald_block_2x'],
    ['storagedrawers:creative_vending_upgrade', 'storagedrawers:creative_storage_upgrade', 'storagedrawers:netherite_storage_upgrade', 'drawers_vending', 'allthecompressed:emerald_block_2x'],
    ['functionalstorage:creative_vending_upgrade', 'functionalstorage:max_storage_upgrade', 'functionalstorage:netherite_upgrade', 'functional_vending', 'allthecompressed:diamond_block_2x'],
    ['pipez:infinity_upgrade', 'pipez:ultimate_upgrade', 'mekanism:pellet_antimatter', 'pipez_infinity', 'allthecompressed:gold_block_2x']
  ]

  creative.forEach(([output, corner, edge, id, compressed]) => {
    addCompatRecipe({
      type: 'extendedcrafting:shaped_table',
      pattern: [
        'ACCBCCA',
        'CD Z DC',
        'C  E  C',
        'BEZSZEB',
        'C  E  C',
        'CD Z DC',
        'ACCBCCA'
      ],
      key: {
        A: corner,
        B: edge,
        C: 'extendedcrafting:black_iron_ingot',
        D: 'extendedcrafting:elite_component',
        E: 'extendedcrafting:crystaltine_ingot',
        S: 'gspack:creative_singularity',
        Z: compressed
      },
      result: { id: output, count: 1 }
    }, `gspack:endgame/creative/${id}`, {output: output})
  })

  // ----- The Ultimate Singularity (Extended Crafting): Ultimate Crafting Table (9x9) -----
  // Replaces Extended Crafting's own recipe. From the outside in:
  //   ring 1: 16 Black Iron Blocks, 8 Triple Compressed Iron Blocks, 4 Nether Star Blocks, 4 Ultimate Catalysts
  //   ring 2: all 19 Extended Crafting singularities, 1 Nether Star Block, 4 Ultimate Components
  //   ring 3: 4 Antimatter Pellets, 4 Insanium Gemstone Blocks, 4 Compressed Unobtainium Blocks, 4 Void Metal
  //   ring 4: 4 Crystaltine Blocks, 4 Infinite Control Circuits
  //   centre: a Creative Singularity
  // Each singularity is the same item with a different extendedcrafting:singularity_id component.
  const singularity = name => ({
    'neoforge:ingredient_type': 'neoforge:components',
    items: 'extendedcrafting:singularity',
    components: { 'extendedcrafting:singularity_id': `extendedcrafting:${name}` }
  })
  const singularities = ['aluminum', 'bronze', 'coal', 'copper', 'diamond', 'electrum', 'emerald', 'glowstone', 'gold', 'invar',
    'iron', 'lapis_lazuli', 'lead', 'nickel', 'platinum', 'redstone', 'silver', 'steel', 'tin']
  const ultimateKey = {
    B: 'extendedcrafting:black_iron_block',
    C: 'extendedcrafting:ultimate_catalyst',
    N: 'extendedcrafting:nether_star_block',
    U: 'extendedcrafting:ultimate_component',
    P: 'mekanism:pellet_antimatter',
    I: 'mysticalagradditions:insanium_gemstone_block',
    O: 'allthecompressed:unobtainium_block_1x',
    R: 'allthecompressed:iron_block_3x',
    W: 'thaumaturge:metal_void',
    E: 'extendedcrafting:crystaltine_block',
    K: 'mekanism_extras:infinite_control_circuit',
    X: 'gspack:creative_singularity'
  }
  // keys a..s are the 19 singularities, in the order of the list above
  singularities.forEach((name, i) => { ultimateKey[String.fromCharCode(97 + i)] = singularity(name) })

  // ----- Craftable trophy ingredients -----
  // These items had no recipe at all (boss trophies, rare drops, LTX's infinite blocks). They get an
  // Extended Crafting recipe: a 3x3 core of their own mod's materials, framed by components (1),
  // Black Iron Ingots (2) and an extra material (3). Advanced = 5x5, Elite = 7x7.
  // [tier, output, core pattern, core key, extra ingredient, recipe name]
  // A Thaumaturge Vis Crystal is one item with a thaumaturge:crystal_aspect component (the crystal blocks don't drop themselves).
  const visCrystal = aspect => ({
    'neoforge:ingredient_type': 'neoforge:components',
    items: 'thaumaturge:essentia_crystal',
    components: { 'thaumaturge:crystal_aspect': { aspect: `thaumaturge:${aspect}` } }
  })
  const newTableRecipes = [
    // The Twilight Forest boss trophies
    ['advanced', 'twilightforest:naga_trophy', ['SSS', 'SNS', 'SSS'],
      { S: 'twilightforest:naga_scale', N: 'minecraft:nether_star' }, 'twilightforest:steeleaf_block', 'naga_trophy'],
    ['advanced', 'twilightforest:lich_trophy', ['TIZ', 'INI', 'FID'],
      { T: 'twilightforest:twilight_scepter', Z: 'twilightforest:zombie_scepter', F: 'twilightforest:fortification_scepter',
        D: 'twilightforest:lifedrain_scepter', I: 'twilightforest:ironwood_block', N: 'minecraft:nether_star' }, 'twilightforest:ironwood_ingot', 'lich_trophy'],
    ['advanced', 'twilightforest:minoshroom_trophy', ['MXM', 'XNX', 'MXM'],
      { M: 'twilightforest:meef_stroganoff', X: 'twilightforest:maze_map_focus', N: 'minecraft:nether_star' }, 'twilightforest:ironwood_block', 'minoshroom_trophy'],
    ['advanced', 'twilightforest:hydra_trophy', ['BCB', 'CNC', 'BCB'],
      { B: 'twilightforest:fiery_blood', C: 'twilightforest:hydra_chop', N: 'minecraft:nether_star' }, 'twilightforest:fiery_block', 'hydra_trophy'],
    ['advanced', 'twilightforest:knight_phantom_trophy', ['KAK', 'ANA', 'KAK'],
      { K: 'twilightforest:knightmetal_block', A: 'twilightforest:armor_shard_cluster', N: 'minecraft:nether_star' }, 'twilightforest:knightmetal_ingot', 'knight_phantom_trophy'],
    ['advanced', 'twilightforest:ur_ghast_trophy', ['TCT', 'CNC', 'TCT'],
      { T: 'twilightforest:fiery_tears', C: 'twilightforest:carminite_block', N: 'minecraft:nether_star' }, 'twilightforest:carminite', 'ur_ghast_trophy'],
    ['advanced', 'twilightforest:alpha_yeti_trophy', ['FIF', 'INI', 'FIF'],
      { F: 'twilightforest:alpha_yeti_fur', I: 'twilightforest:ice_bomb', N: 'minecraft:nether_star' }, 'twilightforest:arctic_fur', 'alpha_yeti_trophy'],
    ['advanced', 'twilightforest:snow_queen_trophy', ['BAB', 'ANA', 'BAB'],
      { B: 'twilightforest:aurora_block', A: 'minecraft:blue_ice', N: 'minecraft:nether_star' }, 'twilightforest:ice_bow', 'snow_queen_trophy'],
    // The Stellaris boss trophies
    ['advanced', 'stellaris:heart_of_luna_trophy', ['DVD', 'VNV', 'DVD'],
      { D: 'stellaris:desh_block', V: 'stellaris:vaccine', N: 'stellaris:novite_block' }, 'stellaris:astrum_vitreus_block', 'heart_of_luna_trophy'],
    ['advanced', 'stellaris:star_crawler_boss_trophy', ['TCT', 'CNC', 'TCT'],
      { T: 'stellaris:lunar_parasite_tooth', C: 'stellaris:lunar_crawler_spike_cluster', N: 'stellaris:novite_block' }, 'stellaris:astrum_vitreus_block', 'star_crawler_trophy'],
    // The Aether II
    ['advanced', 'aether_ii:valkyrie_wings', ['FGF', 'GAG', 'FGF'],
      { F: 'aether_ii:moa_feather', G: 'aether_ii:gravitite_block', A: 'aether_ii:golden_amber_block' }, 'aether_ii:zanite_block', 'valkyrie_wings'],
    // LTX Industries: the infinite tanks
    ['advanced', 'ltxi:infinite_water_tank', ['TWT', 'WPW', 'TWT'],
      { T: 'ltxi:t4_circuit', W: 'minecraft:water_bucket', P: 'ltxi:portable_tank' }, 'minecraft:heart_of_the_sea', 'infinite_water_tank'],
    ['advanced', 'ltxi:infinite_lava_tank', ['TLT', 'LPL', 'TLT'],
      { T: 'ltxi:t4_circuit', L: 'minecraft:lava_bucket', P: 'ltxi:portable_tank' }, 'twilightforest:fiery_ingot', 'infinite_lava_tank'],
    // Elite: the rarest items
    ['elite', 'thaumaturge:primordial_pearl', ['ABC', 'DVE', 'FGH'],
      { A: visCrystal('aer'), B: visCrystal('aqua'), C: visCrystal('ignis'), D: visCrystal('terra'),
        E: visCrystal('ordo'), F: visCrystal('perditio'), G: 'thaumaturge:primal_charm', H: 'thaumaturge:ingot_void',
        V: 'thaumaturge:metal_void' }, 'thaumaturge:salis_mundus', 'primordial_pearl'],
    ['elite', 'apotheosis:godforged_pearl', ['LSL', 'SNS', 'LSL'],
      { L: 'apotheosis:luminous_crystal_shard', S: 'apotheosis:sigil_of_supremacy', N: 'minecraft:nether_star' }, 'apotheosis:sigil_of_enhancement', 'godforged_pearl'],
    ['elite', 'oritech:black_hole', ['FSF', 'SMS', 'FSF'],
      { F: 'oritech:fluxite_block', S: 'oritech:superconductor', M: 'oritech:still_strange_matter_bucket' }, 'oritech:super_ai_chip', 'black_hole'],
    ['elite', 'ltxi:infinite_energy_cell_array', ['CTC', 'TAT', 'CTC'],
      { C: 'ltxi:large_voltaic_cell', T: 'ltxi:t5_circuit', A: 'ltxi:energy_cell_array' }, 'ltxi:nano_logic_core', 'infinite_energy_cell_array']
  ]

  newTableRecipes.forEach(([tier, output, p, key, extra, name]) => {
    let pattern = tier === 'advanced'
      ? ['12321', `2${p[0]}2`, `3${p[1]}3`, `2${p[2]}2`, '12321']
      : ['1223221', '2     2', `2 ${p[0]} 2`, `3 ${p[1]} 3`, `2 ${p[2]} 2`, '2     2', '1223221']
    let fullKey = {
      '1': `extendedcrafting:${tier}_component`,
      '2': 'extendedcrafting:black_iron_ingot',
      '3': extra
    }
    Object.keys(key).forEach(k => { fullKey[k] = key[k] })
    addCompatRecipe({ type: 'extendedcrafting:shaped_table', pattern: pattern, key: fullKey, result: { id: output, count: 1 } }, `gspack:craftable/${name}`)
  })

  addCompatRecipe({
    type: 'extendedcrafting:shaped_table',
    pattern: [
      'CBRBNBRBC',
      'BUabcdeUB',
      'RfPIWIPgR',
      'BhOEKEOiB',
      'NjWKXKWkN',
      'BlOEKEOmB',
      'RnPIWIPoR',
      'BUpqNrsUB',
      'CBRBNBRBC'
    ],
    key: ultimateKey,
    result: { id: 'extendedcrafting:ultimate_singularity', count: 1 }
  }, 'gspack:endgame/ultimate_singularity', {id: 'extendedcrafting:ultimate_singularity'})
})
