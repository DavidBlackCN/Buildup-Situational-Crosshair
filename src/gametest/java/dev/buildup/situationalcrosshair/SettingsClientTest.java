package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.config.CrosshairConfig;
import dev.buildup.situationalcrosshair.config.CrosshairConfig.ColorMode;
import dev.buildup.situationalcrosshair.config.CrosshairModMenu;
import dev.buildup.situationalcrosshair.config.CrosshairSettings;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.gui.YACLScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import java.nio.file.Path;

public final class SettingsClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var previous = context.computeOnClient(c -> CrosshairOptions.snapshot());
        int originalScale = context.computeOnClient(c -> c.options.guiScale().get());
        int originalWidth = context.computeOnClient(c -> c.getWindow().getWidth());
        int originalHeight = context.computeOnClient(c -> c.getWindow().getHeight());
        String language = context.computeOnClient(c -> c.getLanguageManager().getSelected());
        try {
            context.runOnClient(c -> save(CrosshairConfig.DEFAULT));
            context.setScreen(() -> new CrosshairModMenu().getModConfigScreenFactory().create(null));
            context.runOnClient(c -> {
                if (!(c.gui.screen() instanceof YACLScreen)) throw new AssertionError("Mod Menu must open YACL");
            });
            var config = context.computeOnClient(c -> CrosshairSettings.createConfig());
            context.setScreen(() -> config.generateScreen(null));
            context.runOnClient(c -> {
                colorOption(config).requestSet(ColorMode.VANILLA_INVERT);
                if (!CrosshairOptions.adaptiveContrast()) throw new AssertionError("Pending setting leaked into runtime");
                ((YACLScreen)c.gui.screen()).finishOrSave();
                if (CrosshairOptions.adaptiveContrast()) throw new AssertionError("YACL save did not apply");
                CrosshairOptions.setAdaptiveContrast(true);
                CrosshairOptions.load();
                if (CrosshairOptions.adaptiveContrast()) throw new AssertionError("Saved inversion did not reload");
                colorOption(config).requestSet(ColorMode.ADAPTIVE);
                ((YACLScreen)c.gui.screen()).cancelOrReset();
                CrosshairOptions.load();
                if (CrosshairOptions.adaptiveContrast()) throw new AssertionError("Cancelled setting was saved");
            });
            var defaults = context.computeOnClient(c -> CrosshairSettings.createConfig());
            context.setScreen(() -> defaults.generateScreen(null));
            context.runOnClient(c -> {
                colorOption(defaults).requestSetDefault();
                ((YACLScreen)c.gui.screen()).finishOrSave();
                CrosshairOptions.load();
                if (!CrosshairOptions.adaptiveContrast()) throw new AssertionError("Reset default must be adaptive");
            });
            context.runOnClient(c -> c.getLanguageManager().setSelected("zh_cn"));
            var reload = context.computeOnClient(c -> c.reloadResourcePacks());
            context.waitFor(c -> reload.isDone() && c.gui.overlay() == null);
            context.runOnClient(c -> reload.join());
            for (int scale : new int[]{2, 3}) {
                context.runOnClient(c -> {
                    c.getWindow().setWidth(1280); c.getWindow().setHeight(960);
                    c.options.guiScale().set(scale); c.resizeGui();
                    if (c.getWindow().getGuiScale() != scale) throw new AssertionError("GUI scale was clamped");
                });
                context.setScreen(() -> new CrosshairModMenu().getModConfigScreenFactory().create(null));
                context.waitTicks(4);
                context.takeScreenshot(TestScreenshotOptions.of("adaptive-yacl-settings-scale-" + scale)
                        .disableCounterPrefix().withSize(1280, 960)
                        .withDestinationDir(Path.of("screenshots").toAbsolutePath()));
            }
        } finally {
            context.runOnClient(c -> {
                c.gui.setScreen(null);
                save(previous);
                c.getWindow().setWidth(originalWidth); c.getWindow().setHeight(originalHeight);
                c.options.guiScale().set(originalScale); c.resizeGui();
                c.getLanguageManager().setSelected(language);
            });
            var restore = context.computeOnClient(c -> c.reloadResourcePacks());
            context.waitFor(c -> restore.isDone() && c.gui.overlay() == null);
            context.runOnClient(c -> restore.join());
        }
        org.slf4j.LoggerFactory.getLogger("crosshair-settings-test").info(
                "PASS: Mod Menu -> YACL, pending/save/cancel/defaults, persisted reload, Chinese GUI scales 2/3");
    }
    @SuppressWarnings("unchecked")
    private static Option<ColorMode> colorOption(YetAnotherConfigLib config) {
        return (Option<ColorMode>)config.categories().getFirst().groups().getFirst().options().getFirst();
    }
    private static void save(CrosshairConfig config) {
        try { CrosshairOptions.save(config); }
        catch (java.io.IOException ex) { throw new AssertionError(ex); }
    }
}
