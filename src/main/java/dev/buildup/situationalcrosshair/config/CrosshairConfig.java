package dev.buildup.situationalcrosshair.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;

public record CrosshairConfig(boolean enabled, Mode animation, ColorMode colorMode) {
    public enum ColorMode { ADAPTIVE, VANILLA_INVERT }
    public static final CrosshairConfig DEFAULT = new CrosshairConfig(true, Mode.EXPAND, ColorMode.ADAPTIVE);
    public CrosshairConfig { Objects.requireNonNull(animation); Objects.requireNonNull(colorMode); }

    public static CrosshairConfig load(Path path) throws IOException {
        if (!Files.exists(path)) return DEFAULT;
        try {
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            boolean enabled = DEFAULT.enabled;
            if (json.has("enabled")) {
                if (!json.get("enabled").isJsonPrimitive() || !json.getAsJsonPrimitive("enabled").isBoolean())
                    throw new IllegalArgumentException("enabled must be a boolean");
                enabled = json.get("enabled").getAsBoolean();
            }
            return new CrosshairConfig(enabled,
                    json.has("animation") ? Mode.valueOf(json.get("animation").getAsString().toUpperCase(Locale.ROOT)) : DEFAULT.animation,
                    json.has("colorMode") ? ColorMode.valueOf(json.get("colorMode").getAsString().toUpperCase(Locale.ROOT)) : DEFAULT.colorMode);
        } catch (RuntimeException ex) {
            throw new IOException("Invalid crosshair configuration: " + path, ex);
        }
    }

    public void save(Path path) throws IOException {
        Path absolute = path.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        var json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("animation", animation.name().toLowerCase(Locale.ROOT));
        json.addProperty("colorMode", colorMode.name().toLowerCase(Locale.ROOT));
        Path temp = Files.createTempFile(absolute.getParent(), "crosshair-", ".tmp");
        try {
            Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n");
            try { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
