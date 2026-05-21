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

The **Turbine Builder** builds the turbine for you from items in its inventory—similar to the [Reactor Builder](../multiblock/reactor_builder.md).

## Typical workflow

1. **Place** the builder on the face where you want the turbine shell to start.
2. **Open** the screen and set **width, height, and depth** (within your pack’s size limit).
3. Stock the builder with **casing**, **glass**, **rod controller**, **rods**, **blades**, and the **storage blocks** you want for coils.
4. Pick a **layout**:
   - **Efficient** — fewer rod columns (checkerboard); blades grow ring by ring up the shaft (**+3%** height bonus possible—see [Rods and blades](turbine_rod_and_blades.md)).
   - **Productive** — rods on every allowed column; each layer gets full rings right away (maximum steam sooner).
5. Choose **coil type** (cycles the metals the mod supports) and **how many coil layers** (default **3**, change with ◀ ▶ on screen).
6. Use **preview** and the **simulation** panel to see estimated RF/t, steam use, and materials needed.
7. **Mark input** tells the builder which inventory slots supply which blocks for each step.
8. Press **Build** when ready. You can add fluid to the builder tank if your pack uses that for setup checks.

Build order: **outer shell** → **rod controller and rods** → **blades** → **coil blocks** in the top interior layers.

## Simulation panel

Shows whether the layout is valid, expected **RF/t** and **steam**, coil and blade bonuses, and a **material list**—same idea as the reactor builder. Button names can vary by version; read the tooltips on screen while you work.
