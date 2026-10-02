package dev.buildup.situationalcrosshair.semantic;

/** Internal provider contract, not a public third-party integration API. */
@FunctionalInterface
public interface CandidateProvider {
    void collect(CrosshairContextSnapshot context, CandidateCollector collector);
}
