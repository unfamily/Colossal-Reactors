---
navigation:
  parent: multiblock/multiblock-index.md
  title: Reactor Builder
  icon: reactor_builder
  position: 30
item_ids:
  - colossal_reactors:reactor_builder
categories:
  - multiblock
---

# Reactor Builder

<BlockImage id="reactor_builder" scale="4" />

The **Reactor Builder** places the **reactor interior and shell** from materials in its buffer and tank. It does **not** place the **Reactor Controller**, **power ports**, or **resource ports** — see [Getting started](getting_started.md).

## What Build places (in order)

1. **Casing** shell (optionally **open top** for later edits)
2. **Rod controllers** on the top layer above each rod column
3. **Reactor rods** in the interior pattern
4. **Fluid** heat sinks (1000 mB per cell) when configured
5. **Solid** heat sink blocks in remaining interior cells

## Typical workflow

1. **Place** the builder against the intended footprint (orientation matters).
2. **Open** the GUI: set **size**, **rod pattern**, **pattern mode**, heat sink type, and **open top** if needed.
3. **Preview** — purple border, yellow rod positions, red = invalid/extra blocks.
4. **Mark input** — assign buffer slots to specific items (see button tooltips).
5. **Build** when the buffer/tank hold enough materials; **Stop** if red zones appear or you need to edit.
6. **Remove or ignore** the builder block — it is not part of the multiblock.
7. **Manually** add [controller, power port, and resource port(s)](getting_started.md) after the build — controller **outside** one wall; ports on that **same** face, replacing **glass** cells.

**Simulation** in the builder estimates stats and material counts; it does **not** start the real reactor.

<GameScene zoom="4" background="transparent">
  <ImportStructure src="/assets/assemblies/minimal_reactor_automation.snbt" />
  <IsometricCamera yaw="210" pitch="25" />
</GameScene>

Example **finished** layout (builder shell + hand-placed controller and ports). See [Getting started](getting_started.md) for labels.
