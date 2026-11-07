ServerEvents.recipes(event => {
  if (!Platform.isLoaded('gstweaks')) return
  const config = JSON.parse(JsonIO.readString('config/gstweaks/unification.json'))
  if (!config.enabled) return

  config.groups.forEach(({ tag, items }) => {
    const canonical = items.find(item => Item.exists(item))
    if (!canonical) return
    items.forEach(item => {
      if (item === canonical || !Item.exists(item)) return
      event.replaceOutput({ output: item }, item, canonical)
      event.replaceInput({ input: item }, item, `#${tag}`)
    })
  })
})
