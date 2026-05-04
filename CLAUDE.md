# dracolich-mtg-deck-builder-api

MTG deck builder service. Java 25 + Spring Boot 4.x + WebFlux + MongoDB. Owns deck CRUD, favorites, listing, stats, format rules, deck text import/export, public profile reads, and the AI bridge to `dracolich-ai-api`.

## Build & Run

```bash
# Build (forge:common 8.1.0+ required, dracolich-ai-api 1.1.0+ required)
mvn clean install -s ~/.m2/settings-personal.xml

# Run (port 8084, dev profile auto-active)
mvn spring-boot:run -pl mtg-deck-builder-api-web -s ~/.m2/settings-personal.xml
```

Required env in `.env` (or `application-dev.yml`):
```
ANON_COOKIE_SECRET=<32+ char random string>   # openssl rand -base64 48
MONGODB_DATABASE=dracolich-deck-builder
MONGODB_URI=mongodb://localhost:27017
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

JWT public key copied from user-api at `mtg-deck-builder-api-web/src/main/resources/keys/ec-public.pem` — never copy the private key.

End-to-end runtime requires 5 services: MongoDB (27017), user-api (8081), mtg-library-api (8080), ai-api (8082), deck-builder-api (8084).

## Module Structure

| Module | Purpose |
|--------|---------|
| `mtg-deck-builder-api-web` | Spring Boot app, controllers, `SecurityConfig`, Swagger UI |
| `mtg-deck-builder-core` | Services, MapStruct mappers, format rules engine, stats calculator, deck text parser/exporter |
| `mtg-deck-builder-integration` | WebClient → mtg-library-api + ai-api (both unwrap `DmdResponse`) |
| `mtg-deck-builder-api-data` | MongoDB entities (`DeckEntity`, `DeckCardEntity`, `FavoriteEntity`, `AuditEntity`, `AiCacheEntity`) and repositories |
| `mtg-deck-builder-api-dto` | DTOs, records, enums (`Visibility`, `DeckStatus`, `CardCategory`, `ValidationSeverity`), `ErrorCodes` |

## API Endpoints

Base path: `/dracolich/mtg-deck-builder/api/v0/`

### Decks (`/decks`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/` | OWNED | Create a deck (anon → forced PRIVATE; user → respects request `visibility`). Bumps `audit.lastModified`. |
| POST | `/validate` | NO | Stateless: validate + stats for an unsaved deck (sent in body) |
| POST | `/import` (JSON) | OWNED | Import a deck from Scryfall/Moxfield-style text. Strict mode: fails entirely if any card unresolvable. |
| POST | `/import` (text/plain) | OWNED | Same as above, body is raw deck text, metadata in query params |
| POST | `/import/validate` (JSON) | NO | Validate + stats from text without persisting |
| POST | `/import/validate` (text/plain) | NO | Same, raw body |
| POST | `/{id}/cards` | OWNED | Add card to a deck the caller owns. Bumps `audit.lastModified`, invalidates AI cache. |
| POST | `/{id}/copy` | YES | Copy a PUBLIC deck into the caller's account (sets `copiedFromId`, resets `favoritesCount`) |
| POST | `/{id}/claim` | YES (USER) + anon cookie | Transfer anon-owned deck to the authenticated user. Verifies signed `dracolich_anon_id` cookie matches `deck.anonId`. 409 if already claimed (DMD036). |
| GET | `/` | YES | Paginated list of caller's decks. Optional filters: `format`, `status`, `visibility` |
| GET | `/popular` | NO | Public decks sorted by `favoritesCount` DESC. Optional `format` filter |
| GET | `/latest` | NO | Public decks sorted by `audit.created_at` DESC. Optional `format` filter |
| GET | `/{id}` | NO/OWNED | Public decks → anyone; non-public → owner only |
| GET | `/{id}/stats` | NO/OWNED | Deterministic stats (mana curve, color pie, type breakdown, warnings) |
| GET | `/{id}/validate` | NO/OWNED | Format rules validation against the saved deck |
| GET | `/{id}/export` | NO/OWNED | Returns the deck as Scryfall-compatible text (`text/plain`) |
| PUT | `/{id}` | OWNED | Update deck metadata (name, description, format, status, visibility). Partial: only non-null fields applied. Anon cannot promote off PRIVATE (401). Bumps `audit.lastModified`. |
| PUT | `/{id}/cards/{cardId}` | OWNED | Update card count. `count >= 1` (use DELETE to remove). `?category=` query param disambiguates (default MAINBOARD). 404 if (cardId, category) not in deck. Bumps `audit.lastModified`. |
| DELETE | `/{id}` | YES | Delete deck (USER only). Anon decks expire via TTL. |
| DELETE | `/{id}/cards/{cardId}` | OWNED | Remove a card from a deck the caller owns. Bumps `audit.lastModified`, invalidates AI cache. |

