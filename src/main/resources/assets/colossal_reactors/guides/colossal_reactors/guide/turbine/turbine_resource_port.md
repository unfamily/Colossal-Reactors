---
navigation:
  parent: turbine/turbine-index.md
  title: Turbine Resource Port
  icon: turbine_resource_port
  position: 35
item_ids:
  - colossal_reactors:turbine_resource_port
categories:
  - ports
---

# Turbine Resource Port

<BlockImage id="turbine_resource_port" scale="4" />

Attach **Turbine Resource Port(s)** to the casing to move **steam**, **water**, and other pack-allowed fluids between pipes/tanks and the turbine.

Typical layout: **two resource ports + one power port** on the same face as the [Turbine Controller](turbine_controller.md) — see [Getting started](getting_started.md). Hover the diamonds in the scene below.

<GameScene zoom="6" interactive={true} background="transparent">
  <ImportStructure src="/assets/assemblies/minimal_turbine.snbt" />
  <IsometricCamera yaw="200" pitch="28" />
  <BlockAnnotation x="1" y="2" z="0" color="#55ffff">
**Example: steam IN** — **Insert** mode, **Liquid** enabled. Tooltip: *Pulls steam (and other allowed fluids) into the turbine.*
  </BlockAnnotation>
  <BlockAnnotation x="5" y="2" z="0" color="#5599ff">
**Example: water OUT** — **Extract** mode, **Liquid** enabled. Tooltip: *Pushes water or exhaust fluid out to pipes.*
  </BlockAnnotation>
</GameScene>

Pipe steam from your [fission reactor](../multiblock/getting_started.md) (or any steam source) into the **INSERT** port. Pipe **water / exhaust** out of the **EXTRACT** port. How much the turbine can use depends on your blades — see [Steam, RF, and coils](turbine_generation_and_coils.md).

## GUI — buttons and tooltips

The turbine port uses the same screen layout as the reactor port, but **no Solid row** and **no Fuel/Coolant filter**. Hover each control in-game — text below matches the default tooltips.

### Mode (top button — click to cycle)

| Label | Tooltip |
|-------|---------|
| **Insert** | Pulls steam (and other allowed fluids) into the turbine. |
| **Extract** | Pushes water or exhaust fluid out to pipes. |
| **Eject** | Ejects stored fluid from the port tank to the outside. |

### Medium toggles (click to enable — underlined when active)

| Button | Tooltip |
|--------|---------|
| **Liquid** | Click to allow or block fluids through this port. |
| **Gas** | Click to allow or block Mekanism gases through this port. |

### Tanks and dump

| Control | Tooltip |
|---------|---------|
| **Liquid tank** (hover bar) | Liquid: amount / capacity mB, plus fluid name |
| **Gas tank** (hover bar, Mekanism) | Gas: amount / capacity mB, plus gas name |
| **D** under liquid tank | Dump: empty the internal fluid tank (fluid is discarded) |
| **D** under gas tank | Dump: empty the gas tank (gas is discarded) |

Pipes and tanks connect on the **outside**; the **inside** face must point into the turbine.

After changing modes, **Reboot** the [Turbine Controller](turbine_controller.md).

## Automatic capacity scaling

When the turbine **re-validates**, the port’s fluid tank resizes from estimated **steam mB/t × 10**, with a **16,000 mB** floor and **no fixed upper limit**. Capacity grows immediately when demand increases and shrinks only if stored steam still fits the smaller size.
