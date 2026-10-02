# Stage 4 — JSON Rule Engine v0.1

## Result: PASS

Version **0.1.0-alpha.4+mc26.3**. Implements the authoritative root development
specification's frozen schema (§6) and Stage 4 (§§4.1–4.10). Stage 5 is not started.
Stage 3's outstanding manual acceptance remains outstanding; this report does
not retroactively mark it complete.

Toolchain unchanged: Minecraft 26.3, JDK 25.0.3, Loader 0.19.5, Fabric API
0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1. No extra runtime framework dependency.
Parsing uses Minecraft's existing Gson dependency.

## Decisions

- `RuleParser` validates strict JSON and the fixed schema. Unknown/duplicate keys,
  illegal slots/actions, non-deny wildcards, bad priorities, selectors and schema
  versions reject a file with its derived ID and pack recorded in logs.
- `RuleManager` registers a client resource listener through the actual 26.3
  `ResourceLoader` API. `SimplePreparableReloadListener` prepares/compiles off the
  render path and applies one immutable generation. Resource priority follows
  Minecraft's own `listResources`; no stale rules survive removal or invalidation.
- `RuleFactsCapture` contributes only immutable identity/tag/property/player
  values to the existing shared snapshot. Current synced tags are read rather
  than cached across sessions. Zero active rules skips this capture work.
- `CompiledRuleSet` filters missing optional mods and indexes target/item IDs,
  tags, namespaces and generic/type buckets. Matching correctly retains both
  ID/tag alternative branches; full matching does not scan unrelated exact rules.
- Resolver order is DENY → per-slot surviving OVERRIDE → ordinary resolution →
  per-slot FALLBACK when no reliable result remains. Native NONE is an absence
  marker, not evidence defeating a generic rule. Other slots remain independent.
- Source, specificity and confidence remain separate. Pack candidates are STRONG
  author assertions; ordinary broad high-priority rules cannot beat exact runtime
  behavior merely through a numeric priority. Stable IDs break ties.
- Renderer, HUD Mixin, textures and license split are unchanged. No rules are
  activated in the production jar by default. No Stage 5+ systems were added.

API signatures were checked against resolved 26.3 Minecraft and Fabric jars,
including ResourceLoader, SimplePreparableReloadListener, ResourceManager and
the new TypedInstance/typeHolder tag path. The resolved Fabric API module is
`net.fabricmc.fabric-api:fabric-resource-loader-v1:3.0.4+fcdff87f5d`.
Current online API pages could not be retrieved; implementation was verified
against the installed official Maven artifacts and actual client execution.

## Validation

Final build/client evidence is retained under `docs/validation/`:
`stage-4-build-gametest.log` and `stage-4-client.log`.

- `clean build runClientGameTest`: successful; production jar generated.
- Existing 85 semantic contracts, 16 Classic cases, 7 HUD cases and 44 vanilla
  capability checks pass unchanged.
- **88 rule contract checks** cover required sample fixtures (wrench fallback, exact override,
  deny), all four modes, wildcard/state denies, source authority, explicit NONE,
  slot independence, hand/player/property matching, actual matched specificity,
  missing mods, strict JSON errors and shuffled deterministic ordering.
- A 2,001-rule synthetic set selects exactly one rule for complete matching;
  2,000 unrelated exact target rules never enter the full matcher.
- **19 real client reload checks** create two temporary resource packs, activate them through
  the native repository, and call the real resource reload lifecycle. They verify
  path-derived IDs, higher-pack replacement, invalid-high-pack isolation, lower
  resource exposure after removal, stale rule elimination, optional-mod filtering,
  real block/item tags and properties, offhand/player selectors, entity identity,
  and returning to an empty rule set after pack removal.
- Normal `runClient` launches separately and loads the alpha.4 entrypoint and rule
  listener. No desktop UI automation is used.
- Production jar is client-only, contains the unchanged MIT/third-party license
  split and no test classes or enabled test rules. Four Classic PNG SHA-256 hashes
  still match 1.20.1. Workspace root has no `.git`; all development is in `26.3/`.

## Manual checks still useful

- [ ] A pack author enables a hand-written pack through the resource-pack UI and
  reloads with F3+T, then reviews rule-ID diagnostics in the log.
- [ ] Mixed real modpacks, server tag changes and many broad generic rules.
- [ ] Carry forward Stage 3's natural targeting/visual/manual matrix.

Automated repository/reload tests establish Stage 4's functional acceptance; they
do not claim manual UI testing or large-modpack performance profiling.

## Known limitations

- No shipped compatibility rules; pack authors must assert known behavior.
- Broad generic/type rules still need matching when relevant. The synthetic index
  test demonstrates pruning, not a full modpack frame-time benchmark.
- Rules inspect client-visible registry tags and context only, not server plugins,
  arbitrary component internals or server-side permissions.
- Classic cannot visualize all richer secondary semantics yet.
- Existing offline Realms/test harness warnings remain. Test fixtures intentionally
  produce rule rejection logs; these are expected validation results.

## Next

Stage 5 — Classic+ Presentation.
