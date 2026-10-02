package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.BuildupSituationalCrosshairClient;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Map;

public final class CrosshairHudRenderer {
    private static final Identifier VANILLA_CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Map<ClassicCrosshairType, Identifier> TEXTURES = Map.of(
            ClassicCrosshairType.DOT, texture("dot"),
            ClassicCrosshairType.BLOCK, texture("block"),
            ClassicCrosshairType.ATTACK, texture("attack"),
            ClassicCrosshairType.ERROR, texture("error"));

    // Only accessed synchronously during the HUD extraction on the client thread.
    private static ClassicCrosshairType pending;
    private static boolean vanillaRequestedCrosshair;

    private CrosshairHudRenderer() { }

    public static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
            pending = ClassicCrosshairResolver.resolve(Minecraft.getInstance());
            vanillaRequestedCrosshair = false;
            try {
                // Vanilla owns visibility, spectator checks, debug behavior and
                // attack indicators. The mixin suppresses only its central sprite.
                original.extractRenderState(graphics, delta);
                if (vanillaRequestedCrosshair) {
                    graphics.blit(RenderPipelines.CROSSHAIR, TEXTURES.get(pending),
                            (graphics.guiWidth() - 15) / 2, (graphics.guiHeight() - 15) / 2,
                            0, 0, 15, 15, 15, 15);
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

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(BuildupSituationalCrosshairClient.MOD_ID,
                "textures/gui/crosshair_" + name + ".png");
    }
}
