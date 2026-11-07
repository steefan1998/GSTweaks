package nl.gstweaks.daily;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import nl.gstweaks.GSTweaks;

@EventBusSubscriber(modid = GSTweaks.MOD_ID, value = Dist.CLIENT)
final class DailyScreen extends Screen {
    private DailyStatus status;
    private Button claim;
    private int ticks;

    private DailyScreen(DailyStatus status) {
        super(Component.translatable("daily.gstweaks.title"));
        this.status = status;
    }

    @SubscribeEvent
    static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(DailyStatus.TYPE, (status, context) -> {
            var game = net.minecraft.client.Minecraft.getInstance();
            if (game.screen instanceof DailyScreen current) {
                current.status = status;
                if (current.claim != null) current.claim.active = status.canClaim();
            } else if (status.open()) {
                game.setScreen(new DailyScreen(status));
            }
        });
    }

    @Override
    protected void init() {
        claim = addRenderableWidget(Button.builder(Component.translatable("daily.gstweaks.claim"), button -> {
            if (minecraft.player != null) {
                button.active = false;
                minecraft.player.connection.sendCommand("gstweaks daily claim");
            }
        }).bounds(width / 2 - 68, height / 2 + 24, 136, 20).build());
        claim.active = status.canClaim();
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0 && minecraft.player != null) {
            minecraft.player.connection.sendCommand("gstweaks daily status");
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int halfWidth = Math.min(156, width / 2 - 6);
        int x = width / 2;
        int y = height / 2;
        graphics.fill(x - halfWidth, y - 54, x + halfWidth, y + 54, 0xF0121925);
        graphics.outline(x - halfWidth, y - 54, halfWidth * 2, 108, 0xFF536985);
        graphics.centeredText(font, title, x, y - 43, 0xFFFFD865);
        graphics.centeredText(font, status.message(), x, y - 23, 0xFFFFFFFF);
        graphics.centeredText(font, status.progress(), x, y - 8, 0xFFB9D7F0);
        graphics.centeredText(font, status.reset(), x, y + 7, 0xFFADB8C8);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
