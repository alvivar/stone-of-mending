# Stone of Mending

A face-based building tool for selecting a 3D region, peeling it layer by layer, rebuilding it from your offhand, and replacing materials across the whole selection. Fabric mod for Minecraft 26.1.1.

## Features

- **Mark two corners** to define a 3D box selection
- **Collect layers** with mining-style drops (scroll down)
- **Place layers** from offhand material (scroll up)
- **Smart range actions** with Shift+scroll — scans the active range for the next incomplete or collectible slice
- **Replace all blocks** in the selection with offhand material (middle click)
- **Border-only slices** with Ctrl+scroll — collect or place just the perimeter ring, great for hollow structures
- **Reshape mid-stroke** — turn, flip, or pivot the selection with Ctrl+click; right-click commits at the working face. Build stairs, roads, L-shapes, or work a tower from below without re-marking
- **Frontier cursor** extends infinitely beyond the original box
- **Visual rendering** — dim cyan box for the selection, bright slice for the frontier
- **Sound feedback** — distinct audio cues for marking, collecting, placing, replacing, and errors
- **Offhand auto-refill** from inventory
- **Passive item repair** — while held, the stone slowly mends the most damaged item in your inventory (~7 min full repair)
- **No size cap** — intentionally powerful. Your inventory is the only constraint

## Quick Start

1. Grab the **Stone of Mending** from the Tools & Utilities creative tab
2. Hold it in your main hand — it passively repairs your most damaged item while held (~7 min full repair)
3. **Left-click** a block face to mark point A (this locks the slicing direction)
4. **Right-click** another block to mark point B (completes the 3D box)
5. Put blocks in your offhand
6. **Scroll down** to collect, **scroll up** to place, **middle-click** to replace

## Controls

| Input               | Action                                                                     |
| ------------------- | -------------------------------------------------------------------------- |
| Left-click block    | Mark point A (locks face normal / slicing axis)                            |
| Right-click block   | Mark point B — or commit the stroke at the frontier and set a new B        |
| Left-click air      | Clear selection                                                            |
| Scroll down         | Collect the frontier slice (mining drops), cursor moves inward             |
| Scroll up           | Place offhand material at the next outward slice, cursor moves outward     |
| Ctrl + Scroll down  | Collect border only — perimeter ring of the current slice                  |
| Ctrl + Scroll up    | Place border only — perimeter ring from offhand material                   |
| Shift + Scroll down | Smart collect: finds first collectible slice front→far across active range |
| Shift + Scroll up   | Smart fill: finds first incomplete slice far→front across active range     |
| Ctrl + left-click   | Turn the stone — reorient, flip, or pivot based on stroke state            |
| Middle click        | Replace all eligible blocks in the box with offhand material               |

## How It Works

### Selection

Point A defines a face and its normal direction — this becomes the slicing axis. Point B completes the opposite corner of the 3D box. The box renders as a dim cyan outline. The selection clears automatically when you switch to a different item.

### Turning and Reshaping

Both **Ctrl+left-click** and **right-click** adapt to where you are in a stroke. The rule is simple: `frontier=0` preserves the shape, `frontier≠0` commits at the working face.

**Ctrl+left-click** turns the stone toward the dominant axis of your look:

- Same direction — denial, nothing changes.
- Opposite direction — **flip** in place. Same box, worked from the other face.
- Orthogonal + no stroke — **reorient** the whole box, shape preserved.
- Orthogonal + stroke in progress — **pivot** at the current frontier face. Collapses to a one-thick seed you can extend in the new direction. This is how you build stairs and L-shapes.

**Right-click** commits when a stroke is in progress: it derives a fresh A from the frontier face opposite the click and sets B where you clicked. Reshape tracks where you actually are, not where you first marked. With no stroke, it just marks B normally.

### Frontier

The bright slice is the frontier cursor. It starts at the face you clicked (offset 0). Normal scroll moves it linearly — one slice per scroll tick. The frontier can move beyond the original box in either direction indefinitely.

### Normal Scroll

Linear cursor movement, one slice at a time.

- **Scroll down** collects the current frontier slice and advances inward (+1). Stops early if your inventory fills up — the frontier stays put until the slice is fully cleared.
- **Scroll up** places offhand material at frontier−1 and moves outward (−1).

On a valid, unblocked slice, normal scroll always moves the cursor — even if nothing was placed or collected. Blocked slices (unloaded chunks, out of build height) stop the cursor.

### Shift+Scroll (Smart Jump)

Scans the active range — the original box expanded to wherever the frontier has been — for the next interesting slice.

- **Shift+scroll up** fills the deepest incomplete slice (far→front scan).
- **Shift+scroll down** collects the first non-empty slice (front→far scan).

Inside the box, the cursor follows the action but stays clamped within the box boundaries. Outside the box, it only retracts toward the box — never pushes further away.

### Replace (Middle Click)

Swaps every eligible block in the entire box with the offhand material. Grants mining-style drops for removed blocks. Skips air, liquids, block entities, and blocks that already match the offhand material.

## Rules and Notes

- **Inventory is the limit** — collection and replacement stop when your inventory is full instead of dropping items on the ground. Partial progress is kept — the frontier stays on the current slice so you can make room and continue.
- **Mining-style drops** — blocks drop as if mined with an unenchanted netherite pickaxe. Stone → cobblestone, ores → ore drops, glass → nothing.
- **No silk touch, no fortune** — just a plain strong pickaxe.
- **Block entities are always skipped** — chests, furnaces, signs, etc. are never collected, placed over, or replaced.
- **Liquids skipped during replace** — water and lava source blocks are left untouched. Waterlogged blocks (stairs, slabs) _are_ replaced.
- **Offhand auto-refill** — when your offhand empties during placement, smart fill, or replacement, matching stacks are pulled from your inventory automatically.
- **Auto-clear on item switch** — selection clears when you switch away from the Stone of Mending.
- **No size cap** — this tool is intentionally overpowered. Select as large a region as you want.

## Requirements

- Minecraft 26.1.1
- Fabric Loader 0.18.6+
- Fabric API 0.145.4+26.1.1

## Installation

### Singleplayer

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.1.1
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and drop it in your `mods` folder
3. Drop `stone-of-mending-1.0.0.jar` into your `.minecraft/mods/` folder
4. Launch the game with the Fabric profile

### Dedicated Server

The mod must be installed on **both the server and every client**.

1. Install Fabric Loader on the server
2. Drop both Fabric API and `stone-of-mending-1.0.0.jar` into the server's `mods/` folder
3. Each player also needs the mod installed on their client (see above)

## Building from Source

Requires Java 25+.

```bash
git clone https://github.com/alvivar/stone-of-mending.git
cd stone-of-mending
./gradlew build
```

The built jar is `build/libs/stone-of-mending-1.0.0.jar` — copy it to your `.minecraft/mods/` folder.
