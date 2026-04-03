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
- Placement acts one step outward from the current frontier.
- Placement only advances outward when the target slice is fully filled.
- When the offhand empties during placement, the inventory is searched for more of the same material.
- Collection and placement naturally undo each other.
- Marking A always starts a new selection and resets the frontier.
- Clicking air does nothing.
- Selection persists per-player until replaced or logout.
- A collectible block: non-air, no block entity, has a direct item form.
- A placeable target: offhand holds a BlockItem, target space is replaceable.
- Placement uses defaultBlockState() (no orientation or context).
- Placement material comes from the offhand slot.
- Collection uses direct block-to-item pickup, not loot tables.
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
  ScrollActions.java              — collect + place logic
  ClearSelectionC2SPayload.java   — C2S packet (clear selection)

src/client/java/io/github/alvivar/stoneofmending/
  StoneOfMendingClient.java       — client init, packet receiver, disconnect cleanup
  ClientSelectionState.java       — client-side selection mirror
  SelectionRenderer.java          — world-space outline rendering
  mixin/MouseHandlerMixin.java    — scroll interception
  mixin/StartAttackMixin.java     — left-click air clears selection
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

## MVP constraints

- One active selection per player.
- No block entities, no NBT preservation.
- No loot tables, silk touch, or fortune.
- No complex placement (stairs, slabs, oriented blocks).
- No undo, no saved selections.
- No selection size cap (the Stone of Mending is powerful by design).
- No item texture or model yet (shows as missing texture).
- No translation keys yet (literal English strings).

## Done condition

A player can hold the Stone, mark a 3D box, see the full selection outline and frontier slice, scroll down to collect layers inward, and scroll up to place layers outward from the offhand — extending beyond the original selection in both directions. Placement requires completing each slice before advancing, and auto-refills from inventory.
