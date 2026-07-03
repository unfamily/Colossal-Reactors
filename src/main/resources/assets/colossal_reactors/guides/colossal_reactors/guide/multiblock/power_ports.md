---
navigation:
  parent: multiblock/multiblock-index.md
  title: Reactor Power Ports
  icon: power_port
  position: 36
item_ids:
  - colossal_reactors:power_port
  - colossal_reactors:high_cond_power_port
categories:
  - ports
---

# Reactor Power Ports

<Row gap="16" fullWidth={true}>
  <BlockImage id="power_port" scale="4" />
  <BlockImage id="high_cond_power_port" scale="4" />
</Row>

**Reactor power ports** are **face-only** attachments: they send energy from the reactor to cables, ducts, or machines on the **outside**. There is **no screen**—wire the outside face and read flow from your energy network.

## Reactor Power Port

The standard **Reactor Power Port** is enough for most modpacks and typical RF rates.

## Reactor High Conduction Power Port

The **Reactor High Conduction Power Port** is for very large reactors that produce enormous RF per tick. It stores and moves much higher amounts of energy and works best with mods that support huge power transfer (for example Draconic Evolution or Flux Networks when installed).

Craft it from a **Reactor Power Port** plus high-tier materials—check **JEI** for your pack.

## Automatic capacity scaling

When the reactor **re-validates** (controller reboot or structure check after you change the build), every **power port** on the multiblock resizes its RF buffer:

- Target size is about **estimated RF/t × 10** (safety margin), never below **10,000 RF**.
- **Reactor Power Port** caps at **2.1B RF** (2,147,483,647).
- **High Conduction Power Port** uses **64-bit** storage — practical maximum **9.22×10¹⁸ RF**.

Capacity **grows immediately** when the reactor needs more headroom. It **shrinks only** when what is already stored still fits in the smaller target, so energy is not lost to a resize.

Re-validate after major fuel, coolant, or heat-sink changes so port buffers match the new output.

## Features (both variants)

- Place on an outer face of the casing; the **inside** must face the reactor interior.

- Connect compatible energy handlers from other mods on the **outside** of the port.
