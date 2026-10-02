package dev.buildup.situationalcrosshair.rules;

import dev.buildup.situationalcrosshair.semantic.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validated, immutable v0.1 rule. Constructed by RuleParser at reload time. */
public record Rule(String id, Mode mode, int priority, Set<String> requiredMods,
                   Selector target, TargetType targetType, Map<String, String> properties,
                   Selector held, Hand hand, Boolean sneaking, Boolean creative, Boolean usingItem,
                   List<Emission> emit) {
    public enum Mode { AUGMENT, OVERRIDE, DENY, FALLBACK }
    public enum Hand { MAIN, OFF, EITHER }
    public record Selector(Set<String> ids, Set<String> tags, String namespace) {
        public Selector { ids = Set.copyOf(ids); tags = Set.copyOf(tags); }
        public boolean matches(RuleFacts.Identity fact) {
            return (namespace == null || namespace.equals(fact.namespace()))
                    && (ids.isEmpty() && tags.isEmpty() || ids.contains(fact.id())
                    || fact.tags().stream().anyMatch(tags::contains));
        }
        int strength(RuleFacts.Identity fact) {
            if (ids.contains(fact.id())) return 3;
            if (fact.tags().stream().anyMatch(tags::contains)) return 2;
            return namespace != null ? 1 : 0;
        }
    }
    /** null action means deny wildcard; null state means any state in deny mode. */
    public record Emission(ActionSlot slot, ActionKind action, ActionState state) { }
    public Rule { requiredMods = Set.copyOf(requiredMods); properties = Map.copyOf(properties); emit = List.copyOf(emit); }

    public boolean matches(CrosshairContextSnapshot context) {
        var facts = context.ruleFacts();
        return (targetType == null || targetType == context.target()) && target.matches(facts.target())
                && properties.entrySet().stream().allMatch(e -> e.getValue().equals(facts.properties().get(e.getKey())))
                && ((hand != Hand.OFF && held.matches(facts.main())) || (hand != Hand.MAIN && held.matches(facts.off())))
                && (sneaking == null || sneaking == facts.sneaking())
                && (creative == null || creative == context.creative())
                && (usingItem == null || usingItem == facts.usingItem());
    }

    public Specificity specificity(CrosshairContextSnapshot context) {
        var facts = context.ruleFacts();
        int t = target.strength(facts.target());
        int h = Math.max(hand != Hand.OFF && held.matches(facts.main()) ? held.strength(facts.main()) : 0,
                hand != Hand.MAIN && held.matches(facts.off()) ? held.strength(facts.off()) : 0);
        // Classify the branch that actually matched; an unrelated exact ID cannot upgrade a tag match.
        if (t == 3 && h == 3) return !properties.isEmpty() || sneaking != null || creative != null || usingItem != null
                ? Specificity.EXACT_CONTEXT : Specificity.EXACT_ITEM_TARGET;
        if (t == 3) return Specificity.EXACT_TARGET;
        if (h == 3) return Specificity.EXACT_ITEM;
        if (t == 2 && h == 2) return Specificity.TAG_PAIR;
        if (t == 2 || h == 2) return Specificity.SINGLE_TAG;
        if (t == 1 || h == 1) return Specificity.NAMESPACE;
        return Specificity.GENERIC;
    }

    public ActionCandidate candidate(Emission emission, CrosshairContextSnapshot context) {
        return new ActionCandidate(emission.slot(), emission.action(), emission.state(), CandidateSource.PACK_RULE,
                specificity(context), Confidence.STRONG, priority, id);
    }
}
