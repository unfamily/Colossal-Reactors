---
navigation:
  parent: multiblock/multiblock-index.md
  title: Reactor Controller
  icon: reactor_controller
  position: 20
item_ids:
  - colossal_reactors:reactor_controller
categories:
  - multiblock
---

# Reactor Controller

<BlockImage id="reactor_controller" scale="4" />

Open the controller by **right-clicking** the block when the multiblock is valid.

## Operation

- **Start / stop** the reactor and monitor power output, fuel state, and related readouts.

- **Manage fuel**: move fuel items between your inventory and the reactor’s fuel slots when the screen allows.

- **Coolant fluid**: view level and fill or drain where controls are provided.

- **Scram / safety**: use the controller’s controls to shut down or recover from unsafe states when shown.

## Stopping the reactor without breaking it

To **turn the reactor off cleanly** (without mining controller or casing), route **stop / disable** through a **Redstone Port**: configure it so an external redstone signal can **halt** or **hold off** operation while the multiblock stays formed. Exact modes depend on your version—check the [Redstone Port](redstone_port.md) page.

If extra **stability** readouts are enabled for your pack, they appear on this screen—see [Reactor instability](reactor_instability.md) when relevant.

Tooltips and on-screen readouts explain current status; follow them while adjusting fuel, coolant fluid, heat-sink layout, or rods outside the controller screen.
