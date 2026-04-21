# Stone of Mending — Plan

## Tool

A face-based building tool. Mark two corners to define a 3D selection, then scroll to collect or place blocks one layer at a time.

- Scroll down collects the current layer into the player's inventory.
- Scroll up places a new layer from the offhand material.
- The frontier slice extends infinitely beyond the original selection in both directions.

## Mechanics

### Selection

- Hold the Stone of Mending in the main hand.
- Left-click a block face to mark point A. The clicked face locks the normal direction.
- Right-click any block (same dimension) to mark point B.
- A and B define opposite corners of a 3D box.
- The face normal from A defines which axis layers are sliced along.

### Collection (scroll down)

- The frontier starts at the outermost face of the box (the normal side).
- Scroll down collects every supported block in the current frontier slice.
- The frontier moves one layer inward after each scroll.
- Continues beyond the original box indefinitely.

### Placement (scroll up)

- Keep the Stone of Mending in the main hand, placement material in the offhand.
- Scroll up places one new layer from the offhand material.
- The frontier moves one layer outward after each scroll.
- Continues beyond the original box indefinitely.

## Locked decisions

- Selection is a 3D box defined by A and B.
- The first marked face locks the normal. B has no face constraint.
- Outward = direction of the first marked face normal. Inward = opposite.
- The active frontier is a 1-block-thick slice orthogonal to the normal.
- Frontier offset 0 = the outermost slice on the normal side of the box.
- Positive offset = inward (collection direction). Negative offset = outward beyond the face.
- Scroll always moves the frontier. Collection/placement attempt acts on the slice.
- Collection acts at the current frontier, then always advances inward.
- Placement dynamically scans for the first incomplete slice: inside box far-to-face, then outward.
- Placement does not use or modify the frontier offset.
- When the offhand empties during placement or replacement, the inventory is searched for more of the same material.
- Material replacement (middle click) operates on the entire box, does not touch the frontier.
- Marking A always starts a new selection and resets the frontier.
- Clicking air does nothing.
- Selection persists per-player until replaced or logout.
- A collectible block: non-air, no block entity (loaded chunk).
- Collection and replacement use mining-style drops via `Block.getDrops()` with a virtual netherite pickaxe.
- No enchantments on the virtual tool (no silk touch, no fortune).
- Empty drops are valid — block is removed, yields nothing.
- A placeable target: offhand holds a BlockItem, target space is replaceable.
- Placement uses defaultBlockState() (no orientation or context).
- Placement material comes from the offhand slot.
- Offhand auto-refill from inventory applies to placement and replacement.
- Collection and placement are server-side. Rendering is client-side.
- Server validates: held item, active selection, same dimension, loaded target positions.
- Mouse wheel is intercepted only while holding the Stone with an active complete selection.

## Geometry

- `SelectionBox` computes bounds, face block, frontier block, and slice positions from A + B + normal.
- Face block = outermost block coordinate on the normal axis side of the box.
- Frontier block = `faceBlock - offset * normalAxisStep`.
- Slice positions = all blocks in the 2D cross-section at the frontier block coordinate.

## Implementation

### Phase 1: Item and selection state ✓

- Register the Stone of Mending item (creative tab: Tools & Utilities, stacksTo 1).
- Server-side per-player selection state: point A, point B, plane normal, dimension, frontier offset.
- S2C packet sync (SelectionSyncPayload) for rendering.
- Cleanup on player disconnect.

### Phase 2: Marking behavior ✓

- Left-click marks A via AttackBlockCallback (locks face normal and dimension).
- Right-click marks B via StoneOfMendingItem.useOn (same dimension, no plane constraint).
- Cancel normal block interaction when the Stone handles the click.
- Action bar feedback for each action.

### Phase 3: Selection rendering ✓

