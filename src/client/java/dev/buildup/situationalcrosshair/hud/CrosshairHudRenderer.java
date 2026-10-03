package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.BuildupSituationalCrosshairClient;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation;
import dev.buildup.situationalcrosshair.presentation.TransitionController;
import dev.buildup.situationalcrosshair.presentation.TransitionMode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.Supplier;

public final class CrosshairHudRenderer {
    private static final Identifier VANILLA_CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Map<ClassicCrosshairType, Identifier> TEXTURES = Map.of(
            ClassicCrosshairType.DOT, texture("dot"),
            ClassicCrosshairType.BLOCK, texture("block"),
            ClassicCrosshairType.ATTACK, texture("attack"),
            ClassicCrosshairType.ERROR, texture("error"));

    // Only accessed synchronously during the HUD extraction on the client thread.
    private static CrosshairPresentation pending;
    private static boolean vanillaRequestedCrosshair;
    private static final TransitionController TRANSITION = new TransitionController();

    private CrosshairHudRenderer() { }

    public static void register(Supplier<CrosshairPresentation> presentation, Supplier<TransitionMode> animation) {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
            pending = presentation.get();
            vanillaRequestedCrosshair = false;
            try {
                // Vanilla owns visibility, spectator checks, debug behavior and
                // attack indicators. The mixin suppresses only its central sprite.
                original.extractRenderState(graphics, delta);
                if (vanillaRequestedCrosshair) {
                    var frame = TRANSITION.update(pending, animation.get(), System.nanoTime());
                    int x = (graphics.guiWidth() - 15) / 2;
                    int y = (graphics.guiHeight() - 15) / 2;
                    drawPresentation(graphics, frame, x, y);
                } else {
                    TRANSITION.reset();
                }
            } finally {
                pending = null;
                vanillaRequestedCrosshair = false;
            }
        });
    }

    /** Shared drawing path for one resolved frame; coordinates are GUI pixels. */
    public static void drawPresentation(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            TransitionController.Frame frame, int x, int y) {
        var presentation = frame.presentation();
        if (!presentation.customVisible()) return;
        graphics.blit(RenderPipelines.CROSSHAIR, TEXTURES.get(presentation.base()), x, y,
                0, 0, 15, 15, 15, 15);
        CrosshairDecorationRenderer.draw(graphics, frame, x, y);
    }

    /** Called at the exact vanilla sprite draw; no target detection in the mixin. */
    public static boolean shouldDrawVanillaSprite(Identifier sprite) {
        if (pending != null && pending.customVisible() && VANILLA_CROSSHAIR.equals(sprite)) {
            vanillaRequestedCrosshair = true;
            return false;
        }
        return true;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(BuildupSituationalCrosshairClient.MOD_ID,
                "textures/gui/crosshair_" + name + ".png");
    }
}
