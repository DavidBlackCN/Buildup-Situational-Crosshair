package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.presentation.TransitionController;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed sidecar roles and drawing only. No semantic, world or held-item queries. */
public final class CrosshairDecorationRenderer {
    // 3px canvas gaps at rest, >=1px during docking. Envelope [-11,28) x [0,15).
    public static final int LEFT_X = -11, RIGHT_X = 18, LEFT_Y = 3, RIGHT_Y = 2;
    public static final float ACTION_WEIGHT = 0.70f, STATE_WEIGHT = 0.52f;
    private CrosshairDecorationRenderer() { }

    public static void draw(GuiGraphicsExtractor graphics, TransitionController.Frame frame, int x, int y) {
        var p = frame.presentation();
        if (!p.customVisible()) return;
        var left = SidecarSprites.state(p.leftSidecar());
        var right = SidecarSprites.action(p.rightSidecar());
        if (left != null) left.draw(graphics, x + LEFT_X + frame.leftOffset(), y + LEFT_Y, frame.leftOpacity() * STATE_WEIGHT);
        if (right != null) right.draw(graphics, x + RIGHT_X + frame.rightOffset(), y + RIGHT_Y, frame.rightOpacity() * ACTION_WEIGHT);
    }
}
