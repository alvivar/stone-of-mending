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
- Work pulses: `EXPERIENCE_ORB_PICKUP` (collect), `LODESTONE_PLACE` (place/fill), `RESPAWN_ANCHOR_CHARGE` (replace)
- Denial/limit: `BEACON_DEACTIVATE` (errors, clear), `RESPAWN_ANCHOR_DEPLETE` (inventory full)

**Silent on "nothing to do"** — avoids spam on empty slices during rapid scrolling.

**Helper:** `ScrollActions.playSound(ServerPlayer, SoundEvent, float volume)` — package-private, reused from all three files.

### Phase 15: Passive item repair ✓

While the Stone of Mending is held in main hand, it passively repairs the most damaged repairable item in the player's inventory.

**Rules:**
- Every 60 ticks (3 seconds), scan inventory (slots 0–35 + armor + offhand)
- Target: item with highest damage ratio (damage / maxDamage). Tie-break: highest absolute damage
- Repair amount: `max(1, maxDamage / 100)` — ~1% per tick, minimum 1
- Skip undamaged items, undamageable items
- No sound, no particles — silent passive effect
- No selection required — works anytime the stone is held
- Independent from SelectionManager — separate tick logic in StoneOfMendingMod

**Implementation:** Private `tickRepair(MinecraftServer)` method in StoneOfMendingMod, called from the same server tick event. Uses a static tick counter, fires every 20 ticks.

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
