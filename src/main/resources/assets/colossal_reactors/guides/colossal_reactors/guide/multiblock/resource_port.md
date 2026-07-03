---
navigation:
  parent: multiblock/multiblock-index.md
  title: Reactor Resource Port
  icon: resource_port
  position: 35
item_ids:
  - colossal_reactors:resource_port
categories:
  - ports
---

# Reactor Resource Port

<BlockImage id="resource_port" scale="4" />

Attach a **Reactor Resource Port** to the reactor casing to move items and/or fluids between the outside world and the reactor’s internal buffers.

## Features

- **Mode** cycles how the port behaves toward the reactor (insert items/fluid, extract, or eject to world—exact names appear on the button).

- **Filter** (when shown) narrows what the port accepts or pulls, depending on mode.

- **Fluid column**: hover for amount and fluid type. Use **D** under the tank to **dump** the port’s fluid storage when you need to empty it quickly.

- **Item slot**: interact manually or connect automation on the outside face per your modpack’s pipe rules.

## Automatic capacity scaling

When the reactor **re-validates**, each **resource port** resizes its fluid and gas tanks (if Mekanism gas is enabled):

- Target size is about **estimated mB/t for that port’s mode and filter × 10**, never below **16,000 mB** per tank.
- There is **no fixed upper limit** on fluid/gas volume — buffers can grow with very large reactors.
- Capacity **grows immediately** when demand increases. It **shrinks only** when stored fluid or gas still fits in the smaller target.

Use the controller **reboot / re-validate** control after you change fuels, coolants, or port modes so tank sizes stay matched to throughput.
