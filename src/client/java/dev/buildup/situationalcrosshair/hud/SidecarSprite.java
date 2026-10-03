package dev.buildup.situationalcrosshair.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Cached resource metadata. Texture loading/filtering belongs to Minecraft. */
public record SidecarSprite(Identifier texture, int size) {
    public void draw(GuiGraphicsExtractor graphics, int x, int y, float weight) {
        // CROSSHAIR uses INVERT (1-destination, 1-source) RGB blending. Alpha
        // alone does not attenuate it. Multiply RGB as well to genuinely fade
        // the inversion towards the scenery, rather than change its material.
        int channel = Math.round(255 * Math.clamp(weight, 0, 1));
        if (channel == 0) return;
        int color = channel << 24 | channel << 16 | channel << 8 | channel;
        graphics.blit(RenderPipelines.CROSSHAIR, texture, x, y, 0, 0, size, size, size, size, color);
    }
}
