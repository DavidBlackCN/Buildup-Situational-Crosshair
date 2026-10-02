package dev.buildup.situationalcrosshair.semantic;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.Objects;

/** One resolution cycle. Identical proposals deduplicate; conflicting evidence is retained. */
public final class CandidateCollector {
    static final Comparator<ActionCandidate> ORDER = Comparator
            .comparing(ActionCandidate::specificity)
            .thenComparing(ActionCandidate::source)
            .thenComparing(Comparator.comparingInt(ActionCandidate::priority).reversed())
            .thenComparing(ActionCandidate::confidence)
            .thenComparing(ActionCandidate::origin)
            .thenComparing(ActionCandidate::slot)
            .thenComparing(ActionCandidate::action)
            .thenComparing(ActionCandidate::state);
    private final TreeSet<ActionCandidate> candidates = new TreeSet<>(ORDER);

    public void add(ActionCandidate candidate) {
        Objects.requireNonNull(candidate);
        if (!candidate.action().supports(candidate.slot())) throw new IllegalArgumentException("Illegal slot/action");
        candidates.add(candidate);
    }

    public List<ActionCandidate> candidates() { return List.copyOf(candidates); }
}
