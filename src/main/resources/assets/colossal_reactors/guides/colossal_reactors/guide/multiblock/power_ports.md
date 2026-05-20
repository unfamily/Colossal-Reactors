---
navigation:
  parent: multiblock/multiblock-index.md
  title: Power Ports
  icon: power_port
  position: 36
item_ids:
  - colossal_reactors:power_port
  - colossal_reactors:high_cond_power_port
categories:
  - ports
---

# Power Ports

<Row gap="16" fullWidth={true}>
  <BlockImage id="power_port" scale="4" />
  <BlockImage id="high_cond_power_port" scale="4" />
</Row>

Power ports are **face-only** attachments: they forward generated energy from the reactor to cables, ducts, or machines on the **outside**. There is **no screen**—you wire things up and read energy flow from your network or connected machines.

## Power Port

The standard **Power Port** uses `int` buffer and transfer limits (Forge FE), suitable for most modpacks.

## High Conduction Power Port

The **High Conduction Power Port** is the upgraded variant for very large RF buffers and per-tick transfer beyond the standard port’s `int` limits.

- Uses **long** energy storage and extraction rates (tunable in Colossal Reactors **common** config).

- On each tick, pushes stored energy to neighbors in order: **OP** (Brandon’s Core / Draconic Evolution) when exposed, then **Flux Networks** long transfer when installed, then standard **Forge FE**.

- Craft from a **Power Port** plus high-tier materials; exact recipes depend on your modpack (**JEI**).

## Features (both variants)

- Place on an outer face of the casing; the **inside** must face the reactor interior.

- Connect compatible energy handlers from other mods on the **outside** of the port.
