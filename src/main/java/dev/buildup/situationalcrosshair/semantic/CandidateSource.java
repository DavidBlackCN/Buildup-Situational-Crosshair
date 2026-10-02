package dev.buildup.situationalcrosshair.semantic;

/** Declaration order is descending authority within the same specificity. */
public enum CandidateSource {
    VANILLA_RUNTIME, VANILLA_COMPONENT, MOD_API, BUILTIN_COMPAT,
    PACK_RULE, TAG_RULE, GENERIC_INFERENCE, UNKNOWN
}
