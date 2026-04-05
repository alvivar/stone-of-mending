# Code Review: Stone of Mending

Fresh-eyes review focused on: simple code where every line justifies its existence, no abstractions until truly essential, performant, readable, idiomatically organized.

Reviewed independently by Opus and GPT, then merged through discussion.

## Should fix

### 1. `LOGGER` is public but only used internally

`StoneOfMendingMod.java` — `LOGGER` is `public static final` but only called once in the same file. Should be `private`.

### 2. Scroll/middle-click handlers use `getOrCreate` — pollutes the map

`StoneOfMendingMod.java` — When a player sends a scroll or middle-click packet without ever marking a selection, `getOrCreate` creates an empty `Selection` that lingers until disconnect. The clear handler correctly uses `get()` (nullable). The scroll and middle-click handlers should too:

```java
// Current (creates useless entries)
Selection sel = SelectionManager.getOrCreate(player);
if (!sel.isComplete()) return;

// Better (no map pollution)
Selection sel = SelectionManager.get(player);
if (sel == null || !sel.isComplete()) return;
```

Related: `SelectionManager.tick()` calls `clear()` on selections but leaves the empty entry in the map. Could `remove()` instead, avoiding tombstones.

### 3. `advanceFrontier(int delta)` only ever called with `1`

`Selection.java` — Two frontier mutation methods exist: `advanceFrontier(delta)` and `setFrontier(offset)`. The first is only called as `advanceFrontier(1)` in `collect()`. This could just be `sel.setFrontier(sel.frontierOffset() + 1)` at the call site, removing a method that doesn't pull its weight.

### 4. `sliceCollectStatus` uses magic ints, inconsistent with `SliceStatus` enum

`ScrollActions.java` — `checkSlice` returns a clean `SliceStatus` enum. `sliceCollectStatus` returns `1/0/-1` with a javadoc comment to explain meanings. These serve parallel purposes (scan a slice, report status). The int approach works but is inconsistent within the same file.

### 5. Unreachable `default` branch in renderer switch

`SelectionRenderer.java` — `Direction.Axis` is exhaustive (X, Y, Z). The `default -> { return; }` is dead code. Exists because variables are declared before the switch and the compiler can't prove they're assigned without a default. Fix: restructure to avoid pre-declared variables, or use a helper that returns the bounds.

### 6. `fabric.mod.json` description is empty

The `description` field is `""`. Should have a one-liner about the mod.

## Worth discussing, probably leave as-is

### 7. Duplicated inner loops in ScrollActions

The block-placing loop appears in both `place()` and `interiorFill()` (~12 lines each). The block-collecting loop appears in both `collect()` and `interiorCollect()` (~10 lines each).

A `placeSlice()` and `collectSlice()` helper could deduplicate ~22 lines total. However, each method has distinct setup/teardown (different pre-flight checks, different frontier logic). Extracting risks turning straightforward code into parameterized helpers. At the threshold of essential abstraction — leave unless a future change adds more duplication.

### 8. Repeated offhand validation

`place()`, `interiorFill()`, `replace()` all have the identical 3-line offhand check with `instanceof BlockItem blockItem`. The pattern-match binding makes extraction awkward — can't bind in a helper and use in the caller. Fine as-is.

### 9. Repeated client dimension check

`SelectionRenderer.render()` and `MouseHandlerMixin.isStoneActive()` both have the same 3-line dimension null-check-and-compare. Only 2 occurrences. Not enough to justify a helper.

### 10. `sync()` internally uses `getOrCreate`

`SelectionManager.sync()` calls `getOrCreate` which could theoretically create an empty entry. In practice this never happens — `sync()` is only called after a mark or action that already created the entry. Could be tightened defensively but it's not a bug path.

## Explicitly not issues

These were raised during review and deliberately ruled out with evidence:

- **Networking thread safety** — Verified from Fabric API source: `ServerPlayNetworking.registerGlobalReceiver` and `ClientPlayNetworking.registerGlobalReceiver` handlers run on the server/client main thread respectively. Wrapping in `context.server().execute()` would double-wrap.
- **`collect()` vs `checkSlice` preflight differences** — `collect()` only checks loaded status. `checkSlice` also checks replaceability. Different concerns for different operations. Both correct.
- **Two empty payload classes** (`ClearSelectionC2SPayload`, `MiddleClickC2SPayload`) — Structurally identical but semantically distinct. Merging would couple unrelated actions.
- **`collect()` iterating slice twice** (pre-flight + action) — Necessary for atomicity. Prevents partial collection of a slice when some chunks are unloaded.
- **Between-slices refill check in `replace()`** — Avoids entering next slice's loop just to fail immediately on first block.

## Summary

| Priority | Issue                                                                | Files                                |
| -------- | -------------------------------------------------------------------- | ------------------------------------ |
| Fix      | `LOGGER` public → private                                            | StoneOfMendingMod                    |
| Fix      | `getOrCreate` → `get` for packet handlers; remove tombstones in tick | StoneOfMendingMod, SelectionManager  |
| Fix      | Remove `advanceFrontier`, use `setFrontier`                          | Selection, ScrollActions             |
| Fix      | Magic ints → enum in `sliceCollectStatus`                            | ScrollActions                        |
| Fix      | Unreachable default in renderer switch                               | SelectionRenderer                    |
| Fix      | Empty description in mod metadata                                    | fabric.mod.json                      |
| Leave    | Duplicate inner loops                                                | ScrollActions                        |
| Leave    | Repeated offhand validation                                          | ScrollActions                        |
| Leave    | Repeated client dimension check                                      | SelectionRenderer, MouseHandlerMixin |
