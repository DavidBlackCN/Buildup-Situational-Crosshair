package dev.buildup.situationalcrosshair;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-thread session setting shared by commands and any future settings screen. */
public final class CrosshairOptions {
    private static boolean enabled = true;
    private static Mode animation = Mode.EXPAND;
    private static boolean outline = true;
    private CrosshairOptions() { }
    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) {
        enabled = value;
        CrosshairHudRenderer.resetAnimation();
    }
    public static Mode animation() { return animation; }
    public static boolean outline() { return outline; }
    public static void setOutline(boolean value) { outline = value; }
    public static void setAnimation(Mode value) {
        animation = java.util.Objects.requireNonNull(value);
        CrosshairHudRenderer.resetAnimation();
    }

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> dispatcher.register(
                literal("crosshair")
                        .then(literal("enable").executes(c -> {
                            setEnabled(true);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.enabled"));
                            return 1;
                        }))
                        .then(literal("disable").executes(c -> {
                            setEnabled(false);
                            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.disabled"));
                            return 1;
                        }))
                        .then(literal("animation")
                                .then(animationCommand("off", Mode.OFF))
                                .then(animationCommand("fade", Mode.FADE))
                                .then(animationCommand("expand", Mode.EXPAND)))
                        .then(literal("outline")
                                .then(outlineCommand("on", true))
                                .then(outlineCommand("off", false)))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>
            outlineCommand(String name, boolean enabled) {
        return literal(name).executes(c -> {
            setOutline(enabled);
            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.outline." + name));
            return 1;
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>
            animationCommand(String name, Mode mode) {
        return literal(name).executes(c -> {
            setAnimation(mode);
            c.getSource().sendFeedback(Component.translatable("buildup_situational_crosshair.animation." + name));
            return 1;
        });
    }
}
