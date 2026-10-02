package dev.buildup.situationalcrosshair.semantic;

public final class EntityAttackProvider implements CandidateProvider {
    @Override
    public void collect(CrosshairContextSnapshot context, CandidateCollector collector) {
        if (context.target() != TargetType.ENTITY) return;
        // Exact entity-hit channel classification, not a guarantee of damage permission.
        collector.add(new ActionCandidate(ActionSlot.PRIMARY, ActionKind.ATTACK,
                context.spectator() ? ActionState.BLOCKED : context.attackCooling() ? ActionState.COOLDOWN : ActionState.NORMAL,
                CandidateSource.VANILLA_RUNTIME, Specificity.EXACT_TARGET, Confidence.EXACT, 0,
                "buildup_situational_crosshair:entity_attack"));
    }
}
