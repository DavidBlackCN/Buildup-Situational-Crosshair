package dev.buildup.situationalcrosshair.config;

import dev.buildup.situationalcrosshair.CrosshairOptions;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class CrosshairSettings {
    private CrosshairSettings() { }
    public static Screen createScreen(Screen parent) { return createConfig().generateScreen(parent); }

    public static YetAnotherConfigLib createConfig() {
        // YACL owns pending values; bindings update this draft, not live rendering, until Save.
        CrosshairConfig[] draft = {CrosshairOptions.snapshot()};
        var defaults = CrosshairConfig.DEFAULT;
        return YetAnotherConfigLib.createBuilder().title(label("title"))
                .category(ConfigCategory.createBuilder().name(label("title"))
                        .option(Option.<CrosshairConfig.ColorMode>createBuilder()
                                .name(label("color")).description(OptionDescription.of(label("color.description")))
                                .binding(defaults.colorMode(), () -> draft[0].colorMode(), value -> draft[0] =
                                        new CrosshairConfig(draft[0].enabled(), draft[0].animation(), value))
                                .controller(option -> EnumControllerBuilder.create(option).enumClass(CrosshairConfig.ColorMode.class)
                                        .formatValue(value -> label("color." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(label("enabled")).description(OptionDescription.of(label("enabled.description")))
                                .binding(defaults.enabled(), () -> draft[0].enabled(), value -> draft[0] =
                                        new CrosshairConfig(value, draft[0].animation(), draft[0].colorMode()))
                                .controller(TickBoxControllerBuilder::create).build())
                        .option(Option.<Mode>createBuilder()
                                .name(label("animation")).description(OptionDescription.of(label("animation.description")))
                                .binding(defaults.animation(), () -> draft[0].animation(), value -> draft[0] =
                                        new CrosshairConfig(draft[0].enabled(), value, draft[0].colorMode()))
                                .controller(option -> EnumControllerBuilder.create(option).enumClass(Mode.class)
                                        .formatValue(value -> label("animation." + value.name().toLowerCase(Locale.ROOT))))
                                .build())
                        .build())
                .save(() -> {
                    try { CrosshairOptions.save(draft[0]); }
                    catch (java.io.IOException ex) {
                        org.slf4j.LoggerFactory.getLogger("buildup_situational_crosshair").warn("Could not save crosshair options", ex);
                        if (Minecraft.getInstance().gui.screen() instanceof YACLScreen screen)
                            screen.setSaveButtonMessage(label("save_failed"), label("save_failed.description"));
                    }
                }).build();
    }

    private static Component label(String key) { return Component.translatable("buildup_situational_crosshair.settings." + key); }
}
