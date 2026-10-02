package dev.buildup.situationalcrosshair;

import net.fabricmc.api.ClientModInitializer;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

public final class BuildupSituationalCrosshairClient implements ClientModInitializer {
    public static final String MOD_ID = "buildup_situational_crosshair";

    @Override
    public void onInitializeClient() {
        dev.buildup.situationalcrosshair.rules.RuleManager.register();
        CrosshairHudRenderer.register(() -> ClassicCrosshairResolver.resolve(Minecraft.getInstance()));
        LoggerFactory.getLogger(MOD_ID).info("Buildup Situational Crosshair loaded (JSON Rules v0.1 / Classic Theme)");
    }
}
