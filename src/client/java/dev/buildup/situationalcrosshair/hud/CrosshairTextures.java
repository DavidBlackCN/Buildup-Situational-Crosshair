package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.BuildupSituationalCrosshairClient;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Locale;

/** Reads the selected resource pack once per reload, never during HUD extraction. */
public final class CrosshairTextures extends SimplePreparableReloadListener<float[][]> {
    private static final CrosshairTextures INSTANCE = new CrosshairTextures();
    private static final Identifier[] TEXTURES = java.util.Arrays.stream(ClassicCrosshairType.values())
            .map(type -> Identifier.fromNamespaceAndPath(BuildupSituationalCrosshairClient.MOD_ID,
                    "textures/gui/crosshair_" + type.name().toLowerCase(Locale.ROOT) + ".png"))
            .toArray(Identifier[]::new);
    private volatile float[][] masks;

    private CrosshairTextures() { }
    public static Identifier texture(ClassicCrosshairType type) { return TEXTURES[type.ordinal()]; }
    public static float[][] masks() { return INSTANCE.masks; }
    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(BuildupSituationalCrosshairClient.MOD_ID, "crosshair_textures"), INSTANCE);
    }

    @Override protected float[][] prepare(ResourceManager resources, ProfilerFiller profiler) {
        var result = new float[TEXTURES.length][225];
        try {
            for (int type = 0; type < TEXTURES.length; type++) {
                try (var stream = resources.getResourceOrThrow(TEXTURES[type]).open()) {
                    var image = ImageIO.read(stream);
                    if (image == null || image.getWidth() != 15 || image.getHeight() != 15)
                        throw new IOException("Animation requires 15x15 Classic textures");
                    for (int y = 0; y < 15; y++) for (int x = 0; x < 15; x++) {
                        int pixel = image.getRGB(x, y);
                        if ((pixel >>> 24) == 0) continue;
                        if (pixel != 0xffffffff) throw new IOException("Animation requires opaque white Classic pixels");
                        result[type][y * 15 + x] = 1;
                    }
                    if (result[type][112] != 1) throw new IOException("Animation requires a shared center pixel");
                }
            }
            for (int i = 0; i < 225; i++) {
                if (i != 112 && result[ClassicCrosshairType.DOT.ordinal()][i] != 0)
                    throw new IOException("Animation requires a single-pixel DOT");
            }
            return result;
        } catch (IOException | RuntimeException ex) {
            LoggerFactory.getLogger(BuildupSituationalCrosshairClient.MOD_ID)
                    .warn("Using immediate crosshair rendering for this resource pack: {}", ex.getMessage());
            return null;
        }
    }

    @Override protected void apply(float[][] prepared, ResourceManager resources, ProfilerFiller profiler) {
        masks = prepared;
        CrosshairHudRenderer.resetAnimation();
        CrosshairBackground.reset();
    }
}
