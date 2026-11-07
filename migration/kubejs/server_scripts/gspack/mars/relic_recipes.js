ServerEvents.recipes(event => {
  const recipe = {
    type:'extendedcrafting:shaped_table',
    pattern:['ABBBA','BNPNB','BQRQB','BNPNB','ABBBA'],
    key:{A:'extendedcrafting:advanced_component',B:'extendedcrafting:black_iron_ingot',
      N:'stellaris:novite_ingot',P:'powah:crystal_nitro',Q:'advanced_ae:quantum_processor',R:'gspack:martian_research_relic'},
    result:{id:'gspack:martian_navigation_core',count:1}
  }
  if (Platform.isLoaded('stellaris') && Platform.isLoaded('extendedcrafting')
      && Object.values(recipe.key).every(item => Item.exists(item))) {
    event.custom(recipe).id('gspack:mars/navigation_core')
  }
})
