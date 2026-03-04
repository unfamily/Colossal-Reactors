package net.unfamily.colossal_reactors.docs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates README.md in the reactor directory with full documentation for reactor JSON scripts (coolant, fuel, heat sink).
 */
public final class ScriptsDocsGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptsDocsGenerator.class);

    private ScriptsDocsGenerator() {}

    /**
     * Writes README.md into the given reactor directory. Always overwrites. Creates the directory if it does not exist.
     */
    public static void generateReadme(Path reactorDir) throws IOException {
        Files.createDirectories(reactorDir);
        Path readme = reactorDir.resolve("README.md");
        String content = getReadmeContent(reactorDir);
        Files.writeString(readme, content);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Generated scripts documentation at {}", readme.toAbsolutePath());
        }
    }

    private static String getReadmeContent(Path reactorDir) {
        String reactorDirStr = reactorDir.toString().replace("\\", "/");
        return """
            # Colossal Reactors – Reactor scripts

            Place your reactor JSON configs in this folder. The mod loads all `.json` files from here.

            ## Default dump

            When you run `/colossal_reactors dump`, the mod writes default JSON files into this folder:
            - `default_coolant.json` – coolant definitions (e.g. water → steam)
            - `default_fuel.json` – fuel definitions (e.g. uranium)
            - `default_heat_sinks.json` – heat sink block/liquid multipliers

            These files contain the internal defaults. **Any change you make to these files (or any other JSON in this folder) overrides the internal defaults.** Use `/colossal_reactors reload` after editing to apply changes without restarting.

            ## Location

            - **This folder** (config: `dev.000_external_scripts_path` + `/reactor`): default `kubejs/external_scripts/colossal_reactors/reactor`
            - **Path**: `%s`
            - Any `.json` here (except names starting with `.`) is scanned. File names do not matter; the **type** field inside each file selects coolant, fuel, or heat sink.

            ## Reload

            After editing JSON files, run in-game:
            ```
            /colossal_reactors reload
            ```

            ## Coolant (liquid conversion)

            Files with `"type": "colossal_reactors:coolant"` define coolants. Used for water mode (reduce RF → steam) and heat sink fluid modifiers.

            ### Root keys

            | Key | Type | Description |
            |-----|------|-------------|
            | `type` | string | Must be `colossal_reactors:coolant` |
            | `entries` | array | List of coolant entries (see below). |

            ### Entry keys

            | Key | Type | Default | Description |
            |-----|------|---------|-------------|
            | `coolant_id` | string | required | Unique id, e.g. `colossal_reactors:water` |
            | `inputs` | array of string | required | Fluid ids or tags. Tag: `"#c:water"`. Fluid: `"minecraft:water"`. |
            | `output` | string | required | Output fluid for steam, usually a tag, e.g. `"#c:steam"` |
            | `reduce_rf_production` | boolean | false | If true, reactor converts RF to steam (consumes fluid from INSERT ports); no RF when fluid is sufficient. |
            | `rf_to_coolant_factor` | number | 0.45 | Coolant (mB) consumed per 1 RF when in water mode: `mb = rfProduced * rf_to_coolant_factor` |
            | `steam_per_coolant` | number | 1.0 | Steam (mB) produced per 1 mB coolant consumed. |
            | `rf_increment_percent` | number | 0 | RF multiplier = 1 + value/100 (e.g. 2 → 1.02). |
            | `mb_decrement_percent` | number | 100 | Consumption divisor (e.g. 100 → 1.0). |
            | `fluid_color` | string or number | - | ARGB color for GUI (e.g. `"#3498db"`). |
            | `output_color` | string or number | - | ARGB color for steam in GUI. |
            | `disable` | boolean | false | If true, this entry **excludes** the listed inputs from being valid (use to blacklist). |

            ### Example: water coolant

            ```json
            {
              "type": "colossal_reactors:coolant",
              "entries": [
                {
                  "coolant_id": "colossal_reactors:water",
                  "inputs": ["minecraft:water", "#c:water"],
                  "output": "#c:steam",
                  "reduce_rf_production": true,
                  "rf_to_coolant_factor": 0.45,
                  "steam_per_coolant": 1.0,
                  "fluid_color": "#3498db",
                  "output_color": "#e8f0f0"
                }
              ]
            }
            ```

            ### Example: second coolant (e.g. gel)

            Add another entry or file with a different `coolant_id` and inputs. Only one coolant is active at a time (from fluid in INSERT ports).

            ---

            ## Fuel

            Files with `"type": "colossal_reactors:fuel"` define fuel types (items accepted in INSERT ports and consumed in rods).

            ### Root keys

            | Key | Type | Description |
            |-----|------|-------------|
            | `type` | string | Must be `colossal_reactors:fuel` |
            | `entries` | array | List of fuel entries. |

            ### Entry keys

            | Key | Type | Default | Description |
            |-----|------|---------|-------------|
            | `fuel_id` | string | required | Unique id, e.g. `colossal_reactors:uranium` |
            | `inputs` | array of string | required | Item ids or tags, e.g. `"#c:ingots/uranium"`, `"colossal_reactors:uranium_ingot"` |
            | `output` | string | optional | Item id for solid waste produced when this fuel is consumed. |
            | `units_per_item` | number | 1000 | Fuel units one item gives (e.g. 1 ingot = 1000 units). |
            | `base_rf_per_tick` | number | from config | Reference RF (used in formulas). |
            | `base_fuel_units_per_tick` | number | from config | Reference consumption rate. |
            | `disable` | boolean | false | If true, listed inputs are **excluded** from being valid fuel. |

            ### Example: uranium

            ```json
            {
              "type": "colossal_reactors:fuel",
              "entries": [
                {
                  "fuel_id": "colossal_reactors:uranium",
                  "inputs": ["#c:ingots/uranium", "colossal_reactors:uranium_ingot"],
                  "output": "colossal_reactors:nuclear_waste",
                  "units_per_item": 1000
                }
              ]
            }
            ```

            ---

            ## Heat sink

            Files with `"type": "colossal_reactors:heat_sinks"` define which blocks/fluids in the reactor interior count as heat sinks and their fuel/energy multipliers.

            ### Root keys

            | Key | Type | Description |
            |-----|------|-------------|
            | `type` | string | Must be `colossal_reactors:heat_sinks` |
            | `entries` | array | List of heat sink entries. |

            ### Entry keys

            | Key | Type | Description |
            |-----|------|-------------|
            | `valid_blocks` | array of string | Block ids or tags (e.g. `"#c:storage_blocks/diamond"`). Empty = not used. |
            | `valid_liquids` | array of string | Fluid ids or tags (e.g. `"#c:water"`). Empty = not used. |
            | `fuel` | number | Fuel consumption multiplier for cells matching this entry. |
            | `energy` | number | RF/energy multiplier for cells matching this entry. |
            | `must_source` | boolean | Default true for liquids: if true, only fluid **sources** match (not flowing). |

            Blocks/liquids are matched by tag or id. Rod cells use the coolant fluid from INSERT ports for their modifier (when liquids use ports).

            ### Example

            ```json
            {
              "type": "colossal_reactors:heat_sinks",
              "entries": [
                {
                  "valid_liquids": ["#c:water"],
                  "must_source": true,
                  "fuel": 1.05,
                  "energy": 1.15
                },
                {
                  "valid_blocks": ["#c:storage_blocks/diamond"],
                  "fuel": 1.8,
                  "energy": 1.6
                }
              ]
            }
            ```

            ---

            *Documentation auto-generated by Colossal Reactors on every startup.*
            """.formatted(reactorDirStr);
    }
}
