package dev.buildup.situationalcrosshair.semantic;

import java.util.List;
import java.util.function.Supplier;
import dev.buildup.situationalcrosshair.rules.CompiledRuleSet;
import dev.buildup.situationalcrosshair.semantic.CrosshairSemanticState.ResolvedAction;

public final class CrosshairResolver {
    private final List<CandidateProvider> providers;
    private final Supplier<CompiledRuleSet> rules;

    public CrosshairResolver(List<CandidateProvider> providers) { this(providers, () -> CompiledRuleSet.EMPTY); }
    public CrosshairResolver(List<CandidateProvider> providers, Supplier<CompiledRuleSet> rules) {
        this.providers = List.copyOf(providers);
        this.rules = java.util.Objects.requireNonNull(rules);
    }

    public CrosshairSemanticState resolve(CrosshairContextSnapshot context) {
        // Visibility is a hard gate: providers do not run for unavailable/hidden contexts.
        if (context.visibility() != Visibility.SHOW) {
            return new CrosshairSemanticState(context.target(), context.visibility(),
                    ResolvedAction.none(), ResolvedAction.none(), List.of(), context.mainHand(), context.offHand());
        }
        var collector = new CandidateCollector();
        for (var provider : providers) provider.collect(context, collector);
        var candidates = rules.get().apply(context, collector.candidates());
        return new CrosshairSemanticState(context.target(), Visibility.SHOW,
                resolveSlot(candidates, ActionSlot.PRIMARY), resolveSlot(candidates, ActionSlot.SECONDARY), candidates,
                context.mainHand(), context.offHand());
    }

    private static ResolvedAction resolveSlot(List<ActionCandidate> candidates, ActionSlot slot) {
        return candidates.stream().filter(c -> c.slot() == slot)
                // Inferred/unknown proposals cannot become strong gameplay hints.
                .filter(c -> c.confidence().ordinal() <= Confidence.STRONG.ordinal())
                .findFirst().map(ResolvedAction::of).orElseGet(ResolvedAction::none);
    }
}
