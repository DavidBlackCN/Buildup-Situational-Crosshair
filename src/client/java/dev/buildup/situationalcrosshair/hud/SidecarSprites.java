package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.ActionSidecar;
import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.StateSidecar;
import net.minecraft.resources.Identifier;
import java.util.Map;

/** Presentation roles to original, cached sprite metadata; no semantic enums. */
public final class SidecarSprites {
    private static final Map<ActionSidecar, SidecarSprite> ACTIONS = Map.of(
            ActionSidecar.INTERACT, sprite("interact", 10),
            ActionSidecar.PLACE, sprite("place", 10),
            ActionSidecar.TRANSFORM, sprite("transform", 10),
            ActionSidecar.USE, sprite("use", 10));
    private static final SidecarSprite BLOCKED = sprite("blocked", 8);
    private SidecarSprites() { }
    private static SidecarSprite sprite(String name, int size) {
        return new SidecarSprite(Identifier.fromNamespaceAndPath("buildup_situational_crosshair",
                "textures/gui/sidecar/" + name + ".png"), size);
    }
    public static SidecarSprite action(ActionSidecar role) { return ACTIONS.get(role); }
    public static SidecarSprite state(StateSidecar role) { return role == StateSidecar.BLOCKED ? BLOCKED : null; }
}
