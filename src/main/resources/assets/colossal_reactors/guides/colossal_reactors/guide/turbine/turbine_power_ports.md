---
navigation:
  parent: turbine/turbine-index.md
  title: Turbine Power Ports
  icon: turbine_power_port
  position: 36
item_ids:
  - colossal_reactors:turbine_power_port
  - colossal_reactors:turbine_high_cond_power_port
categories:
  - ports
---

# Turbine Power Ports

<Row gap="16" fullWidth={true}>
  <BlockImage id="turbine_power_port" scale="4" />
  <BlockImage id="turbine_high_cond_power_port" scale="4" />
</Row>

Turbine power ports forward generated **RF** from the turbine to cables, ducts, or machines on the **outside**. There is **no screen**—wire the outside face and read flow from your energy network.

## Turbine Power Port

The standard **Turbine Power Port** uses `int` buffer and transfer limits (Forge FE), suitable for most modpacks.

## Turbine High Conduction Power Port

The **Turbine High Conduction Power Port** is the upgraded variant for very large RF buffers and per-tick transfer beyond the standard port’s `int` limits.

- Uses **long** energy storage and extraction (tunable in Colossal Reactors **ports → turbine** config).

- On each tick, pushes stored energy to neighbors in order: **OP** (Brandon’s Core / Draconic Evolution) when exposed, then **Flux Networks** long transfer when installed, then standard **Forge FE**.

- Craft from a **Turbine Power Port** plus high-tier materials; check **JEI** for your pack.

## Features (both variants)

- Place on an outer face of the casing; the **inside** must face the turbine interior.

- Connect compatible energy handlers from other mods on the **outside** of the port.

Generated RF comes from steam consumption × generation rate × coil × blade efficiency—see [Steam, RF, and coils](turbine_generation_and_coils.md).
