package dev.buildup.situationalcrosshair.crosshair;

import java.util.List;
import net.minecraft.client.Minecraft;
import dev.buildup.situationalcrosshair.semantic.*;

/** Compatibility facade for the unchanged HUD boundary. */
public final class ClassicCrosshairResolver {
    private static final CrosshairResolver SEMANTICS = new CrosshairResolver(List.of(
            new BaseTargetProvider(), new HarvestProvider(), new EntityAttackProvider(), new VanillaUseProvider()),
            dev.buildup.situationalcrosshair.rules.RuleManager::current);

    private ClassicCrosshairResolver() { }

    public static CrosshairSemanticState semanticState(Minecraft client) {
        return SEMANTICS.resolve(ContextCapture.capture(client));
    }

    public static ClassicCrosshairType resolve(Minecraft client) {
        return ClassicPresentation.map(semanticState(client));
    }
}
