# Ideas

Design notes for features not yet in PLAN.md. Entries here are brainstormed and
reviewed but not yet committed. Move into PLAN.md when ready to implement.

---

## Feature A — Passive top-up — ✓ moved to PLAN.md (Phase 21)

**Concept**: While held in main hand, the stone slowly tops up low-count
expedition basics. Maintenance semantics, not farming.

### Rules

- **Items**: arrows and torches only. No third item. Tight scope wins.
- **Main hand only** (same rule as passive repair)
- **Runs in parallel with repair** — both passives tick independently
- **Selector**: whichever supported item has the lowest `count / cap` ratio
  (absolute count is the wrong comparison — 4 torches vs 10 arrows is
  meaningless)
- **Per-item caps**: arrows 64, torches 64 (one stack each)
- **Stops entirely when both items are at or above cap** — no trickle above
  stocked
- **Cadence**: one item every 1000 ticks (50 seconds, one Minecraft hour)
- **Timer persistence**: the top-up counter pauses when the stone is not in
  main hand, resumes from where it left off when it is. Switching to another
  item mid-cycle doesn't reset progress. Same pattern as the existing repair
  counter.
- **Inventory-full behavior**: if no room to insert, skip silently (same
  spirit as collect — inventory is the limit)
- **Silent** — no sound, no overlay message. Invisible like repair.

### Cost model

Decided: **parallel, no tradeoff**. Both repair and top-up tick on their own
timers. The cap is the throttle. GPT argued for "repair pauses while
producing" (one shared budget) — rejected because both passives are already
invisible to the player; adding a hidden mutual-exclusivity makes the mechanic
unlearnable.

---

## Feature B — Template memory

**Concept**: Save the current box's dimensions, then replay it on the next
Mark A. Turns the stone into a reusable stamp for repeating geometry
(hallways, rooms, pillars).

### Rules

- **One slot only** — no template library. Shift+right-click on a complete box
  overwrites the stored template.
- Stores just `{width, height, depth, normal}` — not world position, not
  material
- **Replay**: next Mark A on a block → instead of waiting for Mark B, the
  stone auto-constructs the saved shape extending from the clicked position in
  the direction the player is facing
- Clear feedback if the saved shape can't fit (world height, unloaded chunks,
  etc.)
- Explicit save action, explicit use action — no implicit state

### Why this and not others

GPT's ranking (identity-aligned first): template memory > ghost preview >
mending burst > top-up > undo > idle lore. Template memory strengthens the
stone's core identity (geometry, repetition, drafting instrument) where most
other candidates dilute it into generic magical utility.

---

## Parked candidates

Considered, not chosen. Preserved here so we don't re-debate them.

- **Mending burst**: active right-click in air spends XP to insta-repair held
  item. On-brand by name but redundant with passive repair. Extends
  "mending," not the building tool.
- **Ghost preview**: show the would-be box between Mark A and Mark B while
  aiming at the eventual B corner. Safe UX polish. Low risk, low drama — keep
  as a polish pass, not a feature.
- **Undo last action**: one-level undo for collect/place. High value but
  state-heavy, easy to make ugly.
- **Idle flavor lore**: rare messages while held ("The stone hums softly...").
  Cheap, but clutters chat and dilutes the existing voice. Rejected.

---

## Guiding principle (from GPT)

> Let the stone be a builder's companion, not a vending machine.

Every proposed feature should either deepen the geometry/mending identity or
stay restrained enough not to drift it.
