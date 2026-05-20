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

Attach a **Turbine Resource Port** to the turbine casing to move **steam** (and compatible fluids) between the outside world and the turbine interior buffers.

## Features

- **Mode** cycles insert, extract, or eject behaviour toward the turbine (exact names on the button).

- **Filter** (when shown) limits accepted fluids—must match the active **turbine generation** entry (default: **`#c:steam`**).

- **Fluid column**: hover for amount and type. Use **D** under the tank to dump the port storage when needed.

- Connect fluid pipes or tanks on the **outside** face; the **inside** must face the turbine interior.

Pipe reactor steam output into this port to feed a running turbine. See [Steam, RF, and coils](turbine_generation_and_coils.md) for consumption limits.