- Client-side outline via LevelRenderEvents.AFTER_SOLID_FEATURES.
- Single-block green marker when only A exists.
- Dim cyan outline for the full 3D box when A and B exist.
- Bright frontier slice outline at the current offset.
- Dimension-gated (won't render in wrong dimension).
- Only visible when holding the Stone in main hand.

### Phase 4: Scroll input ✓

- Client mixin (MouseHandlerMixin) intercepts MouseHandler.onScroll at HEAD.
- Guards: no screen, player exists, holding Stone, complete selection, matching dimension.
- Cancels hotbar scroll, sends ScrollActionC2SPayload (direction ±1) to server.
- Server validates held item, complete selection, same dimension, direction ±1.

### Phase 5: Collection (scroll down) ✓

- ScrollActions.collect() iterates SelectionBox.slicePositions(frontierOffset).
- Skips: unloaded chunks, air, block entities, blocks with no item form (asItem == AIR).
- Checks removeBlock() result before giving items (prevents duping).
- Inserts into inventory, drops overflow at player feet.
- Always advances frontier +1 (inward). Syncs to client.

### Phase 6: Placement (scroll up) ✓

- ScrollActions.place() checks offhand for BlockItem, rejects with feedback otherwise.
- Places defaultBlockState() at slicePositions(frontierOffset - 1) (one step outward).
- Only replaces replaceable blocks (canBeReplaced).
- Shrinks offhand per placed block, stops when empty.
- Always advances frontier -1 (outward). Syncs to client.

## How to run

```
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
.\gradlew runClient
```

Or in Git Bash:

```
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.2.10-hotspot" ./gradlew runClient
```

The Stone of Mending is in the creative menu under Tools & Utilities.

## File layout

```
src/main/java/io/github/alvivar/stoneofmending/
  StoneOfMendingMod.java          — init, AttackBlockCallback (mark A), scroll handler dispatch
  StoneOfMendingItem.java         — useOn (mark B)
  ModItems.java                   — item registration + creative tab
  Selection.java                  — mutable per-player state (A, B, normal, dimension, offset)
  SelectionManager.java           — UUID→Selection map, sync helper
  SelectionSyncPayload.java       — S2C packet (selection state to client)
  ScrollActionC2SPayload.java     — C2S packet (scroll direction ±1)
  SelectionBox.java               — shared geometry (bounds, face, frontier, slice positions)
  ScrollActions.java              — collect + place + replace + interior operations
  ClearSelectionC2SPayload.java   — C2S packet (clear selection)
  MiddleClickC2SPayload.java      — C2S packet (material replacement)
  SetNormalC2SPayload.java        — C2S packet (change normal from look direction)

src/client/java/io/github/alvivar/stoneofmending/
  StoneOfMendingClient.java       — client init, packet receiver, disconnect cleanup
  ClientSelectionState.java       — client-side selection mirror
  SelectionRenderer.java          — world-space outline rendering
  mixin/MouseHandlerMixin.java    — scroll + middle-click interception
  mixin/StartAttackMixin.java     — left-click air clears, Ctrl+click changes normal
```

### Phase 7: Polish and controls ✓

**7a. Success-gated frontier**: The frontier only advances outward when the target slice is fully filled. If the player runs out of material mid-slice, the frontier stays — next scroll-up retries the same slice. A slice is "complete" when no position in it is replaceable (second pass check after placement). If a slice is already fully occupied before any placement, it counts as complete and the frontier advances (prevents getting stuck on solid ground). Unloaded positions count as incomplete (conservative).

Feedback cases:

- Placed N, complete → advance, "Placed N blocks"
- Placed N, incomplete → stay, "Placed N blocks (incomplete)"
- Placed 0, complete → advance, "Slice already full"
- Placed 0, incomplete → stay, "Nothing to place"

**7b. Offhand auto-refill**: When the offhand stack empties during placement, scan the player's inventory (slots 0–35) for a matching stack (`ItemStack.isSameItemSameComponents`). If found, move it to the offhand slot, clear source slot, refresh the local reference, and continue placing. If not found, stop.

**7c. Left-click air clears selection**: Client mixin on `Minecraft.startAttack()` detects left-click miss (hitResult == MISS) while holding Stone with an active selection. Sends `ClearSelectionC2SPayload` to server. Server validates held item, clears selection, syncs to client. No message if no selection exists (silent no-op).

### Phase 8: Dynamic placement + material replacement

**8a. Dynamic placement scan**: Placement no longer uses `frontierOffset`. Each scroll-up scans for the first incomplete slice in order: inside the box from far side to face (`depth-1` → `0`), then outward beyond the face (`-1, -2, ...`). `frontierOffset` becomes collection-only. Helpers: `isSliceComplete(level, box, offset)` and `findNextPlacementOffset(level, box)` in `ScrollActions`; `depth()` in `SelectionBox`.

**8b. Mining-style drops**: Collection and replacement use `Block.getDrops(state, level, pos, null, player, tool)` (6-arg) with a cached virtual netherite pickaxe (no enchantments) as the tool context. Blocks yield what they'd drop if mined (Stone → Cobblestone, Ores → drops, Glass → nothing). Empty drops are valid — block is still removed, just yields nothing. Simplifies eligibility: skip air and block entities only (no more `asItem() == AIR` check). No XP drops. The virtual pickaxe is cached as a `private static final` field, reused across all drop calls.

**8c. Material replacement (middle click)**: Middle click while holding Stone with complete selection swaps every eligible block in the entire selected box to the offhand material. Order: far-side slice to face slice (consistent with placement). Source rules: same as collection (non-air, no block entity). Target rules: offhand must be BlockItem. Skips blocks already matching the target. Auto-refill from inventory (same as placement — scans slots 0–35 for matching stacks when offhand empties). Partial replace stops when materials run out. Does not interact with the frontier. Each block is transactional: compute old drops → replace with new block → only if replacement succeeds, consume material + grant drops → if fails, restore old block.

Input model:

- Left-click block → mark A
- Right-click block → mark B
- Left-click air → clear selection
- Scroll down → collect layer (frontier cursor, inward)
- Scroll up → place layer (frontier cursor, outward)
- Shift + scroll up → fill deepest incomplete slice inside box (no frontier)
- Shift + scroll down → collect deepest non-empty slice inside box (no frontier)
- Middle click → replace material in entire box
- Ctrl + left-click → change normal to look direction, reset frontier

### Phase 9: Interior operations (Shift+scroll)

**9a. Range fill (Shift+scroll up)**: Scans the active range from far side toward front. Finds the first incomplete slice. Places offhand material there. Does not touch the frontier. Aborts on blocked (unloaded) slices. Same placement rules as normal scroll-up (offhand BlockItem, canBeReplaced, auto-refill). Feedback: "Filled N blocks" or "Nothing to fill".

**9b. Range collect (Shift+scroll down)**: Scans the active range from front toward far side. Finds the first slice with collectible blocks (non-air, no block entity). Collects from that slice using mining-style drops. Does not touch the frontier. Aborts on blocked slices. Feedback: "Collected N blocks" or "Nothing to collect".

**Active range**: `min(frontierOffset, 0)` to `max(frontierOffset - 1, depth - 1)`. Expands beyond the original box when the frontier has extended outward (negative) or inward past the far side (positive). When frontier is at 0 (untouched), range equals the original box.

**Cursor tracking**: Shift+scroll moves the frontier to reflect where the action happened (target for fill, target+1 for collect). If the frontier is currently inside the box [0, depth-1], it is clamped so it cannot exit the box via shift+scroll. If outside the box, it only moves in the direction that retracts toward the box (shift+down when in front, shift+up when past far side). This keeps the visual indicator honest about where the tool is working while preventing shift+scroll from pushing the cursor further out.

**Linear scroll (Phase 10b)**: Normal scroll-up always targets exactly `frontier - 1`. No gap-skipping — the cursor moves one slice outward whether or not anything was placed. Aborts only if the target slice is blocked (unloaded/out-of-bounds). Mirrors scroll-down which always advances +1. Shift+scroll provides the "smart jump" for skipping to interesting slices. `findNextPlacement` was removed.

**Wire format**: `ScrollActionC2SPayload` gains a `shifted` boolean. Client mixin detects shift via `InputConstants.isKeyDown(window, KEY_LSHIFT/KEY_RSHIFT)`. Server dispatches to `interiorFill`/`interiorCollect` when shifted.

### Phase 10: Auto-clear on item switch

Server tick checks players with active selections. If a player is no longer holding the Stone of Mending in main hand, their selection is cleared and synced to the client. Uses `ServerTickEvents.END_SERVER_TICK`. Only iterates players in `SelectionManager` (skips empty map). Covers hotbar switching, item dropping, inventory changes. Collects players to clear into a snapshot list to avoid concurrent modification.

### Phase 11: Item model, texture, and language ✓

- 16×16 item texture: dark slate stone with glowing teal mending veins. Kinked diagonal seam from upper-right to lower-left, bright core glow at the knot point, one branch vein. Shading follows top-left light source (Minecraft standard).
- Item model: standard `item/generated` pointing to the texture.
- MC 26.1 item definition file: `assets/stone_of_mending/items/stone_of_mending.json` pointing to the model (required by 26.1's new item model system).
- Language file: `en_us.json` with display name "Stone of Mending".
- Texture generated via Python script (`gen_texture.py`) using Pillow.

Files:

```
src/main/resources/assets/stone_of_mending/
  items/stone_of_mending.json             — item definition (MC 26.1 required)
  models/item/stone_of_mending.json       — item/generated model
  textures/item/stone_of_mending.png      — 16×16 texture
  lang/en_us.json                         — display name
```

### Phase 12: Ctrl+click to re-orient normal ✓

Ctrl+left click to change the slicing direction based on where the player is looking. Frontier resets to 0.

**Behavior:**

- Ctrl+left click while holding the Stone with an active selection: new normal = dominant axis of player's look direction. Frontier resets to 0. A and B stay unchanged.
- Ctrl+click always takes precedence over clear-on-miss and mark-A.

**Precedence:** Ctrl+click > click-air-clear > click-block-mark-A.

**Client (StartAttackMixin):**

- `isCtrlWithStone()` helper checks player + Stone + selection + Ctrl key state.
- Guards both `startAttack` (send payload + cancel) and `continueAttack` (cancel only, prevents creative-mode `AttackBlockCallback` re-entry through `continueDestroyBlock`).

**Payload:** `SetNormalC2SPayload()` — empty, server computes direction from player's look vector.

**Server handler:**

- Validates: holding Stone, selection has A, same dimension.
- Computes dominant axis from `player.getLookAngle()` via `normalFromLook()`.
- Sets `sel.setNormal(newNormal)`, `sel.setFrontier(0)`.
- Syncs + overlay message: "Normal: East" etc.

**Files modified:**

- `SetNormalC2SPayload.java` — empty C2S payload
- `StartAttackMixin.java` — Ctrl detection, dual injection (startAttack + continueAttack)
- `StoneOfMendingMod.java` — handler with `normalFromLook` + `directionName` helpers
- `Selection.java` — `setNormal(Direction)` setter

### Phase 13: Inventory-limited collection

Collection and replacement stop when the player's inventory is full instead of dropping items on the ground. The inventory is the natural limit of the stone's power.

**Core rule:** Before removing any block, check if its drops fit in inventory. If they don't, stop immediately — don't remove that block or any further blocks.

**Helper:** `canFitAll(Inventory, List<ItemStack>)` — copies slots 0–35 into a temp array, simulates inserting each drop (respecting `isSameItemSameComponents`, stack limits, empty slots). Read-only against real inventory. Called per-block; since drops are added to real inventory on success, each subsequent call sees the updated state.

**Affected methods:**

- `collect()` — stop on full, don't advance frontier on partial slice (remaining blocks exist)
- `interiorCollect()` — stop on full, don't move cursor if stopped early
- `replace()` — stop on full, no frontier concern (stateless over whole box)

All three: remove `player.drop(drop, false)` fallback entirely. Empty drops (glass, etc.) need no inventory check — block is still removed.

**Frontier behavior:** `collect()` only advances frontier (+1) if the entire slice was processed without hitting inventory full. Partial slice = frontier stays = player can scroll down again after making room. Same for `interiorCollect()` cursor tracking.

**Messages (lore voice):**

- Partial progress + full: "The stone gathered 12 blocks, then could carry no more."
- Zero progress (already full): "The stone can carry no more."
- Same pattern for replace: "The stone replaced 9 blocks, then could carry no more."

**Unaffected methods:** `place()` and `interiorFill()` consume from offhand, don't produce drops.

**Files modified:**

- `ScrollActions.java` — `canFitAll` helper, modified collect/interiorCollect/replace loops, new messages, removed drop fallback

### Phase 14: Sound feedback ✓

All impactful actions play a sound to the acting player only (via `ClientboundSoundPacket` sent directly to the player's connection, not broadcast).

**Sound palette — 3 families:**

- Crystalline/attunement: `AMETHYST_BLOCK_CHIME` (mark A), `ENCHANTMENT_TABLE_USE` (mark B), `LODESTONE_COMPASS_LOCK` (change normal)
- Work pulses: `LODESTONE_BREAK` (collect), `LODESTONE_PLACE` (place/fill), `RESPAWN_ANCHOR_SET_SPAWN` (replace)
- Denial/limit: `AMETHYST_BLOCK_HIT` (errors, clear), `CONDUIT_DEACTIVATE` (inventory full)

**Silent on "nothing to do"** — avoids spam on empty slices during rapid scrolling.

**Helper:** `ScrollActions.playSound(ServerPlayer, SoundEvent, float volume)` — package-private, reused from all three files.

### Phase 15: Passive item repair ✓

While the Stone of Mending is held in main hand, it passively repairs the most damaged repairable item in the player's inventory.

**Rules:**

- Every 80 ticks (4 seconds), scan inventory (slots 0–35 + armor + offhand)
- Target: item with highest damage ratio (damage / maxDamage). Tie-break: highest absolute damage
- Repair amount: `max(1, maxDamage / 100)` — ~1% per tick, minimum 1
- Skip undamaged items, undamageable items
- No sound, no particles — silent passive effect
- No selection required — works anytime the stone is held
- Independent from SelectionManager — separate tick logic in StoneOfMendingMod

**Implementation:** Private `tickRepair(MinecraftServer)` method in StoneOfMendingMod, called from the same server tick event. Uses a static tick counter, fires every 80 ticks.

### Phase 23: Passive stone-mending aura (experimental) ✓

When the repair pulse has nothing to do (all items mended), the stone extends
its attention outward and mends broken stone — cobblestone and cobbled
deepslate — into their whole forms. One block per 4-second pulse.

**Selection (Phase 23.1 refinement)**: Favors visible blocks. Each candidate
is scored by **exposure** = number of its 6 neighbors that are NOT full
opaque cubes (range 0–6). Only the current maximum-exposure tier is eligible.
Within that tier, the top 9 closest are taken and one is picked at random.
As exposed cobble transmutes out, less-exposed blocks rise to the new max
tier — all cobble in range eventually gets mended, in natural order
(surface → wall → buried). Unloaded neighbors don't count as exposed
(prevents chunk-boundary bias).

**Why "experimental"**: this is the only passive that mutates the world around
the player rather than their inventory. It blurs the trust line the other
passives respect ("the stone changes only what you ask it to"). Accepted
because the etymology is too on-name to pass up: the item literally calls
itself the Stone of Mending, and cobblestone → stone is the most literal
"broken → whole" transformation in the game.

**Rule**:

- Runs inside `tickRepair` as a fallback when `best.isEmpty()` (nothing to
  repair in the player's inventory).
- Scans a 9×9×9 cube (player position ±4) for `minecraft:cobblestone` or
  `minecraft:cobbled_deepslate`.
- Picks the closest one by squared distance from block center to player.
- Replaces it with the whole form (stone or deepslate) using standard
  neighbor+client updates.
- Silent — no sound, no particle, no overlay. Players discover it by watching.
- Not gated by game mode: works in creative too (consistent with other
  passives).
- Unloaded chunks skipped — no force-loading.

**Scope**:

- Cobblestone → stone
- Cobbled deepslate → deepslate
- Nothing else (mossy cobble, blackstone, etc. have no clean mend target)

**Files modified**:

- `StoneOfMendingMod.java` — added `mendNearbyStone(ServerPlayer)` helper and
  branch in `tickRepair` when no damaged items found.

### Phase 22: Hunger cost per block handled ✓

The stone is a better mining method, not a free one. Every block handled by
the stone costs the same hunger exhaustion as mining by hand in vanilla.

**Rule:** `0.0625` exhaustion per successful block — **one stack (64) per
hunger point**. Applied flat to all block operations: collect, place,
replace, border variants, interior variants. Placement costs the same as
collection — symmetric (diverges from vanilla where placement is free, but
conceptually cleaner for a reshape-the-world tool).

**Why 0.0625 ("a stack per drumstick-half")**: Minecraft-native mental model
— players already think in stacks of 64. At this rate, 64 blocks = 4
exhaustion = 1 hunger point = half a drumstick. Sits between jump (0.05) and
sprint (0.1) in vanilla's activity ladder — effortful but not punishing. Small
ops stay invisible, medium ops clearly register, room-sized builds cost a
meal or two.

**Only successful blocks are charged.** Failed attempts, already-matching
replace targets, blocked slices, and "nothing to do" operations cost nothing.
The count variable already tracked in each operation (for overlay messages) is
reused as the charge basis.

**Creative mode auto-skips** — `ServerPlayer.causeFoodExhaustion` checks
`abilities.invulnerable` and no-ops in creative. No manual guard needed.

**Peaceful mode**: hunger regens continuously, so the cost is effectively
nullified. Accepted asymmetry rather than patching with health damage.

**Passive repair and top-up stay free** — they aren't "work."

**Magnitude**: 12.5× vanilla mining, slightly above jump exhaustion. A
64-block op drains half a drumstick; a 128-block op drains a full drumstick;
a 1000-block op drains ~15 drumsticks (~1.5 food bars). Small ops (≤30 blocks)
stay subtle; medium-to-large ops clearly cost food.

**Files modified:**

- `ScrollActions.java` — `EXHAUSTION_PER_BLOCK` constant, `chargeExhaustion`
  helper, one call per operation end (7 total) using the existing count
  variable.

### Phase 21: Passive top-up (arrows + torches) ✓

While the Stone of Mending is held in main hand, it passively tops up low-count
expedition basics. Maintenance semantics, not farming. Parallel with repair.

**Rules:**

- **Items**: arrows (cap 64) and torches (cap 64) only — one stack each.
- **Main hand only** (same rule as repair).
- **Selector**: whichever supported item has the lowest `count / cap` ratio.
  Ties go to arrows (deterministic).
- **Stops entirely when both items are at or above cap** — no trickle above
  stocked.
- **Cadence**: one item every 1125 ticks (56.25 seconds) — tuned so a full stack of 64 takes exactly one real-time hour.
- **Timer persistence**: per-player counter. Pauses when the stone is not in
  main hand, resumes from last value when it is. Different from repair's
  global counter (80 ticks, cheap to miss; 1000 ticks would feel bad to
  reset).
- **Inventory-full behavior**: `inv.add()` returns false → silently skip. Timer
  still resets.
- **Silent** — no sound, no overlay.

**Implementation:** Per-player `Map<UUID, Integer> topupTicks` in
`StoneOfMendingMod`. `tickTopup(server)` iterates online players, increments
per-UUID counter only for players holding the stone in main hand (natural
pause when unheld). At 1000, call `produceTopup(player)` and reset counter.

`produceTopup` counts arrows + torches via `Inventory.countItem`, computes
ratios, picks target, `inv.add(new ItemStack(target))`. Timer resets
unconditionally — even when both caps hit or inventory full. Alternative
("stay armed, fire instantly on next dip below cap") rejected as too
reactive; maintenance tempo wants a full cadence before each produce.

Named constants: `TOPUP_TICKS`, `ARROW_CAP`, `TORCH_CAP`.

Cleanup on disconnect: `topupTicks.remove(uuid)`.

**Files modified:**

- `StoneOfMendingMod.java` — `topupTicks` map, `tickTopup`, `produceTopup`,
  disconnect cleanup, wired into existing tick event.

### Phase 20: Ctrl+click reorients at frontier=0, pivots when stroke in progress ✓

**Problem:** Phase 17 made all orthogonal Ctrl+clicks pivot, collapsing the box to a 1-thick seed. This broke a valid pre-Phase-17 flow: select a tall tower, Ctrl+click down to face the bottom, scroll down to consume from below.

**Fix:** Use `frontierOffset` as the discriminator (same rule family as Phase 19 mark B):

- **frontier = 0** = no stroke in progress, cursor still at box face → **reorient whole box** (preserve A/B, change normal).
- **frontier ≠ 0** = stroke in progress, cursor displaced → **pivot at frontier face**.

**Full rule set:**

- A-only → reorient from look.
- Complete + same → deny.
- Complete + opposite → flip in place (Phase 17).
- Complete + orthogonal + frontier = 0 → reorient whole box. Message: "The stone now faces {dir}."
- Complete + orthogonal + frontier ≠ 0 → pivot at frontier face (Phase 17). Message: "The stone pivots toward {dir}."

**Trade-off:** Pivoting a thick untouched box directly requires scrolling once first. Acceptable because pivot semantically means "turn an in-progress stroke," not "derive a stroke from any fresh box."

**Files modified:**

- `StoneOfMendingMod.java` — added `frontierOffset == 0` fast-path before pivot math.

### Phase 19: Mark B commits stroke at frontier ✓

**Problem:** After scrolling or pivoting, right-click (mark B) reshaped from stale A, not from the current working face. Boxes spanned the whole historical stroke. Reshape felt disconnected from where the player actually was.

**Rule:** If `isComplete()` AND `frontierOffset != 0`, right-click commits the stroke: derive a fresh A from the current frontier face, then set B to the clicked position. Frontier resets to 0.

**Fresh A derivation:**

- **Normal axis**: `frontierBlock(frontierOffset)` — the face coordinate.
- **Non-normal axes**: **farther bound** of the old box from the clicked coordinate. Corner opposite the click, robust against original A/B click order.

**Cases:**

- A-only → standard `markB`.
- Complete + frontier=0 → standard `markB` (no stroke in progress, user is just re-marking B).
- Complete + frontier!=0 → commit at frontier, derive fresh A, set B, reset frontier.

**Why "farther bound" not stale A coords:** Using stale A's non-normal coords silently depended on which corner the user originally clicked. Two equivalent boxes with A/B reversed would collapse differently on reshape. Farther-bound is order-independent.

**Files modified:**

- `StoneOfMendingItem.java` — pre-`markB` commit logic, `fartherBound()` helper.

### Phase 18: Mark B resets frontier ✓

**Rule:** Marking either A or B resets `frontierOffset` to 0. A fresh mark = a fresh shape = a fresh cursor.

**Why:** `frontierOffset` only has meaning relative to the current box. Reshaping B while preserving the old offset makes the frontier slice jump to an unexpected world position (because `faceBlock()` and `frontierBlock()` both depend on the new bounds). Silent stale cursor state attached to new geometry.

**Implementation:** `Selection.markB()` sets `frontierOffset = 0`. `markA` already did this.

### Phase 17: Ctrl+click pivots at frontier face ✓

**Extends Phase 12.** When a full selection exists, Ctrl+click no longer reorients the same box — it pivots at the frontier face, reseeding the tool for continued extrusion. Enables stairs, roads, L-shapes.

**Mental model:** The tool becomes a continuous stroke. Mark a footprint, extrude, pivot, extrude. Each pivot rewrites A/B to a new 1-thick seed box at the front face of the current stroke.

**Pivot rules:**

- **No selection**: no-op ("The stone remembers no mark.").
- **A-only**: old reorient-from-look behavior — set normal from look, frontier=0. No box change (there is no box yet).
- **Complete selection** (A+B) + Ctrl+click:
  - New normal = dominant axis of player's look direction.
  - **Same direction**: denial — "The stone already faces {dir}."
  - **Opposite direction** (same axis, opposite sign): flip in place — box preserved, normal flipped, frontier=0. "The stone turns to face {dir}." Lets the player work the same volume from the other side without re-marking.
  - **Orthogonal direction**: pivot at frontier face.

**Frontier face** = `frontierBlock(frontierOffset)` on the old normal axis. The single consistent pivot point regardless of whether the player last placed or collected.

**Geometry of the pivot** (O = old normal axis, N = new normal axis, T = third axis):

- **T**: preserve old box's full extent `[Tmin, Tmax]`.
- **O**: collapse to frontier face coord (single block thickness).
- **N**: collapse to one end of old N extent, based on new normal sign:
  - `maxN` if new normal is positive (Up, South, East)
  - `minN` if new normal is negative (Down, North, West)

Then: `normal = newNormal`, `frontier = 0`.

**Example:** `3x3x5` box, normal=Z, scrolled +2. Ctrl+click looking Up.

- O=Z → collapse to `frontierBlock(2)`.
- N=Y, Up is positive → collapse to `maxY`.
- T=X → preserve `[minX, maxX]`.
- Result: 3x1x1 box, normal=Up, frontier=0. Scroll up places a 3x1 slab at `maxY+1`.

**Intentional behaviors:**

- Thick volumes "hug" the edge matching the turn direction (not center).
- Pivoting when frontier has moved beyond the original box seeds the new stroke outside the original selection — this is the desired stroke behavior.
- No player position logic — geometry is deterministic from normals and bounds.
- No undo — each pivot rewrites A and B permanently. Re-mark to start a new stroke.

**Implementation approach:** Bounds-math. Extract old min/max on each world axis, identify O/N/T by axis, rewrite bounds per rule above, construct new A and B from the resulting mins/maxes.

**Files modified:**

- `StoneOfMendingMod.java` — handler reworked: detect complete selection, compute pivot from bounds, rewrite A/B.
- `Selection.java` — `setPoints(BlockPos, BlockPos)` setter.
- Message: "The stone pivots toward {dir}." for pivot case; existing "now faces" for A-only fallback.

**Not included:**

- Undo / box history — each pivot rewrites permanently.
- Opposite-direction flip — no-op for now.
- Non-orthogonal pivots — `normalFromLook` already collapses to axes.

### Phase 16: Ctrl+scroll — Border-only slices ✓

Ctrl+scroll down/up collects/places only the perimeter ring of the current slice, not the full area.

**Geometry:** `SelectionBox.borderPositions(int offset)` — efficient perimeter iteration. If either non-normal dimension ≤ 2, returns the full slice (all blocks are border). Otherwise emits first row, last row, and left/right edges for middle rows. No duplicates.

**Input:** `ScrollActionC2SPayload` gains a `ctrl` boolean. Client mixin detects Ctrl key state alongside Shift. Shift wins over Ctrl (Ctrl+Shift+scroll = shift behavior).

**Actions:** `ScrollActions.collectBorder()` and `ScrollActions.placeBorder()` — dedicated methods using `borderPositions()` with border-aware preflight checks. Same frontier semantics as normal scroll (advance one step per scroll). Same inventory-limited collection rules. Same sounds and messages.

**Server dispatch:** shifted → interior actions, else ctrl → border actions, else → normal actions.

**Not included:** Ctrl+Shift combo (shift wins), Ctrl+middle-click border replace (deferred).

**Files modified:**

- `SelectionBox.java` — `borderPositions(int offset)`
- `ScrollActionC2SPayload.java` — add `ctrl` field
- `MouseHandlerMixin.java` — detect Ctrl key, send it
- `StoneOfMendingMod.java` — dispatch ctrl to border actions
- `ScrollActions.java` — `collectBorder()`, `placeBorder()`

## MVP constraints

- One active selection per player.
- No block entities, no NBT preservation.
- Loot-table drops but no tool context (no silk touch, no fortune).
- No complex placement (stairs, slabs, oriented blocks).
- No undo, no saved selections.
- No selection size cap (the Stone of Mending is powerful by design).
- No translation keys yet (literal English strings for overlay messages).

## Done condition

A player can hold the Stone, mark a 3D box, see the full selection outline and frontier slice, scroll down to collect layers inward (mining-style drops), scroll up to fill the next incomplete layer from offhand, and middle-click to replace all blocks in the box with offhand material. Placement and replacement auto-refill from inventory. Collection and replacement stop when inventory is full (no drops on ground). The tool extends beyond the original selection in both directions. Ctrl+click to re-orient the slicing direction based on look direction. The item has a custom texture (dark stone with teal mending veins) and display name.
