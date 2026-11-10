package nl.gstweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = GSTweaks.MOD_ID, value = Dist.CLIENT)
final class GSTweaksOptionsScreen extends Screen {
    private boolean woodenToolsOnly;
    private Button toggle;

    private GSTweaksOptionsScreen(boolean woodenToolsOnly) {
        super(Component.translatable("options.gstweaks.title"));
        this.woodenToolsOnly = woodenToolsOnly;
    }

    @SubscribeEvent
    static void register(RegisterClientPayloadHandlersEvent event) {
        if (GSTweaks.loaded("tconstruct")) {
            event.register(GSTweaksOptions.Status.TYPE, (status, context) -> {
                var game = Minecraft.getInstance();
                if (game.screen instanceof GSTweaksOptionsScreen options) {
                    options.update(status.woodenToolsOnly());
                } else if (status.open()) {
                    game.setScreen(new GSTweaksOptionsScreen(status.woodenToolsOnly()));
                }
            });
        }
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(248, width - 24);
        toggle = addRenderableWidget(Button.builder(GSTweaksOptions.woodenToolsStatus(woodenToolsOnly), button -> {
            if (minecraft.player != null) {
                button.active = false;
                minecraft.player.connection.sendCommand("gstweaks options wooden_tools_only " + !woodenToolsOnly);
            }
        }).bounds((width - buttonWidth) / 2, height / 2 + 24, buttonWidth, 20).build());
    }

    private void update(boolean enabled) {
        woodenToolsOnly = enabled;
        if (toggle != null) {
            toggle.setMessage(GSTweaksOptions.woodenToolsStatus(enabled));
            toggle.active = true;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2;
        int y = height / 2;
        int halfWidth = Math.min(180, x - 5);
        graphics.fill(x - halfWidth, y - 47, x + halfWidth, y + 55, 0xF0121925);
        graphics.outline(x - halfWidth, y - 47, halfWidth * 2, 102, 0xFF536985);
        graphics.centeredText(font, title, x, y - 36, 0xFFFFD865);
        graphics.centeredText(font, Component.translatable("options.gstweaks.description"), x, y - 21, 0xFFB9D7F0);
        graphics.centeredText(font, Component.translatable("options.gstweaks.paxels"), x, y - 8, 0xFFB9D7F0);
        graphics.centeredText(font, Component.translatable("options.gstweaks.reload_warning"), x, y + 7, 0xFFFFD865);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
