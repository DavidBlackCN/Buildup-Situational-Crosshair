package dev.buildup.situationalcrosshair.presentation;

/** Session choices only. Persistent settings and GUI belong to Stage 7. */
public final class PresentationOptions {
    private static CrosshairTheme theme = CrosshairTheme.CLASSIC_PLUS;
    private static TransitionMode animation = TransitionMode.SUBTLE;
    private PresentationOptions() { }
    public static CrosshairTheme theme() { return theme; }
    public static TransitionMode animation() { return animation; }
    public static void theme(CrosshairTheme value) { theme = java.util.Objects.requireNonNull(value); }
    public static void animation(TransitionMode value) { animation = java.util.Objects.requireNonNull(value); }
}
