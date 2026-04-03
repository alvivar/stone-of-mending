# Stone of Mending — Plan

## Tool

A face-based building tool. Mark a rectangular selection on a block face, then scroll to move a frontier plane forward or backward one layer at a time.

- Scroll down collects the current layer into the player's inventory.
- Scroll up places a new layer from the offhand material.

## Mechanics

### Selection and collection

- Hold the Stone of Mending in the main hand.
- Left-click a block face to mark point A.
- Right-click a block face to mark point B.
- A and B define a rectangular selection on a single face-aligned plane, one block thick.
- Scroll down collects every supported block in the current plane.
- The frontier moves one block inward after collection.

### Placement and growth

- Keep the Stone of Mending in the main hand, placement material in the offhand.
- Scroll up places one new layer on the outward side of the current plane.
- One block item consumed per placed block.
- The frontier moves one block outward after placement.

## Locked decisions

- Selection is always a 2D face-aligned rectangle, one block thick.
- First marked face locks the plane normal. Second mark must be on the same plane.
- Outward = direction of the first marked face normal. Inward = opposite.
- Selection is a moving frontier, not a fixed area.
- Marking A always starts a new selection and resets the frontier.
- Marking B on a different plane is rejected. A is kept, action bar feedback.
- Clicking air does nothing.
- Selection persists per-player until replaced or logout.
- The frontier moves only if at least one block was successfully collected or placed.
- A collectible block: non-air, no block entity, has a direct item form.
- A placeable target: offhand holds a BlockItem, target space is replaceable.
- Placement material comes from the offhand slot.
- Collection uses direct block-to-item pickup, not loot tables.
- Collection and placement are server-side. Rendering is client-side.
- Server validates: held item, active selection, loaded target positions before any world edit.
- Mouse wheel is intercepted only while holding the Stone with an active selection.

## Implementation

### Phase 1: Item and selection state ✦

- Register the Stone of Mending item.
- Server-side per-player selection state: point A, point B, plane normal, frontier offset.
- Packet sync from server to client for rendering.

### Phase 2: Marking behavior

- Left-click marks A, right-click marks B.
- B accepted only on the same plane as A.
- Compute plane normal, in-plane axes, min/max bounds, frontier plane.
- Cancel normal block interaction when the Stone handles the click.

### Phase 3: Selection rendering

- Client-side outline in world space.
- Single-point marker when only A exists.
- Box outline for the full slab when A and B exist.
- Renders the current frontier plane, not the original.

### Phase 4: Scroll input

- Small client mixin to intercept mouse wheel.
- When holding the Stone with active selection: cancel hotbar scroll, send direction packet to server.

### Phase 5: Collection (scroll down)

- Iterate every block in the current frontier plane.
- Skip air and unsupported blocks.
- Resolve direct item form, insert into inventory, drop overflow.
- Remove block from world.
- Move frontier one block inward.
- Action bar feedback for results.

### Phase 6: Placement (scroll up)

- Read offhand stack, require a placeable block item.
- For each position in the outward plane: place block, consume one item.
- Stop when materials run out or space is blocked.
- Move frontier one block outward.
- Action bar feedback for results.

## MVP constraints

- One active selection per player.
- No block entities, no NBT preservation.
- No loot tables, silk touch, or fortune.
- No complex placement (stairs, slabs, oriented blocks).
- No undo, no saved selections.
- Simple full blocks only.

## Done condition

A player can hold the Stone, mark a face-aligned rectangle, see the frontier rendered, scroll down to collect layers, and scroll up to place layers from the offhand.
