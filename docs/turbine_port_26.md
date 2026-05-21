# Turbine stack port 1.21.1 → 26.1.2

Source of truth: `Colossal-Reactors` post rotor-cache fix.

| File | Action |
|------|--------|
| `TurbineControllerBlockEntity` | Adapt: structureRevision, partial sync, rotorLoadFactor, quiet caches |
| `TurbineControllerBlock` | Copy/adapt: notify, containsBlock, refreshGate, animateTick |
| `TurbineRodBlock`, `TurbineBladeItem` | Copy hooks |
| `TurbineRedstonePortBlock`, `RedstonePortBlockEntity` | notify redstone |
| `client/turbine/TurbineRotorClientRegistry` | Copy from 1.21 |
| `TurbineRotorGeometry`, `Visibility`, `SimulationSource` | Copy from 1.21 |
| `TurbineRotorAnimationManager` | Replace with facade |
| `TurbineControllerBlockEntityRenderer` | Integrate registry |
| `TurbineRodBladeHidingBlockStateModel` | Delegate registry |
| `ClientConfig` | Port turbine section |
| `ColossalReactorsClientEvents` | clientTick registry |
| `TurbineRotorRenderHelper` | Align offsets if needed |

Discard on 26: monolithic AnimationManager validation/scan, global HIDDEN_STATIC, full `loadWithComponents` on rotor packets.

## QA matrix (26.1.2)

1. Two turbines ON spinning 10–30 blocks apart — edit rod on one → other stays visible
2. Two adjacent turbines same chunk
3. SP: rapid rod break/place; structure mirrors from server packet
4. Dedicated server: no client mirror; structure packet has bounds
5. VISUAL OFF on one turbine → hide only that turbine
6. Redstone gate closed → static blades visible, BER assembly idle
7. Chunk unload/reload controller → geometry rebuilds
8. Profiler: `onDataPacket` does not reload full fluid buffers each sim tick
