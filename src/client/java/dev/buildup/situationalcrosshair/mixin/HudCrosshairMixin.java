package dev.buildup.situationalcrosshair.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * HUD API replaces entire layers, not individual sprites. Cancelling the layer
 * would also remove vanilla attack indicators and bypass internal visibility.
 * This condition targets the named crosshair sprite, never an ordinal; all other
 * sprite draws and the original method remain intact. Rendering lives in the HUD API.
 */
@Mixin(Hud.class)
public abstract class HudCrosshairMixin {
    @WrapWithCondition(method = "extractCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private boolean buildup$keepOtherSprites(GuiGraphicsExtractor graphics, RenderPipeline pipeline,
                                             Identifier sprite, int x, int y, int width, int height) {
        return CrosshairHudRenderer.shouldDrawVanillaSprite(sprite);
    }
}
