package dev.buildup.situationalcrosshair.mixin;

import dev.buildup.situationalcrosshair.hud.CrosshairBackground;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class CrosshairBackgroundMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void sampleBeforeHud(CallbackInfo ci) { CrosshairBackground.beforeGui(); }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeSampler(CallbackInfo ci) { CrosshairBackground.close(); }
}
