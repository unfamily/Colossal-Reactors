---
navigation:
  parent: turbine/turbine-index.md
  title: Turbine structure
  icon: turbine_casing
  position: 10
item_ids:
  - colossal_reactors:turbine_casing
  - colossal_reactors:turbine_glass
  - colossal_reactors:turbine_rod
  - colossal_reactors:turbine_rod_controller
categories:
  - multiblock
---

# Turbine structure

## Building the shell

<Row gap="16" fullWidth={true}>
  <BlockImage id="turbine_casing" scale="4" />
  <BlockImage id="turbine_glass" scale="4" />
</Row>

- Use **Turbine Casing** for solid walls and **Turbine Glass** where you want to see inside.

- Build a **closed box**. Default maximum size is **65×65×65** (same idea as large reactors; your pack may allow less).

- Inside you need space for **rods and blades**, a **coil** section, **ports** on the walls, and one **Turbine Controller** on an outer **side wall** (north, south, east, or west—not the top or bottom of the box).

## Rod controller

<ItemImage id="turbine_rod_controller" scale="4" />

You need **exactly one** **Turbine Rod Controller** on the **top layer of the rotor section**. It sets which way the shaft runs (you can orient it in **six directions**, like a heating coil).

- On an **odd × odd** rod footprint, it sits on the **center** cell.
- If one side length is **even**, **two** center cells work; if **both** are even, **four** center cells work. Any of those is fine by hand; the **Turbine Builder** picks one center for you and highlights it in the preview.

- Rod columns are laid out by your pattern in the volume **below** the coil section—they do not have to sit directly under this block.

- Rods must line up with the controller’s axis. Wrong-facing rods are fixed when the turbine checks the build, when possible.

- This is **not** the **Turbine Controller** on the outside wall—that one opens the main turbine screen.

## Turbine rods

<ItemImage id="turbine_rod" scale="4" />

- **Turbine Rods** form the shaft. They follow the rod controller’s direction (up/down or along a wall).

- **Right-click** floor or ceiling for a **vertical** shaft; **right-click** a wall for a **horizontal** shaft along that wall.

- Attach **blades** with the blade item—see [Rods and blades](turbine_rod_and_blades.md). Breaking a rod drops all its blades on that spot.

## What goes where (overview)

| Area | Where | What you put there |
|------|--------|-------------------|
| **Rotor** | Main interior, below the coils | Rods, blades |
| **Coils** | Top layers inside | Metal storage blocks (gold, copper, …) |
| **Ports** | Casing faces | Steam in, RF out, redstone |

How many coil layers to use (default **3**) is set in the [Turbine Builder](turbine_builder.md). For RF and steam numbers see [Steam, RF, and coils](turbine_generation_and_coils.md).
