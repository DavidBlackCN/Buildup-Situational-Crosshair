package dev.buildup.situationalcrosshair.crosshair;

import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.util.Arrays;

public final class CrosshairTransitionTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        var masks = new float[4][225];
        for (var type : ClassicCrosshairType.values()) {
            var image = ImageIO.read(Path.of("src/main/resources/assets/buildup_situational_crosshair/textures/gui/crosshair_"
                    + type.name().toLowerCase(java.util.Locale.ROOT) + ".png").toFile());
            for (int y = 0; y < 15; y++) for (int x = 0; x < 15; x++)
                masks[type.ordinal()][y * 15 + x] = (image.getRGB(x, y) >>> 24) / 255f;
        }
        for (var mode : CrosshairTransition.Mode.values()) for (var a : ClassicCrosshairType.values())
            for (var b : ClassicCrosshairType.values()) {
                var c = new CrosshairTransition(masks);
                check(Arrays.equals(c.update(a, mode, 0), masks[a.ordinal()]), "first frame snaps");
                c.update(b, mode, 0);
                for (int ms : new int[]{0, 10, 35, 65, 100, 139, 140, 200}) {
                    var frame = c.update(b, mode, ms * 1_000_000L);
                    if (a != b && mode != CrosshairTransition.Mode.OFF)
                        check(c.active() == (ms < 140), "all shape transitions last 140ms");
                    check(frame[112] == 1, "center never fades");
                    for (int i = 0; i < frame.length; i++) {
                        check(Float.isFinite(frame[i]) && frame[i] >= 0 && frame[i] <= 1, "bounded coverage");
                        if (a == ClassicCrosshairType.BLOCK && b == ClassicCrosshairType.ERROR && masks[a.ordinal()][i] == 1)
                            check(frame[i] == 1, "error keeps shared outline");
                    }
                }
                check(Arrays.equals(c.update(b, mode, 200_000_000), masks[b.ordinal()]), "exact destination");
                check(!c.active(), "stationary path restored");
            }
        for (var mode : new CrosshairTransition.Mode[]{CrosshairTransition.Mode.FADE, CrosshairTransition.Mode.EXPAND}) {
            var c = new CrosshairTransition(masks);
            c.update(ClassicCrosshairType.DOT, mode, 0);
            c.update(ClassicCrosshairType.BLOCK, mode, 0);
            for (int n = 1; n < 50; n++) {
                long now = n * 5_000_000L;
                var currentTarget = n == 1 ? ClassicCrosshairType.BLOCK : ClassicCrosshairType.values()[(n - 1) % 4];
                var before = c.update(currentTarget, mode, now).clone();
                var after = c.update(ClassicCrosshairType.values()[n % 4], mode, now);
                check(Arrays.equals(before, after), "interruption must be continuous");
            }
            c.reset();
            check(Arrays.equals(c.update(ClassicCrosshairType.ERROR, mode, 0), masks[3]), "reset has no stale frame");
            c.update(ClassicCrosshairType.DOT, mode, 0);
            check(Arrays.equals(c.update(ClassicCrosshairType.BLOCK, CrosshairTransition.Mode.OFF, 1), masks[1]), "off is immediate");
            c.update(null, mode, 2);
            check(!c.active(), "Vanilla fallback clears transition");
        }
        System.out.println("PASS: " + checks + " crosshair animation assertions");
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
