package dev.buildup.situationalcrosshair.presentation;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class PresentationCommands {
    private PresentationCommands() { }
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(literal("crosshair")
                .then(literal("theme")
                        .then(literal("classic").executes(c -> { PresentationOptions.theme(CrosshairTheme.CLASSIC);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.theme.classic")); return 1; }))
                        .then(literal("classic_plus").executes(c -> { PresentationOptions.theme(CrosshairTheme.CLASSIC_PLUS);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.theme.classic_plus")); return 1; })))
                .then(literal("animation")
                        .then(literal("off").executes(c -> { PresentationOptions.animation(TransitionMode.OFF);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.animation.off")); return 1; }))
                        .then(literal("subtle").executes(c -> { PresentationOptions.animation(TransitionMode.SUBTLE);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.animation.subtle")); return 1; })))));
    }
}
