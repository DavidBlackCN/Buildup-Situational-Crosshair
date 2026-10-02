package dev.buildup.situationalcrosshair.rules;

import dev.buildup.situationalcrosshair.semantic.*;
import java.util.*;
import java.util.function.Predicate;

/** Immutable reload product. One selective anchor per rule, including both ID/tag OR branches. */
public final class CompiledRuleSet {
    public static final CompiledRuleSet EMPTY = new CompiledRuleSet(List.of(), ignored -> false);
    private final Map<String, List<Rule>> index;
    private final int size;

    public CompiledRuleSet(Collection<Rule> rules, Predicate<String> modLoaded) {
        var buckets = new HashMap<String, List<Rule>>();
        var ids = new HashSet<String>();
        int active = 0;
        for (var rule : rules) {
            if (!ids.add(rule.id())) throw new IllegalArgumentException("Duplicate rule ID: " + rule.id());
            if (!rule.requiredMods().stream().allMatch(modLoaded)) continue;
            active++;
            for (var key : anchors(rule)) buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rule);
        }
        var frozen = new HashMap<String, List<Rule>>();
        buckets.forEach((key, list) -> frozen.put(key, list.stream().sorted(Comparator.comparing(Rule::id)).toList()));
        index = Map.copyOf(frozen);
        size = active;
    }
    public int size() { return size; }

    public List<Rule> eligible(CrosshairContextSnapshot context) {
        var keys = new HashSet<String>();
        keys.add("generic"); keys.add("type:" + context.target());
        var facts = context.ruleFacts();
        factKeys(keys, "target", facts.target());
        factKeys(keys, "item", facts.main()); factKeys(keys, "item", facts.off());
        var candidates = new TreeMap<String, Rule>();
        for (var key : keys) for (var rule : index.getOrDefault(key, List.of())) candidates.put(rule.id(), rule);
        return List.copyOf(candidates.values());
    }

    /** DENY filters all modes. A surviving override owns only its emitted slot. */
    public List<ActionCandidate> apply(CrosshairContextSnapshot context, List<ActionCandidate> nativeCandidates) {
        if (size == 0) return nativeCandidates;
        var matched = eligible(context).stream().filter(r -> r.matches(context)).toList();
        var denies = matched.stream().filter(r -> r.mode() == Rule.Mode.DENY).flatMap(r -> r.emit().stream()).toList();
        Predicate<ActionCandidate> allowed = candidate -> denies.stream().noneMatch(e -> e.slot() == candidate.slot()
                && (e.action() == null || e.action() == candidate.action()) && (e.state() == null || e.state() == candidate.state()));
        var normal = new ArrayList<>(nativeCandidates);
        var overrides = new ArrayList<ActionCandidate>();
        var fallback = new ArrayList<ActionCandidate>();
        for (var rule : matched) {
            if (rule.mode() == Rule.Mode.DENY) continue;
            var destination = switch (rule.mode()) {
                case AUGMENT -> normal;
                case OVERRIDE -> overrides;
                case FALLBACK -> fallback;
                default -> throw new AssertionError();
            };
            for (var emission : rule.emit()) destination.add(rule.candidate(emission, context));
        }
        var result = new CandidateCollector();
        for (var slot : ActionSlot.values()) {
            var forced = overrides.stream().filter(c -> c.slot() == slot).filter(allowed).toList();
            if (!forced.isEmpty()) { forced.forEach(result::add); continue; }
            var ordinary = normal.stream().filter(c -> c.slot() == slot).filter(allowed).toList();
            var selected = new ArrayList<>(ordinary);
            boolean clear = ordinary.stream().anyMatch(CompiledRuleSet::clear);
            if (!clear) fallback.stream().filter(c -> c.slot() == slot).filter(allowed).forEach(selected::add);
            boolean hasAction = selected.stream().anyMatch(CompiledRuleSet::clear);
            // Native NONE is an absence marker, not stronger evidence against a generic pack hint.
            selected.stream().filter(c -> !hasAction || c.action() != ActionKind.NONE
                    || c.source() != CandidateSource.VANILLA_RUNTIME).forEach(result::add);
        }
        return result.candidates();
    }

    private static boolean clear(ActionCandidate candidate) {
        return (candidate.action() != ActionKind.NONE || candidate.source() != CandidateSource.VANILLA_RUNTIME)
                && candidate.confidence().ordinal() <= Confidence.STRONG.ordinal();
    }

    private static Set<String> anchors(Rule rule) {
        var keys = identityAnchors("target", rule.target());
        if (!keys.isEmpty()) return keys;
        keys = identityAnchors("item", rule.held());
        if (!keys.isEmpty()) return keys;
        if (rule.target().namespace() != null) return Set.of("target:ns:" + rule.target().namespace());
        if (rule.held().namespace() != null) return Set.of("item:ns:" + rule.held().namespace());
        if (rule.targetType() != null) return Set.of("type:" + rule.targetType());
        return Set.of("generic");
    }
    private static Set<String> identityAnchors(String prefix, Rule.Selector selector) {
        var keys = new HashSet<String>();
        for (var id : selector.ids()) keys.add(prefix + ":id:" + id);
        for (var tag : selector.tags()) keys.add(prefix + ":tag:" + tag);
        return keys;
    }
    private static void factKeys(Set<String> keys, String prefix, RuleFacts.Identity identity) {
        if (!identity.id().isEmpty()) { keys.add(prefix + ":id:" + identity.id()); keys.add(prefix + ":ns:" + identity.namespace()); }
        for (var tag : identity.tags()) keys.add(prefix + ":tag:" + tag);
    }
}
