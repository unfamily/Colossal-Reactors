---
navigation:
  parent: turbine/turbine-index.md
  title: Turbine Builder
  icon: turbine_builder
  position: 30
item_ids:
  - colossal_reactors:turbine_builder
categories:
  - multiblock
---

# Turbine Builder

<BlockImage id="turbine_builder" scale="4" />

The **Turbine Builder** places casing, glass, rod controllers, rods, blades, and coil blocks from supplied materials—similar to the [Reactor Builder](../multiblock/reactor_builder.md).

## Typical workflow

1. **Place** the builder against your turbine footprint (orientation matters).
2. **Open** the screen and set **width, height, depth** (within config limits).
3. Load the buffer with **casing**, **glass**, **rod controller**, **rods**, **blades**, and **coil blocks** (storage blocks matching your chosen coil type).
4. Choose **rod pattern**:
   - **Efficient** — checkerboard rod columns; blade rings grow with layer height (better blade efficiency bonus).
   - **Productive** — every rod column filled; max blade ring on each layer (maximum steam cap sooner).
5. Set **coil type** (cycles datapack entries) and **coil layer count** (default **3**, ◀ ▶ in GUI).
6. Use **preview** and **simulation** to check estimated RF/t, steam use, and material counts.
7. **Mark input** assigns buffer slots to specific items for each build stage.
8. **Build** when ready; supply fluids to the builder tank if marking steam input for validation.

Build order: **frame** → **rod controllers + rods** → **blades** → **coil blocks** in the top interior layers.

## Simulation panel

Shows status, RF/t, steam mB/t, coil and blade efficiency, and **required materials**—same layout philosophy as the reactor builder.

Exact button labels depend on your game version; read tooltips on the builder screen while operating it.
