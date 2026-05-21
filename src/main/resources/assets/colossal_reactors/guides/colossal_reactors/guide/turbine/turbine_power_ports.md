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

Power ports send the turbine’s **RF** to cables and machines on the **outside**. There is **no GUI**—connect the outer face and read power on your network.

## Turbine Power Port

The normal **Turbine Power Port** is enough for most modpacks and typical RF rates.

## Turbine High Conduction Power Port

The **High Conduction** variant is for **very large** turbines that produce enormous RF per tick. It stores and moves much higher amounts of energy (tunable in mod settings) and works better with mods that support huge power transfer (for example Draconic Evolution or Flux Networks when installed).

Craft it from a **Turbine Power Port** plus high-tier parts—check **JEI**.

## Both variants

- Place on a **casing face** with the inside toward the turbine interior.

- Attach energy cables or machines on the **outside**.

Output depends on steam, coils, and blades—see [Steam, RF, and coils](turbine_generation_and_coils.md).