### Favorites (`/favorites`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `?deck_id=` | YES | Favorite a deck. Atomically increments `favoritesCount`. |
| DELETE | `?deck_id=` | YES | Unfavorite. Decrements `favoritesCount` (clamped at 0). |

### User profile (`/users`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/{userId}/decks` | NO | Public-only listing of a user's decks. Optional `format` filter, paginated, sorted by `audit.last_modified` DESC. |
| GET | `/{userId}/favorites` | NO | Public-only listing of decks the user has favorited. Empty `deckIds` short-circuits to empty page. |

Path is `userId` rather than `username` for now — frontend already has userId from JWT claims. Username support is a thin add-on (would need a `GET /users/by-username/{username}` on user-api + a `DracolichUserClient` here).

### AI bridge (`/ai`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/decks/{id}/suggest` | YES | AI card suggestions. Returns `DeckAiResultDto` JSON. Cached per-deck until modified. |
| POST | `/decks/{id}/analyze` | YES | AI deck analysis with structured issues. Returns `DeckAiResultDto` JSON. Cached per-deck until modified. |
| POST | `/cards/{cardId}/format/{format}/suggest` | YES | Stateless one-shot card synergy. SSE stream (still text). |

### Auth labels

- **YES** — JWT required (anon principals are rejected by the service)
- **OWNED** — JWT *or* signed anon cookie; ownership of the resource enforced via `OwnershipResolver` (USER → matches `userId`, ANON → matches `anonId`). `loadOwnedDeckOrFail` returns 404 if deck doesn't exist, 401 if it exists but caller doesn't own it.
- **NO** — no identity required for public reads. Mutations always need at least an anon cookie.

## Security

`SecurityConfig` wires four beans, all from `forge:common`:

```java
@Bean JwtAuthenticationWebFilter jwtFilter(JwtTokenValidator v)        // order 10
@Bean AnonCookieFilter anonCookieFilter(...)                           // order 20
@Bean AnonCookieSigner anonCookieSigner(...)
@Bean JwtTokenValidator jwtTokenValidator(...)                         // EcPublicKeyJwtValidator.fromResource
```

Filter order: JWT runs first; if it sets a `Principal(USER, ...)` the cookie filter skips. Anon users get a `Principal(ANON, mintedId)` and a `Set-Cookie: dracolich_anon_id=...` (24h, HttpOnly, SameSite=Lax).

