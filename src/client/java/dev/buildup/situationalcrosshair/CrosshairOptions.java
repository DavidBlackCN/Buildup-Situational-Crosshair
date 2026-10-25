package dev.buildup.situationalcrosshair;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-thread session setting shared by commands and any future settings screen. */
public final class CrosshairOptions {
    private static boolean enabled = true;
    private CrosshairOptions() { }
    public static boolean enabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

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
                        }))));
    }
}
