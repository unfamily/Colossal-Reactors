---
navigation:
  parent: turbine/turbine-index.md
  title: Steam, RF, and coils
  icon: turbine_resource_port
  position: 12
categories:
  - multiblock
---

# Steam, RF, and coils

## Energy production

When the turbine is **valid** and **running**, each tick:

```text
RF/tick = steamConsumedMb × rfPerSteamMb × coilEfficiency × bladeEfficiency
```

- **steamConsumedMb** — steam taken from the [Turbine Resource Port](turbine_resource_port.md), clamped by blade steam capacity and availability.

- **rfPerSteamMb** — from the active **turbine generation** datapack entry (default fallback **7** RF per mB steam for `#c:steam` → water).

- **coilEfficiency** — from blocks in the **coil zone** (see below).

- **bladeEfficiency** — from layer-wise blade layout (see [Rods and blades](turbine_rod_and_blades.md)).

Global **production** and **consumption** multipliers in config scale RF output and steam use.

### Why 7 RF/mB?

Fission reactors convert fuel into steam at a lower effective RF density. Turbines are meant to be a **net gain** when you pipe reactor steam into a full blade + coil setup—tune `steamMbPerBladePerTick` in config for your target multiblock size (design goal: very large turbines near **~20M mB/t** steam at full blade count).

## Steam capacity (blades)

Maximum steam per tick scales with **valid blade count**:

```text
maxSteamMb/tick ≈ bladeCount × steamMbPerBladePerTick
```

`steamMbPerBladePerTick` is in config (balance section). More balanced rings on more rods raise the cap.

## Electrical coil zone

The **top interior layers** (default **3**, adjustable in the builder) are the **coil zone**. Only blocks listed in the **elec coils** datapack count.

For every matching block in that zone:

```text
coilEfficiency = min( average(eff_coe), average(eff_max) )
```

- **eff_coe** and **eff_max** come from each entry (e.g. gold, copper storage blocks, netherite).

- **Air** or unknown blocks use the configured empty-coil efficiency (default **0.3**).

- Invalid or unresolved entries are skipped (same idea as heat sink sanitization).

Use **storage blocks** (tags like `#c:storage_blocks/copper_all`, gold, electrum, etc.)—not heating coil blocks from the reactor.

**JEI** lists coil entries when the turbine JEI categories are installed.

## Turbine generation (steam type)

The **turbine generation** datapack defines which fluid counts as turbine steam and the **rf_production** per mB. The default entry accepts **`#c:steam`** and outputs water as exhaust.

Changing generation entries or config defaults lets pack makers support other fluids or RF rates without editing code.
