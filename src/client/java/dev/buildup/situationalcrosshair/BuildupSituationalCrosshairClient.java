package dev.buildup.situationalcrosshair;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.LoggerFactory;

public final class BuildupSituationalCrosshairClient implements ClientModInitializer {
    public static final String MOD_ID = "buildup_situational_crosshair";

    @Override
    public void onInitializeClient() {
        LoggerFactory.getLogger(MOD_ID).info("Buildup Situational Crosshair loaded (Stage 0)");
    }
}
