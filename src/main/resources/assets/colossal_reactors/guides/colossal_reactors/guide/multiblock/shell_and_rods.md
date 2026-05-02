---
navigation:
  parent: multiblock/multiblock-index.md
  title: Shell, faces & rods
  icon: reactor_casing
  position: 10
item_ids:
  - colossal_reactors:reactor_casing
  - colossal_reactors:reactor_glass
  - colossal_reactors:reactor_rod
  - colossal_reactors:rod_controller
---

# Shell, faces & rods

## Building the shell

<Row gap="16" fullWidth={true}>
  <BlockImage id="reactor_casing" scale="4" />
  <BlockImage id="reactor_glass" scale="4" />
</Row>

- Use **Reactor Casing** for opaque walls and **Reactor Glass** for transparent faces where you want to see inside or emit light.

- The interior must form a **closed box** with valid faces only on the outside. Leave room for rods, fluids, interior components, and any casing ports before you seal the last blocks.

## Rod controller

<BlockImage id="rod_controller" scale="4" />

The **Rod Controller** sits on the **top shell layer** of the box where you want a **fuel rod column**. Every interior cell below it in that column must be **Reactor Rod** blocks—otherwise validation fails. Use it to declare rod placement and geometry for the multiblock (distinct from the **Reactor Controller** block on an outer face, which runs the GUI).

## Fuel rods

<BlockImage id="reactor_rod" scale="4" />

- **Reactor Rods** hold fuel items. Place them where your design allows; adjacency and layout change **RF output** and **fuel consumption** in the controller readouts.

- Insert and extract fuel through the **Reactor Controller** or automation that targets fuel slots (see the controller page).

## Heat sinks (passive)

**Heat sinks** are **passive** thermal components **inside** the reactor air volume. They can be **solid blocks** or **stationary liquid volumes** you place within the shell (counted as interior fill for layout rules—not the same as the active coolant loop).

They change **RF output** and **fuel consumption** based on how you fill interior cells next to rods. **Thermal / instability** readouts that depend on them appear **only** when the pack enables **reactor instability**; otherwise there is no separate “temperature” mechanic tied to heat sinks.

They are **not** consumed like the active coolant circuit.

## Coolant (active)

**Coolant (active)** is the **working fluid** in the circuit: it is **consumed** while the reactor runs. Some coolants (e.g. water in **steam** mode) **divert** would-be **RF** into **exhaust fluid**—with enough conversion, **RF can go to zero** while fuel still burns. Datapack **RF** and **consumption** multipliers apply on top; other coolants may only scale RF and fluid use without that diversion. If the circuit is **starved**, **RF** can also fall until supply returns.

**Thermal / instability** behaviour from the fluid circuit applies **only** when **reactor instability** is enabled; otherwise you see normal power and fluid levels without that layer.

Fill or drain through the **Reactor Controller** or compatible **fluid-capable ports**, depending on your build.
