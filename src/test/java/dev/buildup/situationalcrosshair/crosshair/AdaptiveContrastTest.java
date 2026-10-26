package dev.buildup.situationalcrosshair.crosshair;

import java.nio.ByteBuffer;
import dev.buildup.situationalcrosshair.config.CrosshairConfig;
import java.nio.file.Files;

public final class AdaptiveContrastTest {
    public static void main(String[] args) throws Exception {
        for (double start : new double[]{.10, .35}) {
            var selector = new AdaptiveContrast();
            Object face = "same textured block face";
            int tone = selector.accept(start, 0, face);
            for (long ms = 50; ms <= 700; ms += 50) selector.accept(start, ms * 1_000_000, face);
            // Sweeps and long pauses over orange leaves / contrasting pixels on one surface.
            for (long ms = 750; ms < 7000; ms += 50)
                expect(selector.accept((ms / 600) % 2 == 0 ? .04 : .50, ms * 1_000_000, face), tone, "same-face texture stability");
            for (long ms = 7000; ms <= 8000; ms += 50) tone = selector.accept(.003, ms * 1_000_000, face);
            expect(tone, AdaptiveContrast.LIGHT, "same-face darkness escape");
            for (long ms = 8050; ms <= 9200; ms += 50) tone = selector.accept(.60, ms * 1_000_000, face);
            expect(tone, AdaptiveContrast.DARK, "same-face bright lighting escape");
        }
        var selector = new AdaptiveContrast();
        expect(selector.accept(.10, 0, "stone"), AdaptiveContrast.LIGHT, "initial stone");
        // Rapidly traversing blocks must not restart confirmation forever.
        int tone = 0;
        for (long ms = 50; ms <= 700; ms += 50) tone = selector.accept(.60, ms * 1_000_000, "snow" + ms);
        expect(tone, AdaptiveContrast.DARK, "moving across snow");
        selector.reset();
        expect(selector.accept(.60, 0, null), AdaptiveContrast.DARK, "reset");
        for (long ms = 50; ms <= 600; ms += 50) tone = selector.accept(.10, ms * 1_000_000, null);
        expect(tone, AdaptiveContrast.LIGHT, "unanchored air adapts");
        var mixed = ByteBuffer.wrap(new byte[]{0,0,0,-1,-1,-1,-1,-1});
        if (Math.abs(AdaptiveContrast.luminance(mixed) - .5) > .000001 || mixed.position() != 0)
            throw new AssertionError("RGBA average / buffer position");
        var directory = Files.createTempDirectory("crosshair-config-test-");
        var file = directory.resolve("config.json");
        try {
            if (!CrosshairConfig.load(file).equals(CrosshairConfig.DEFAULT)) throw new AssertionError("Missing config defaults");
            for (var mode : CrosshairConfig.ColorMode.values()) for (var animation : CrosshairTransition.Mode.values()) {
                var config = new CrosshairConfig(false, animation, mode);
                config.save(file);
                if (!CrosshairConfig.load(file).equals(config)) throw new AssertionError("Persistence round trip");
            }
            Files.writeString(file, "{\"colorMode\":\"unknown\"}");
            try { CrosshairConfig.load(file); throw new AssertionError("Invalid config accepted"); }
            catch (java.io.IOException expected) { }
            if (!Files.readString(file).contains("unknown")) throw new AssertionError("Invalid config overwritten");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        System.out.println("PASS: same-surface texture stability, lighting changes, moving targets, reset, RGBA and config persistence");
    }
    private static void expect(int actual, int expected, String label) {
        if (actual != expected) throw new AssertionError(label + ": " + Integer.toHexString(actual));
    }
}
