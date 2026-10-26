package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.config.CrosshairConfig;
import dev.buildup.situationalcrosshair.config.CrosshairConfig.ColorMode;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import dev.buildup.situationalcrosshair.hud.CrosshairBackground;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-thread runtime options. Only explicit settings saves write the config file. */
public final class CrosshairOptions {
    private static boolean enabled = true;
    private static Mode animation = Mode.EXPAND;
    private static boolean adaptiveContrast = true;
    private CrosshairOptions() { }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("buildup_situational_crosshair.json");
    }
    public static void load() {
        try {
            apply(CrosshairConfig.load(configPath()));
            if (!Files.exists(configPath())) snapshot().save(configPath());
        } catch (IOException ex) {
            apply(CrosshairConfig.DEFAULT);
            org.slf4j.LoggerFactory.getLogger(BuildupSituationalCrosshairClient.MOD_ID)
                    .warn("Using default crosshair options; existing config has been preserved", ex);
        }
    }
    public static CrosshairConfig snapshot() {
        return new CrosshairConfig(enabled, animation, adaptiveContrast ? ColorMode.ADAPTIVE : ColorMode.VANILLA_INVERT);
    }
    public static void save(CrosshairConfig config) throws IOException {
        config.save(configPath());
        apply(config);
    }
    public static void apply(CrosshairConfig config) {
        setEnabled(config.enabled());
        setAnimation(config.animation());
        setAdaptiveContrast(config.colorMode() == ColorMode.ADAPTIVE);
    }
    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        CrosshairHudRenderer.resetAnimation();
        CrosshairBackground.reset();
    }
    public static Mode animation() { return animation; }
    public static boolean adaptiveContrast() { return adaptiveContrast; }
    public static void setAdaptiveContrast(boolean value) {
        adaptiveContrast = value;
        CrosshairBackground.reset();
    }
    public static void setAnimation(Mode value) {
        animation = java.util.Objects.requireNonNull(value);
        CrosshairHudRenderer.resetAnimation();
    }
}
