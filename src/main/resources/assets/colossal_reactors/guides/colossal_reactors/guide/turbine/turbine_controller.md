---
navigation:
  parent: turbine/turbine-index.md
  title: Turbine Controller
  icon: turbine_controller
  position: 20
item_ids:
  - colossal_reactors:turbine_controller
categories:
  - multiblock
---

# Turbine Controller

<BlockImage id="turbine_controller" scale="4" />

**Right-click** the controller when the turbine is **built correctly** to open the main screen.

## Operation

- **Start and stop** the turbine and watch **RF output**, **steam use**, and efficiency on screen.

- The turbine is checked again from time to time while it runs; if something breaks, it stops and tells you why (action bar and GUI message).

- Normally you may only have **one** turbine controller per multiblock.

- Place it on a **side wall** of the shell (north, south, east, or west in the world—not the top or bottom). That is separate from which way the **rod shaft** runs inside.

## Fluids and power

- **Steam** is piped in through [Turbine Resource Ports](turbine_resource_port.md).

- **RF** leaves through [Turbine Power Ports](turbine_power_ports.md).

- Spent steam often becomes **water** (or another fluid your pack defines)—see [Steam, RF, and coils](turbine_generation_and_coils.md).

## Stopping without breaking blocks

A [Turbine Redstone Port](turbine_redstone_port.md) can shut the turbine down from outside redstone while leaving the structure intact—same role as a [Reactor Redstone Port](../multiblock/redstone_port.md).

What you see on the controller depends on your **blade layout** and **coil blocks**; adjust those inside the turbine, not only on this screen.
