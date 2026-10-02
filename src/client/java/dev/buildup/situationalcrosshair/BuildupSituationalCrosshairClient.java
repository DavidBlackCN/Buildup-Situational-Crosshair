package dev.buildup.situationalcrosshair;

import net.fabricmc.api.ClientModInitializer;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import org.slf4j.LoggerFactory;

public final class BuildupSituationalCrosshairClient implements ClientModInitializer {
    public static final String MOD_ID = "buildup_situational_crosshair";

    @Override
    public void onInitializeClient() {
        CrosshairHudRenderer.register();
        LoggerFactory.getLogger(MOD_ID).info("Buildup Situational Crosshair loaded (Classic Theme)");
    }
}
