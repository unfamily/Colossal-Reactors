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

**Right-click** the controller when the turbine multiblock is **valid** to open the control screen.

## Operation

- **Start / stop** the turbine and monitor RF output, steam use, and efficiency readouts.

- The controller validates structure periodically while running (interval in config).

- Only **one** turbine controller per multiblock is allowed unless config enables multiples.

## Fluids and power

- **Steam** enters through [Turbine Resource Ports](turbine_resource_port.md) on the casing.

- **RF** exits through [Turbine Power Ports](turbine_power_ports.md).

- Exhaust fluid (e.g. water from steam) follows the active **turbine generation** datapack entry.

## Stopping without breaking the shell

Use a [Turbine Redstone Port](turbine_redstone_port.md) to halt or hold off operation from external redstone while keeping the multiblock formed—same idea as the fission reactor redstone port.

Read on-screen tooltips while tuning; blade layout and coil blocks outside the GUI strongly affect the numbers shown when running.
