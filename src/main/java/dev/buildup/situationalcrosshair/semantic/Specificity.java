package dev.buildup.situationalcrosshair.semantic;

/** Declaration order is descending specificity, independent of confidence. */
public enum Specificity {
    EXACT_CONTEXT, EXACT_ITEM_TARGET, EXACT_TARGET, EXACT_ITEM,
    TAG_PAIR, SINGLE_TAG, NAMESPACE, GENERIC
}
