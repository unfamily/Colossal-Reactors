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

Attach **Reactor Resource Port(s)** to the **casing** to move items and/or fluids between the outside world and the reactor.

## Typical layouts

### Manual / RF-only (no ports required)

Fuel and waste can be handled through the [Reactor Controller](reactor_controller.md) GUI. You still need at least one [Power Port](power_ports.md).

### Automation: fuel in + waste out (**two ports**, same face as controller)

Put **both resource ports on the same wall** as the reactor controller. Example in [Getting started](getting_started.md) — ports are spread on that face; hover the diamonds in the scene for setup hints.

| Port (example) | Mode | Filter | Moves |
|----------------|------|--------|-------|
| Lower-left | **INSERT** | **Fuel** | Solid fuel **into** rods |
| Lower-right | **EXTRACT** | **Waste** | **Waste** **out** |

Add a **power port** on that same face. Pipe/hopper on the **outside** of each port.

<GameScene zoom="6" interactive={true} background="transparent">
  <ImportStructure src="/assets/assemblies/minimal_reactor_automation.snbt" />
  <IsometricCamera yaw="200" pitch="28" />
  <BlockAnnotation x="1" y="2" z="0" color="#55ff55">
**Example: fuel IN** — open the port → **Insert** → **Fuel** filter → enable **Solid** / **Liquid** as needed. Tooltip on **Insert**: *The reactor accepts solid fuel and a coolant fluid.*
  </BlockAnnotation>
  <BlockAnnotation x="5" y="2" z="0" color="#ffaa55">
**Example: waste OUT** — **Extract** → **Waste** filter. Tooltip on **Extract**: *The reactor extracts nuclear waste and spent coolant fluid.*
  </BlockAnnotation>
</GameScene>

### Fluid coolant loop (**extra ports**, optional)

If you run **active coolant** (Setup 2 in [Getting started](getting_started.md)):

- **INSERT** with **Coolant** filter — pipe coolant **in** (*Import: coolant fluid only.*)
- **EXTRACT** with **Coolant** filter — pipe **steam / spent fluid** **out** → [Turbine Getting started](../turbine/getting_started.md) (*Output: exhausted liquid coolant only.*)
- Keep **EXTRACT waste** on a separate port; do not expect one EXTRACT port to eject unrelated fluids.

## GUI — buttons and tooltips

Open the port GUI and **hover each control** — the text below is what you see in-game (English).

### Mode (top button — click to cycle)

| Label | Tooltip |
|-------|---------|
| **Insert** | The reactor accepts solid fuel and a coolant fluid. |
| **Extract** | The reactor extracts nuclear waste and spent coolant fluid. |
| **Eject** | The reactor ejects the solid fuel it contains. |

### Filter (reactor only — click to cycle fuel / coolant role)

Depends on **mode**; hover the button for the current filter:

| Mode | Filter label | Tooltip |
|------|--------------|---------|
| **Insert** | All / Fuel / Coolant | *Import: fuel and coolant.* / *Import: fuel only (items or fluid).* / *Import: coolant fluid only.* |
| **Extract** | All / Waste / Coolant | *Output: solid waste and liquid coolant.* / *Output: solid waste only.* / *Output: exhausted liquid coolant only.* |
| **Eject** | Both / Fuel Only / Coolant Only | *Eject: fuel items and coolant fluid from rods.* / *Eject: fuel items only…* / *Eject: coolant fluid only.* |

### Medium toggles (click to enable — underlined when active)

| Button | Tooltip |
|--------|---------|
| **Solid** | Click to allow or block solid items through this port. |
| **Liquid** | Click to allow or block fluids through this port. |
| **Gas** | Click to allow or block Mekanism gases through this port. |

### Tanks and dump

| Control | Tooltip |
|---------|---------|
| **Liquid tank** (hover bar) | Liquid: amount / capacity mB, plus fluid name |
| **Gas tank** (hover bar, Mekanism) | Gas: amount / capacity mB, plus gas name |
| **D** under liquid tank | Dump: empty the internal fluid tank (fluid is discarded) |
| **D** under gas tank | Dump: empty the gas tank (gas is discarded) |

- **Item slot** — manual I/O or hopper/pipe on the **outside** face.

After changing modes or filters, **Reboot** the [Reactor Controller](reactor_controller.md).

## Automatic capacity scaling

When the reactor **re-validates**, each resource port resizes fluid/gas tanks (Mekanism gas when enabled):

- Target ≈ **estimated mB/t for that port’s mode and filter × 10**, minimum **16,000 mB** per tank.
- **No fixed upper cap** on fluid/gas volume for large reactors.
- Capacity **grows** immediately when demand increases; **shrinks** only when stored contents still fit the smaller size.
