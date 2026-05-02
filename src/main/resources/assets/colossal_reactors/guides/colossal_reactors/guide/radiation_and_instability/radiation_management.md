---
navigation:
  parent: radiation_and_instability/risk-index.md
  title: Radiation management
  icon: radiation_scrubber
  position: 10
item_ids:
  - colossal_reactors:radiation_scrubber
  - colossal_reactors:radiation_cure
categories:
  - processing
---

# Radiation management

Present when **radiation management** is enabled in config and your instance includes the integrated content (typically **Mekanism**). Use **JEI** to confirm recipes and blocks exist for your save.

## Radiation Scrubber

<BlockImage id="radiation_scrubber" scale="4" />

The **Radiation Scrubber** spends energy and optional **catalyst** items to process airborne radiation-related substances into a **chemical/gas buffer** shown in its tank.

### Controls

- Two **item slots** for catalyst configuration (see JEI and your datapack for valid catalysts).

- **Gas/chemical tank**: hover for contents and fill level.

- **Energy bar** on the side: hover for stored energy.

When **Mekanism** is present, automation may interact through Mek’s chemical APIs as exposed by the block; otherwise interact manually through the screen and attached pipes where supported.

## Radiation Cure

<ItemImage id="radiation_cure" scale="2" />

**Radiation Cure** is a consumable used with that radiation workflow. Open from the item tooltip with GuideME’s **open guide** shortcut when configured.
