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

Open the controller by **right-clicking** the block when it sits on a **side** face of the casing (not top or bottom), **replacing a casing block** on the outer frame with the screen facing **outward**.

New to multiblocks? Start with [Getting started](colossal_reactors:multiblock/getting_started.md).

## Operation

- **Validate / run** — first open runs validation; when valid the reactor can turn **ON** and show RF/tick, fuel, and coolant readouts.
- **Fuel** — move fuel between your inventory and rod storage when slots are shown.
- **Coolant fluid** — view level and fill/drain when controls are provided (optional for **RF-only** play with an empty coolant buffer).
- **Reboot** — re-validates the structure and refreshes port tank sizes after you change the build or port settings.

## Stopping without breaking the multiblock

Use a [Reactor Redstone Port](redstone_port.md) if you want external redstone to **hold off** or **stop** operation while the shell stays formed. **Without** a redstone port, the reactor runs whenever it is valid and **ON** (no redstone gate).

If **stability** readouts are enabled for your pack, they appear here — see [Reactor instability](reactor_instability.md).

## Reactor valid but not producing?

| Check | |
|-------|---|
| Power port present and cabled? | RF needs a [Power Port](power_ports.md) with something accepting energy on the **outside**. |
| RF-only mode? | Empty coolant buffer = no fluid required; water/steam mode needs coolant + extract path. |
| Redstone port? | Reactor may stay **OFF** until the port’s mode + signal allow run. |
| Waste backing up? | Add an **EXTRACT** [Resource Port](resource_port.md) or empty waste manually. |

Tooltips and the status line on this screen explain the current state.
