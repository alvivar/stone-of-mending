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
- Marking A always starts a new selection and resets the frontier.
- Clicking air does nothing.
- Selection persists per-player until replaced or logout.
- A collectible block: non-air, no block entity, has a direct item form.
- A placeable target: offhand holds a BlockItem, target space is replaceable.
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

- Register the Stone of Mending item.
- Server-side per-player selection state: point A, point B, plane normal, dimension, frontier offset.
- Packet sync from server to client for rendering.

### Phase 2: Marking behavior ✓

- Left-click marks A (locks face normal and dimension), right-click marks B.
- B accepted in the same dimension, no plane constraint.
- Cancel normal block interaction when the Stone handles the click.

### Phase 3: Selection rendering ✓

- Client-side outline in world space.
- Single-block marker when only A exists.
- Dim outline for the full 3D box when A and B exist.
- Bright outline for the current frontier slice.
- Frontier slice tracks the offset, including beyond the original box.

### Phase 4: Scroll input ✓

- Client mixin intercepts mouse wheel in MouseHandler.onScroll.
- When holding the Stone with active complete selection in matching dimension: cancel hotbar scroll, send direction packet to server.

### Phase 5: Collection (scroll down)

- Use SelectionBox.slicePositions() to iterate the frontier slice.
- Skip air and unsupported blocks.
- Resolve direct item form, insert into inventory, drop overflow.
- Remove block from world.
- Advance frontier one layer inward.
- Sync selection to client. Action bar feedback.

### Phase 6: Placement (scroll up)

- Read offhand stack, require a placeable block item.
- Use SelectionBox.slicePositions() for the next outward slice.
- Place block, consume one item per placed block.
- Stop placing when materials run out or all positions blocked.
- Advance frontier one layer outward.
- Sync selection to client. Action bar feedback.

## MVP constraints

- One active selection per player.
- No block entities, no NBT preservation.
- No loot tables, silk touch, or fortune.
- No complex placement (stairs, slabs, oriented blocks).
- No undo, no saved selections.
- No selection size cap (the Stone of Mending is powerful by design).

## Done condition

A player can hold the Stone, mark a 3D box, see the full selection outline and frontier slice, scroll down to collect layers inward, and scroll up to place layers outward from the offhand — extending beyond the original selection in both directions.
