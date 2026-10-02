package dev.buildup.situationalcrosshair;

import net.fabricmc.api.ClientModInitializer;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.presentation.*;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

public final class BuildupSituationalCrosshairClient implements ClientModInitializer {
    public static final String MOD_ID = "buildup_situational_crosshair";

    @Override
    public void onInitializeClient() {
        dev.buildup.situationalcrosshair.rules.RuleManager.register();
        PresentationCommands.register();
        CrosshairHudRenderer.register(() -> PresentationResolver.resolve(
                ClassicCrosshairResolver.semanticState(Minecraft.getInstance()), PresentationOptions.theme()),
                PresentationOptions::animation);
        LoggerFactory.getLogger(MOD_ID).info("Buildup Situational Crosshair loaded (Classic+ / JSON Rules v0.1)");
    }
}
