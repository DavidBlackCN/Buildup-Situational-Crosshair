# JSON rules v0.1

Rules are client resource-pack files, independent of textures and server datapacks:

```text
assets/<namespace>/crosshair_rules/**/*.json
assets/blackpack/crosshair_rules/example/wrench.json → blackpack:example/wrench
```

Enable the resource pack normally. F3+T/resource reload rebuilds the complete rule
set, including removals. For the same resource path, the highest-priority pack wins.
Different paths are different rules; pack order does not act as candidate priority.
No rules ship enabled by default. The example fixture files under
`src/test/resources/rules/` are tests, not production compatibility claims.

## Example

```json
{
  "schema": 1,
  "mode": "override",
  "priority": 0,
  "requires": {"mods": ["exampletech"]},
  "when": {
    "target": {"type": "block", "ids": ["exampletech:crusher"]},
    "held": {"hand": "main", "ids": ["exampletech:wrench"]},
    "player": {"sneaking": false}
  },
  "emit": [{"slot": "secondary", "action": "transform", "state": "normal"}]
}
```

This asserts behavior supplied by a pack author; it neither executes the action
nor grants permission to perform it. Use exact selectors and only assert known
behavior. It is not a built-in integration with the example mod.

## Fields and matching

`schema: 1`, `mode` and a nonempty `emit` array are required. `priority` defaults
to 0 and must be an integer in [-100,100]. `requires` and `when` are optional.
All enumerated values are lowercase. Unknown fields, duplicate JSON keys, nulls,
comments, malformed IDs and wrong value types reject the entire file.

| Selector | Supported fields | Meaning |
| --- | --- | --- |
| `when.target` | `type`, `ids`, `tags`, `namespace`, `properties` | Current miss/block/entity target |
| `when.held` | `hand`, `ids`, `tags`, `namespace` | Current main/off/either hand; default either |
| `when.player` | `sneaking`, `creative`, `using_item` | Optional booleans |

- Target, held and player selectors combine with AND.
- IDs inside `ids` use OR; tags inside `tags` use OR. If both are present, IDs
  and tags are alternative branches. `namespace`, type and properties still apply.
- ID/tag arrays must be nonempty when supplied. IDs require explicit namespaces;
  tags use `c:tools/wrench`, without a `#` prefix. Missing registry IDs/tags simply
  do not match. Rule files cannot define gameplay registry tags.
- `hand: either` means either complete held selector must match one hand; conditions
  are never split across two hands. Empty hands have no item ID or item tags.
- `properties` requires explicit `target.type: block`. Values are strings such as
  `"waterlogged": "false"`; property entries combine with AND. Unknown properties
  do not match. `miss` cannot have IDs, tags or namespace.
- All listed `requires.mods` must be installed. An absent optional mod makes a
  valid rule inactive, not erroneous. An empty mods array imposes no restriction.

Supported slots/actions:

| Slot | Actions |
| --- | --- |
| `primary` | `none`, `mine`, `attack`, `special` |
| `secondary` | `none`, `interact`, `use`, `place`, `transform`, `special` |

States: `normal`, `charging`, `ready`, `cooldown`, `blocked`, `invalid`.
State defaults to `normal` for emitted candidates. `none` can only have `normal`.
Several emissions may compete in one slot; stable resolver ordering selects one.

## Modes and precedence

| Mode | Behavior |
| --- | --- |
| `augment` | Add ordinary PACK_RULE candidates; cannot bypass native authority through priority alone |
| `fallback` | Participate per slot only if no surviving reliable ordinary action exists |
| `override` | Surviving override candidates replace the competition only in emitted slots |
| `deny` | Remove matching candidate actions from native, augment, override and fallback pools |

Denies run first. In deny mode only, `action: "*"` removes all actions in the
specified slot. Omitted deny state matches every state; an explicit state limits
the denial to that state. Priority cannot undo a deny. If every override emission
for a slot is denied, remaining ordinary/fallback candidates in that slot may win.

Fallback treats native NONE as absence. A reliable action with BLOCKED, INVALID or
COOLDOWN is still a clear result, so fallback does not replace it. Explicit rule
NONE is a deliberate ordinary result; override NONE can suppress a slot. Weak or
unknown-confidence native candidates do not prevent a fallback.

Within each surviving pool, ordering remains specificity → source → priority
(descending) → confidence → rule ID → stable action/state tie-break. All parsed
rule candidates are PACK_RULE/STRONG: a pack author's explicit assertion, not a
runtime proof. Use `fallback` for broad suggestions and reserve `override` for
known corrections. Visibility HIDE/VANILLA gates always precede all rules.

Specificity uses the branch that actually matched: exact target+item, exact target,
exact item, paired tags, single tag, namespace, generic. Exact target+item with
property/player constraints becomes EXACT_CONTEXT. Unmatched IDs cannot elevate
a tag match's specificity. Generic player conditions alone do not become exact
runtime evidence. Conflicting equal overrides resolve by stable rule ID.

## Reload and implementation limits

Validation/JSON parsing and compilation happen during resource preparation; apply
publishes one immutable generation. Errors include rule ID and source pack in the
log. A broken highest-priority file is rejected; the loader does not silently
resurrect a shadowed lower pack's rule or a stale previous-generation rule.

Index anchors cover target/item IDs and tags, namespace, target type and generic
rules. Both ID/tag OR branches are indexed. Full matching only examines relevant
buckets, though deliberately broad generic rules must still be evaluated. Optional
mod filtering occurs before indexing. Context reads current synced registry tags;
no tag result is cached across server changes. With no active rules, rule fact
capture is skipped entirely.

Individual files are limited to 1 Mi characters and JSON nesting depth 16.
No scripting, regex selectors, NBT/component query language, class-name matching,
server synchronization, texture fields, GUI or debug overlay exists in v0.1.
Rules only change semantics. Classic keeps its four existing graphics (including
its historical charged-crossbow visual exception); secondary rules do not add
new visible modifiers. Classic+ presentation belongs to Stage 5.

Run `./gradlew ruleTest` for parser/matcher/resolver/index contracts and
`./gradlew runClientGameTest` for real pack reload tests.