Service methods read identity via `ReactiveSecurityContextUtil.getPrincipal()` then `switchIfEmpty(Mono.error(unauthorized()))`. Modification endpoints route through `loadOwnedDeckOrFail()` which separates 404 (doesn't exist) from 401 (exists but not yours) — the second case happens often when a JWT expires mid-session.

**Claim flow special case**: `POST /decks/{id}/claim` is the only endpoint that needs to read the anon cookie *while* a JWT is present. Since the cookie filter skips when JWT is set, the controller pulls the cookie via `AnonCookieFilter.readAnonIdCookie(exchange, anonCookieSigner)` (verifies signature) and passes the verified anonId to the service. Missing/invalid cookie → 401. Mismatch with `deck.anonId` → 401. Already claimed (`deck.userId != null`) → 409.

Swagger UI exposes the "Authorize" button via `@SecurityScheme(name = "bearerAuth")` + `@OpenAPIDefinition(security = @SecurityRequirement(name = "bearerAuth"))` on `MtgDeckBuilderWebApplication`. The header is attached to every Try-it-out call when authorized; service-layer Principal checks do the actual gating.

## Data Model

### `DeckEntity` (MongoDB collection: `decks`)

```
id, userId, anonId, copiedFromId, audit (AuditEntity)
name, description, format (Format enum), status (DeckStatus enum)
colors (Set<Color>), commander (List<DeckCardEntity> — supports partners)
cards (List<DeckCardEntity>), favoritesCount (Long, default 0)
visibility (PUBLIC | PRIVATE | UNLISTED)
aiSessionId (String) — ai-api session reused per deck
aiCache (AiCacheEntity) — structured suggest + analyze results
```

- `userId` xor `anonId` (anon decks always `visibility=PRIVATE`)
- `commander.cardCategory` is null (commander zone is separate from MAINBOARD/SIDEBOARD/MAYBE_BOARD)
- `format` references `dm.dracolich.mtgLibrary.dto.enums.Format`
- `audit.lastModified` is bumped on any composition or metadata change via `touchLastModified()` — drives AI cache invalidation
- Claim flow: sets `userId = principal.id()`, clears `anonId`, leaves cards/cache intact (no `lastModified` bump since composition unchanged)
- `DeckDto` exposes computed `card_count`, `mainboard_count`, `sideboard_count`, `maybeboard_count` via MapStruct `@AfterMapping`

### `DeckCardEntity` (embedded — denormalized snapshot)

```
cardId (FK → mtg-library-api), name, manaCost, cmc, colors, typeLine,
imageUri (Map<String,String> — Scryfall sizes), count, cardCategory
```

Display data is denormalized for fast deck reads. Card identity stays referential via `cardId`. The `(cardId, cardCategory)` pair is the unique key when targeting a specific copy via PUT/DELETE — same `cardId` can appear with different `cardCategory` values across MAINBOARD / SIDEBOARD / MAYBE_BOARD.

### `FavoriteEntity` (MongoDB collection: `favorites`)

```
id, userId, deckIds (Set<String>)
```

One document per user; favorited deck IDs in a set. Deck-side `favoritesCount` is the denormalized counter for popular-deck queries.

### `AiCacheEntity` (embedded on `DeckEntity`)

```
suggestResult (DeckAiResultDto), suggestAt (Instant)
analyzeResult (DeckAiResultDto), analyzeAt (Instant)
```

Stores the full structured AI response so cache hits skip the AI call entirely. Validity check: `audit.lastModified <= cachedAt`.

## Format Rules Engine

`FormatRuleSet` interface + 2 concrete impls + registry:

| Class | Format(s) | Rules enforced |
|---|---|---|
| `CommanderRuleSet` | `COMMANDER` | exactly 100 total cards (commander + 99 main), 1-2 commanders (Partner/Background), singleton (basics excluded), color identity ⊆ commander |
| `SixtyCardFormatRuleSet` | `STANDARD`, `MODERN`, `PIONEER`, `PAUPER` | no commander allowed, mainboard ≥60, sideboard ≤15, ≤4 of each non-basic across mainboard+sideboard |

`FormatRuleSetRegistry` is a `@Component` with an `EnumMap` lookup. Other formats (LEGACY, BRAWL, etc.) return a `format_unsupported` violation — extend the registry as needed.

**Not enforced**: banned-list checks, Pauper "commons only", Companion validation. Banned list would need per-card `legalities` lookups via mtg-library-api — deferred per YAGNI.

## Stats Calculator

`DeckStatsCalculator` is a pure-static helper computing:

- `cardCount`, `landCount`
- `manaCurve` (TreeMap<Integer, Integer>, non-lands only, bucketed by `cmc.intValue()`)
- `colorPie` (Map<Color, Double>, normalized 3-decimal percentages, lands and colorless excluded)
- `typeBreakdown` (Map<String, Integer>, a card with multiple types counts in each)
- `averageCmc` (mean of non-land CMCs, weighted, 2-decimal)
- `warnings` (deterministic — low/high land ratio, high average CMC)

Used by `/decks/{id}/stats`, `/decks/validate`, `/decks/import/validate`, AND the AI bridge (stats injected into prompts).

## Deck Text Parser / Exporter

`core/text/DeckTextParser` + `DeckTextExporter` handle Scryfall/Moxfield-format decklists:

- Section markers: `// Commander`, `// Sideboard`, `// Outside the Game`, `// Maybeboard`
- Strips set codes `(CMM)`, collector numbers, foil markers `*F*`
- DFC name resolution flows through mtg-library-api
- Strict mode: any unresolved card name fails the entire import with per-line errors (HTTP 422, error code DMD035)

## AI Bridge

deck-builder-api owns the public AI endpoints; ai-api is treated as internal infrastructure. See the root `CLAUDE.md` for the structured-response shape and tool architecture; not duplicated here.

## Repository — `DeckCustomRepository` queries

Hand-rolled `ReactiveMongoTemplate` queries that go beyond `ReactiveMongoRepository` derived methods:

| Method | Used by |
|---|---|
| `findUserDecksByFilters(userId, format, status, visibility, page, size)` | `GET /decks` (caller's own list) |
| `findPopularPublicDecks(format, page, size)` | `GET /decks/popular` |
| `findLatestPublicDecks(format, page, size)` | `GET /decks/latest` |
| `findPublicDecksByUserId(userId, format, page, size)` | `GET /users/{userId}/decks` |
| `findPublicDecksByIds(deckIds, page, size)` | `GET /users/{userId}/favorites` (empty deckIds → empty page short-circuit) |

Shared `paginated(criteria, page, size, sort)` helper does the count + find + `PageImpl` assembly.

## Error Codes

| Code | Meaning |
|------|---------|
| DMD030 | MTG Library lookup failed |
| DMD031 | MTG Library request rejected |
| DMD032 | MTG Library API unavailable (also reused for ai-api 5xx — name is misleading, plan to rename to `upstreamUnavailable`) |
| DMD033 | Generic not-found (deck, card) |
| DMD034 | User not authorized to perform this action |
| DMD035 | Deck import failed (per-line errors in response, returns 422) |
| DMD036 | Deck already claimed (returns 409) |

Two `ErrorUtil` classes split by module concerns:
- `core/helpers/ErrorUtil` — `notFound`, `unauthorized`, `internalServerError`, `importFailed`, `deckAlreadyClaimed` + `ImportFailure` record
- `integration/helpers/ErrorUtil` — `libraryNotFound`, `libraryClientError`, `libraryUnavailable`

## Required Properties

```yaml
spring:
  webflux:
    base-path: dracolich/mtg-deck-builder/api/v0/
  mongodb:
    database: ${MONGODB_DATABASE}
    uri: ${MONGODB_URI}

server:
  port: ${PORT:8084}

dracolich:
  cookie:
    secret: ${ANON_COOKIE_SECRET}     # >= 32 chars
    lifetime: PT24H
    secure: false                      # true in prod
  jwt:
    public-key: classpath:keys/ec-public.pem
  mtg-library:
    api:
      base-url: http://localhost:8080/dracolich/mtg-library/api/v0
  ai:
    api:
      base-url: http://localhost:8082/dracolich/ai/api/v0
```

## Required Dependency Versions

```xml
<dracolich.forge.version>8.1.0</dracolich.forge.version>     <!-- SSE-aware DmdResponseWrapper -->
<dracolich.mtg-library.version>1.2.0</dracolich.mtg-library.version>
<dracolich.ai-api.version>1.1.0</dracolich.ai-api.version>   <!-- IssueDto + topic field on CardSuggestionDto -->
```

## Phase 3 Status — Feature Complete

All originally-scoped Phase 3 endpoints have shipped. Outstanding work is hardening / deferred items only:

| Item | Priority | Notes |
|------|----------|-------|
| Username-based public profile URLs | LOW | Currently `/users/{userId}/...`. Add when public-profile UI lands. |
| Test coverage | MEDIUM | 0% — pure-logic classes (`FormatRuleSet` impls, `DeckStatsCalculator`, `DeckTextParser`) are highest-value targets |
| Rename `libraryUnavailable` → `upstreamUnavailable` | LOW | Cosmetic |
| `application-prod.yml` | DEFERRED | Helm/ArgoCD phase (Azure AKS) |
| Rate limiting | DEFERRED | Spring Cloud Gateway phase |
| Banned-list validation | DEFERRED | Would need per-card legality lookups |
| ai-api auth | DEFERRED | Post-Phase 4 per root CLAUDE.md note |

## Implementation Notes

- `audit.lastModified` is bumped via `touchLastModified()` after any composition or metadata change. PUT metadata bumps it always (simpler than per-field detection); PUT card bumps on count change; claim does NOT bump (composition unchanged, AI cache stays valid).
- `canAccess()` in `DeckServiceImpl` is hand-rolled (predates `OwnershipResolver` from forge). Could be unified.
- `MAYBE_BOARD` enum value uses underscore form. The text format uses `// Maybeboard` (no underscore) — the parser maps both.
- Reactor + Netty: blocking on event loop is the recurring trap. All tool methods that buffer must `.subscribeOn(Schedulers.boundedElastic())` or run inside `Mono.fromCallable` — `.toFuture().join()` blocks whichever thread calls `.join()`.
- Spring AI's `ChatClient` is a singleton in `AnthropicConfig` — re-registering tools per-call accumulates duplicates and crashes with "Multiple tools with the same name."
- `DmdResponseWrapper` (forge) skips wrapping for endpoints that produce `text/event-stream` or `application/x-ndjson`. SSE flux passes through unbuffered.
