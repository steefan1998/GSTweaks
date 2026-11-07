// Workaround for Thaumaturge 1.1.0 CraftingAspectContributor.outputOf():
// assemble(EMPTY) may return null for a custom crafting/arcane recipe.
// Thaumaturge catches exceptions, but dereferences null outside that catch.
// Quarantine only that case. EMPTY results and thrown exceptions are safe for
// Thaumaturge and must not disable otherwise valid dynamic recipes.
// Run after the pack's recipe changes. Removed IDs appear in logs/kubejs/server.log.

const GSPACK_GUARD_CRAFTING = Java.loadClass('net.minecraft.world.item.crafting.CraftingRecipe')
const GSPACK_GUARD_INPUT = Java.loadClass('net.minecraft.world.item.crafting.CraftingInput')

ServerEvents.recipes(event => {
  if (!Platform.isLoaded('thaumaturge')) return
  const GSPACK_GUARD_ARCANE = Java.loadClass('com.leclowndu93150.thaumaturge.api.recipe.IArcaneRecipe')
  const GSPACK_GUARD_ARCANE_INPUT = Java.loadClass('com.leclowndu93150.thaumaturge.content.recipe.workbench.ArcaneCraftingInput')

  let quarantined = 0
  event.forEachRecipe({}, recipe => {
    let nullOutput = false
    try {
      let original = recipe.getOriginalRecipe()
      if (original == null) return
      // Match Thaumaturge's precedence for recipes implementing both interfaces.
      if (original instanceof GSPACK_GUARD_ARCANE) {
        nullOutput = original.assemble(GSPACK_GUARD_ARCANE_INPUT.EMPTY) === null
      } else if (original instanceof GSPACK_GUARD_CRAFTING) {
        nullOutput = original.assemble(GSPACK_GUARD_INPUT.EMPTY) === null
      }
    } catch (error) {
      // Thaumaturge already handles this case by treating it as ItemStack.EMPTY.
      return
    }
    if (nullOutput) {
      recipe.remove()
      quarantined++
      console.warn(`[GSPack] Quarantined recipe ${recipe.id}: assemble(EMPTY) returned null and would crash Thaumaturge's aspect index. This recipe is temporarily unavailable.`)
    }
  })
  console.info(`[GSPack] Thaumaturge null-output guard: quarantined ${quarantined} recipes.`)
})
