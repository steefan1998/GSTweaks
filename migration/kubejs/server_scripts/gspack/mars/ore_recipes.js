// Silk-Touched Mars ores retain standard furnace/blast-furnace processing.
ServerEvents.recipes(event => {
  ;['iron','copper','gold','osmium','uranium','unobtainium'].forEach(name => {
    let namespace = ['osmium','uranium'].includes(name) ? 'alltheores' : name === 'unobtainium' ? 'allthemodium' : 'minecraft'
    let input = 'gspack:mars_'+name+'_ore'
    let output = namespace+':'+name+'_ingot'
    if (!Item.exists(output)) return
    event.smelting(output,input).xp(0.7).id('gspack:mars/'+name+'_smelting')
    event.blasting(output,input).xp(0.7).id('gspack:mars/'+name+'_blasting')
  })
})
