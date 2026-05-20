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

- Use **Turbine Casing** for opaque walls and **Turbine Glass** for transparent faces.

- The interior must form a **closed box** (default maximum size **65×65×65**, same scale as reactors—tunable in config).

- Leave room for the **rod zone**, **coil zone** at the top of the interior, **ports** on the casing, and exactly one **Turbine Controller** on an outer face.

## Rod controller

<BlockImage id="turbine_rod_controller" scale="4" />

The **Turbine Rod Controller** sits on the **closure layer** (top interior Y of the rotor section). There is **exactly one** per turbine. It defines the **axis** of the multiblock (six facing directions, like heating coils).

- If the rod footprint is **odd×odd**, the controller goes on the single geometric center cell.
- If one side is **even**, **two** center cells are valid (1×2 or 2×1); if **both** are even, **four** cells are valid (2×2). Manual builds may use any valid center; the **Turbine Builder** places the first valid center (minimum X, then Z) and preview shows that cell in white.

- Rod columns are placed by pattern below the coil zone—not necessarily under the controller.

- Rods must align with the controller axis (or the opposite direction). Misaligned rods are corrected during validation when possible.

- This block is **not** the **Turbine Controller** on the shell—that block runs the turbine GUI and tick logic.

## Turbine rods

<BlockImage id="turbine_rod" scale="4" />

- **Turbine Rods** are vertical or horizontal columns depending on the rod controller facing.

- Place with **six-way** orientation: click floor/ceiling for a vertical column, or a wall for a horizontal column along that wall.

- **Turbine Blades** attach to rods (see [Rods and blades](turbine_rod_and_blades.md)); breaking a rod drops all blades on that rod at the rod position.

## Interior zones (overview)

| Zone | Location | Contents |
|------|----------|----------|
| **Rod + blade** | Below the coil cap | Rod columns, blades on rods |
| **Electrical coil** | Top layers of interior | Storage blocks from datapack (gold, copper, etc.) |
| **Ports** | Casing faces | Steam in, RF out, redstone |

Layer split and coil depth are configured in the [Turbine Builder](turbine_builder.md) (default **3** coil layers). See [Steam, RF, and coils](turbine_generation_and_coils.md) for production math.
