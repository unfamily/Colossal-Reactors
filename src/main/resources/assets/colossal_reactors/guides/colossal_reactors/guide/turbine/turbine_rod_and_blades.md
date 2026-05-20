---
navigation:
  parent: turbine/turbine-index.md
  title: Rods and blades
  icon: turbine_blade
  position: 15
item_ids:
  - colossal_reactors:turbine_blade
  - colossal_reactors:turbine_rod
categories:
  - multiblock
---

# Rods and blades

<Row gap="16" fullWidth={true}>
  <BlockImage id="turbine_rod" scale="4" />
  <BlockImage id="turbine_blade" scale="4" />
</Row>

## Turbine Blade item

The **Turbine Blade** does **not** place like a normal block from your hand. **Right-click a Turbine Rod** with the blade item to attach the next blade on that rod.

Each click consumes one item and grows blades in **balanced rings** around the rod:

1. **First ring** — 2 blades on opposite sides.
2. **Second ring** — 4 blades (one per lateral side).
3. **Further rings** — 8, 12, 16… up to the configured maximum radius (default **31** rings → up to **124 blades per rod**).

The algorithm picks the side and distance so rings stay balanced (same depth on all four lateral axes before advancing).

## Blade block behaviour

- Blades are **ethereal**: no collision, not mineable by hand, no direct loot.

- When a **rod is broken**, every blade on that rod is removed and dropped as items **at the rod position** (no need to hunt blades inside the turbine).

## Blade efficiency (layers)

Along the rod controller **axis**, the game groups blades into **layers** (horizontal slices when the axis is vertical).

| Layer pattern | Effect on blade efficiency |
|---------------|----------------------------|
| More blades on layer N+1 than N | **Bonus** per step (default **+3%** each, configurable) |
| Equal blade count | Neutral |
| Fewer blades on N+1 than N | **Breaks** the bonus chain; each drop applies a **malus** (default **−3%** each) |

For maximum blade bonus, grow ring counts **from the bottom up** (Efficient builder pattern). Filling every layer at max radius immediately (Productive pattern) skips ascending bonuses but reaches peak steam capacity faster.

Balanced rings (4, 8, 12…) may be required for a blade to count toward steam capacity when `requireBalancedBladeRings` is enabled in config.

## Builder patterns

The [Turbine Builder](turbine_builder.md) can place rods and blades automatically:

- **Efficient** — checkerboard rod columns; blade rings grow with height (ascending layout).
- **Productive** — all rod columns filled; max blade ring on every layer.

Manual placement with the blade item follows the same ring rules as the builder.
