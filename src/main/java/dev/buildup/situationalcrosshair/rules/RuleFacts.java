package dev.buildup.situationalcrosshair.rules;

import java.util.Map;
import java.util.Set;

/** Registry identities only; no live stacks, holders, properties or world references. */
public record RuleFacts(Identity target, Identity main, Identity off,
                        Map<String, String> properties, boolean sneaking, boolean usingItem) {
    public record Identity(String id, Set<String> tags) {
        public static final Identity EMPTY = new Identity("", Set.of());
        public Identity { java.util.Objects.requireNonNull(id); tags = Set.copyOf(tags); }
        public String namespace() { int colon = id.indexOf(':'); return colon < 0 ? "" : id.substring(0, colon); }
    }
    public static final RuleFacts EMPTY = new RuleFacts(Identity.EMPTY, Identity.EMPTY, Identity.EMPTY, Map.of(), false, false);
    public RuleFacts {
        java.util.Objects.requireNonNull(target);
        java.util.Objects.requireNonNull(main);
        java.util.Objects.requireNonNull(off);
        properties = Map.copyOf(properties);
    }
}
