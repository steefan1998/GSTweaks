ServerEvents.recipes(event => {
  // ----- The final goal: Ultimate Crafting Table (9x9) -----
  // Proof that you have done most of the pack. From the outside in:
  //   ring 1: the eight Twilight Forest boss trophies, 4 Nether Star Blocks, 4 Crystaltine Blocks and
  //           compressed blocks from All The Compressed: 8 Double Compressed Netherite Blocks (L) and
  //           4 Double Compressed Unobtainium-Vibranium Alloy Blocks (M)
  //   ring 2: the capstone item of 20 mods, 4 Creative Singularities, 4 Crystaltine Blocks
  //   ring 3: the eight creative items, 4 Infinite Control Circuits, 4 Compressed Antimatter Blocks (O)
  //   ring 4: 4 Creative Singularities, 4 Ultimate Components
  //   centre: the Extended Crafting Ultimate Singularity
  // With the eight creative items that is 16 Creative Singularities in total.
  const recipe = {
    type: 'extendedcrafting:shaped_table',
    pattern: [
      'NFaLMLbFN',
      'FSmqolrSF',
      'cpOA+BOtd',
      'LuCUSUEvL',
      'Mk+S*S+iM',
      'LeDUSUIsL',
      '1wOG+HOx2',
      'FSyznj@SF',
      'NFgLMLhFN'
    ],
    key: {
      // creative items
      A: 'mekanism:creative_energy_cube',
      B: 'ae2:creative_storage_cell',
      C: 'oritech:creative_storage',
      D: 'powah:energy_cell_creative',
      E: 'mysticalagradditions:creative_essence',
      G: 'sophisticatedstorage:infinity_upgrade',
      H: 'mekanism:creative_fluid_tank',
      I: 'ae2:creative_energy_cell',
      // endgame materials
      S: 'gspack:creative_singularity',
      '*': 'extendedcrafting:ultimate_singularity',
      U: 'extendedcrafting:ultimate_component',
      F: 'extendedcrafting:crystaltine_block',
      N: 'extendedcrafting:nether_star_block',
      '+': 'mekanism_extras:infinite_control_circuit',
      // compressed blocks (All The Compressed)
      L: 'allthecompressed:netherite_block_2x',
      M: 'allthecompressed:unobtainium_vibranium_block_2x',
      O: 'allthecompressed:antimatter_block_1x',
      // capstones: one per mod
      m: 'mekanism:mekasuit_bodyarmor',
      q: 'advanced_ae:quantum_core',
      o: 'oritech:black_hole',
      l: 'ltxi:infinite_energy_cell_array',
      r: 'railcraft:electric_locomotive',
      p: 'powah:energy_cell_nitro',
      t: 'tconstruct:queens_slime_ingot',
      u: 'thaumaturge:primordial_pearl',
      v: 'apotheosis:godforged_pearl',
      k: 'equivox:klein_star_omega',
      i: 'mysticalagradditions:insanium_gemstone_block',
      e: 'evilcraft:dark_power_gem_block',
      s: 'rootsclassic:crystal_staff',
      w: 'aether_ii:valkyrie_wings',
      x: 'allthemodium:unobtainium_vibranium_block',
      y: 'stellaris:heart_of_luna_trophy',
      z: 'stellaris:star_crawler_boss_trophy',
      n: 'aquaculture:neptunium_block',
      j: 'stellaris:novite_block',
      '@': 'aether_ii:gravitite_block',
      // the Twilight Forest boss trophies
      a: 'twilightforest:naga_trophy',
      b: 'twilightforest:lich_trophy',
      c: 'twilightforest:minoshroom_trophy',
      d: 'twilightforest:hydra_trophy',
      '1': 'twilightforest:knight_phantom_trophy',
      '2': 'twilightforest:ur_ghast_trophy',
      g: 'twilightforest:alpha_yeti_trophy',
      h: 'twilightforest:snow_queen_trophy'
    },
    result: { id: 'gspack:infinity_trophy', count: 1 }
  }
  if (Platform.isLoaded('extendedcrafting') && Object.values(recipe.key).every(item => Item.exists(item))) {
    event.custom(recipe).id('gspack:endgame/infinity_trophy')
  }


})
