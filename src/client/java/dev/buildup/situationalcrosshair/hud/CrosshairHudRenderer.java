package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import dev.buildup.situationalcrosshair.CrosshairOptions;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition;
import dev.buildup.situationalcrosshair.crosshair.CrosshairOutline;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import java.util.function.Supplier;

public final class CrosshairHudRenderer {
    private static final Identifier VANILLA_CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static CrosshairTransition transition;
    private static float[][] loadedMasks;
    private static Object level;
    private static final float[] outline = new float[CrosshairOutline.SIZE * CrosshairOutline.SIZE];

    // Only accessed synchronously during the HUD extraction on the client thread.
    private static ClassicCrosshairType pending;
    private static boolean vanillaRequestedCrosshair;

    private CrosshairHudRenderer() { }

    public static void register(Supplier<ClassicCrosshairType> crosshair) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (level != client.level || client.player == null || !CrosshairOptions.enabled()
                    || client.gui.hud.isHidden() || !client.options.getCameraType().isFirstPerson()) resetAnimation();
            level = client.level;
        });
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
            pending = crosshair.get();
            vanillaRequestedCrosshair = false;
            try {
                // Vanilla owns visibility, spectator checks, debug behavior and
                // attack indicators. The mixin suppresses only its central sprite.
                original.extractRenderState(graphics, delta);
                if (vanillaRequestedCrosshair) {
                    int x = (graphics.guiWidth() - 15) / 2;
                    int y = (graphics.guiHeight() - 15) / 2;
                    draw(graphics, pending, x, y, System.nanoTime());
                } else {
                    resetAnimation();
                }
            } finally {
                pending = null;
                vanillaRequestedCrosshair = false;
            }
        });
    }

    /** Called at the exact vanilla sprite draw; no target detection in the mixin. */
    public static boolean shouldDrawVanillaSprite(Identifier sprite) {
        if (pending != null && VANILLA_CROSSHAIR.equals(sprite)) {
            vanillaRequestedCrosshair = true;
            return false;
        }
        return true;
    }

    public static void resetAnimation() {
        if (transition != null) transition.reset();
    }

    /** Also used by deterministic client visual checks. Positions are GUI pixels. */
    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                            ClassicCrosshairType type, int x, int y, long now) {
        var masks = CrosshairTextures.masks();
        if (loadedMasks != masks) {
            loadedMasks = masks;
            transition = masks == null ? null : new CrosshairTransition(masks);
        }
        if (transition == null) {
            graphics.blit(RenderPipelines.CROSSHAIR, CrosshairTextures.texture(type), x, y, 0, 0, 15, 15, 15, 15);
            return;
        }
        var frame = transition.update(type, CrosshairOptions.animation(), now);
        if (CrosshairOptions.outline()) {
            CrosshairOutline.write(frame, outline);
            for (int i = 0; i < outline.length; i++) {
                int alpha = Math.round(outline[i] * 255);
                if (alpha == 0) continue;
                int px = x + i % CrosshairOutline.SIZE - 1;
                int py = y + i / CrosshairOutline.SIZE - 1;
                // Normal alpha blend, before the inverse body; no darkening under full pixels.
                graphics.fill(px, py, px + 1, py + 1, alpha << 24);
            }
        }
        if (!transition.active()) {
            graphics.blit(RenderPipelines.CROSSHAIR, CrosshairTextures.texture(type), x, y, 0, 0, 15, 15, 15, 15);
            return;
        }
        // Merge coverage BEFORE drawing: one inverse blend per screen pixel.
        // INVERT uses source RGB, so alpha-only fading would not fade its color.
        for (int i = 0; i < frame.length; i++) {
            int strength = Math.clamp(Math.round(frame[i] * 255), 0, 255);
            if (strength == 0) continue;
            int color = 0xff000000 | strength << 16 | strength << 8 | strength;
            graphics.blit(RenderPipelines.CROSSHAIR, CrosshairTextures.texture(ClassicCrosshairType.DOT),
                    x + i % 15, y + i / 15, 7, 7, 1, 1, 15, 15, color);
        }
    }
}
