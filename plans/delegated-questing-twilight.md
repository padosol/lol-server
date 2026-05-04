# Simplify Review Plan: Gold Timeline Feature

## Aggregated Findings

### Fixes to apply

1. **Remove `@JsonProperty("timeccing_others")`** — MatchSummonerDTO is populated via Object[] mapping, not Jackson. Dead code unrelated to this feature.
2. **Remove unnecessary comments** — `// 팀 누적 골드 타임라인` (TeamInfoData) and `// 골드 타임라인 (participant_frame JOIN)` (MatchSummonerDTO). Field names are self-documenting.
3. **Unify `convertToGameData` to use `buildTeamInfoData`** — Eliminates redundant `matchTeamRepository.findByMatchId()` call and divergent team-building logic. Team data is already in `MatchSummonerDTO` via the native SQL JOIN.

### Skip (with reasons)

- **Extract `toInt` to shared utility** — Only 2 usages across different repos. Premature abstraction.
- **Magic numbers 100/200** — Pre-existing issue across entire codebase, not introduced by this change.
- **Fragile positional Object[] mapping** — Architectural trade-off for native SQL. Would need integration test to mitigate, out of scope.
- **`timestamps` duplication per participant** — Valid concern but requires schema/query redesign. Track as future optimization.
- **`Integer[]` vs `int[]`** — Negligible impact at current scale.
- **N+1 in `convertToGameData`** — Pre-existing issue not introduced by this change.

## Files to modify

| File | Change |
|------|--------|
| `MatchSummonerDTO.java` | Remove `@JsonProperty` and comment |
| `TeamInfoData.java` | Remove comment |
| `MatchPersistenceAdapter.java` | Unify `convertToGameData` to use `buildTeamInfoData`, remove `matchTeamRepository.findByMatchId` call |

## Verification

`./gradlew :module:infra:persistence:postgresql:compileJava` + `./gradlew :module:infra:api:asciidoctor`
