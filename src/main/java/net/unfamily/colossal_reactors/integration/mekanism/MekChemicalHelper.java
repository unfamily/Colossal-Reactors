package net.unfamily.colossal_reactors.integration.mekanism;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reflection bridge to Mekanism chemicals when the mod is loaded (optional dependency).
 * <p>
 * On NeoForge 26.x gas/chemical runtime is {@linkplain #GAS_SUPPORT_ENABLED locked off} until
 * official Mekanism (or a stable long-term port) is acceptable to ship against. Bridge code below
 * is already adapted for Mekanism Version Locked APIs (ChemicalStack int amounts, IChemicalTank
 * wrap, ResourceHandler radiation dump) so flipping the flag is enough to re-enable later.
 * {@link ModList#isLoaded(String)} with {@code "mekanism"} remains the load check.
 */
public final class MekChemicalHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MekChemicalHelper.class);

    /**
     * Runtime lock: {@code false} on 26.x — chemical fuel/coolant/ports/JEI stay inactive even if
     * Mekanism Version Locked is installed. Set {@code true} when ready to support Mek gas again.
     */
    public static final boolean GAS_SUPPORT_ENABLED = true;

    private MekChemicalHelper() {}

    public static boolean isGasSupportEnabled() {
        return GAS_SUPPORT_ENABLED;
    }

    public static boolean isLoaded() {
        return GAS_SUPPORT_ENABLED && ModList.get().isLoaded("mekanism");
    }

    @Nullable
    private static Object chemicalRegistry() {
        if (!isLoaded()) return null;
        try {
            Class<?> api = Class.forName("mekanism.api.MekanismAPI");
            return api.getField("CHEMICAL_REGISTRY").get(null);
        } catch (Throwable t) {
            LOGGER.warn("Could not access Mekanism CHEMICAL_REGISTRY: {}", t.getMessage());
            return null;
        }
    }

    @Nullable
    private static ResourceKey<?> chemicalRegistryNameKey() {
        try {
            Class<?> api = Class.forName("mekanism.api.MekanismAPI");
            Object key = api.getField("CHEMICAL_REGISTRY_NAME").get(null);
            if (key instanceof ResourceKey<?> rk) {
                return rk;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean chemicalTagExists(Identifier tagId) {
        Object registry = chemicalRegistry();
        if (registry == null) return false;
        try {
            ResourceKey<?> registryName = chemicalRegistryNameKey();
            if (registryName == null) return false;
            Class<?> tagKeyClass = Class.forName("net.minecraft.tags.TagKey");
            Object tagKey = tagKeyClass.getMethod("create", ResourceKey.class, Identifier.class)
                    .invoke(null, registryName, tagId);
            Object opt = registry.getClass().getMethod("getTag", tagKeyClass).invoke(registry, tagKey);
            if (opt instanceof Optional<?> optional && optional.isPresent()) {
                Object holders = optional.get();
                return !Boolean.TRUE.equals(holders.getClass().getMethod("isEmpty").invoke(holders));
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Chemical tank that accepts any Mek chemical, including radioactive gases (fissile fuel, nuclear waste).
     * {@link mekanism.api.chemical.BasicChemicalTank#create(long, mekanism.api.IContentsListener)} uses
     * {@link mekanism.api.chemical.attribute.ChemicalAttributeValidator#DEFAULT}, which rejects them.
     */
    @Nullable
    public static Object createBasicTank(long capacityMb) {
        return createAllValidTank(capacityMb);
    }

    /** Same as {@link #createBasicTank(long)} — explicit name for ports and general storage. */
    @Nullable
    public static Object createAllValidTank(long capacityMb) {
        if (!isLoaded()) return null;
        try {
            Class<?> tankClass = Class.forName("mekanism.api.chemical.BasicChemicalTank");
            Class<?> listenerClass = Class.forName("mekanism.api.IContentsListener");
            Method create = tankClass.getMethod("createAllValid", long.class, listenerClass);
            return create.invoke(null, capacityMb, null);
        } catch (Throwable t) {
            LOGGER.warn("Could not create Mek all-valid chemical tank (capacity={}): {}", capacityMb, t.toString());
            return null;
        }
    }

    /**
     * Tank validator matching Mek's {@code Radioactive Waste Barrel} ({@code StackedWasteBarrel#ATTRIBUTE_VALIDATOR}):
     * only radioactive chemicals.
     */
    @Nullable
    public static Object createRadioactiveOnlyTank(long capacityMb) {
        if (!isLoaded()) return null;
        try {
            Class<?> tankClass = Class.forName("mekanism.api.chemical.BasicChemicalTank");
            Class<?> listenerClass = Class.forName("mekanism.api.IContentsListener");
            Class<?> validatorInterface = Class.forName("mekanism.api.chemical.attribute.ChemicalAttributeValidator");
            Object validator = loadRadioactiveWasteBarrelValidator();
            if (validator == null) {
                return createAllValidTank(capacityMb);
            }
            Method create = tankClass.getMethod("createWithValidator", long.class, validatorInterface, listenerClass);
            return create.invoke(null, capacityMb, validator, null);
        } catch (Throwable t) {
            LOGGER.debug("Could not create Mek radioactive chemical tank: {}", t.getMessage());
            return createAllValidTank(capacityMb);
        }
    }

    @Nullable
    private static Object loadRadioactiveWasteBarrelValidator() {
        try {
            Class<?> barrelClass = Class.forName("mekanism.common.capabilities.chemical.StackedWasteBarrel");
            var field = barrelClass.getDeclaredField("ATTRIBUTE_VALIDATOR");
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable t) {
            LOGGER.debug("Could not load Mek radioactive waste barrel validator: {}", t.getMessage());
            return null;
        }
    }

    /** True if stack holds a Mek chemical with {@code Chemical#isRadioactive()}. */
    public static boolean isRadioactiveStack(@Nullable Object chemicalStack) {
        if (chemicalStack == null) return false;
        try {
            return Boolean.TRUE.equals(chemicalStack.getClass().getMethod("isRadioactive").invoke(chemicalStack));
        } catch (Throwable e) {
            return false;
        }
    }

    @Nullable
    public static Object getChemicalInTank(@Nullable Object handler, int tank) {
        if (handler == null) {
            return null;
        }
        try {
            return handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, tank);
        } catch (Throwable ignored) {
        }
        // Mek VL BasicChemicalTank is IChemicalTank (resource container), not IChemicalHandler.
        if (tank != 0) {
            return null;
        }
        return stackFromChemicalTank(handler);
    }

    @Nullable
    private static Object stackFromChemicalTank(@Nullable Object tank) {
        if (tank == null) {
            return null;
        }
        try {
            Object asStack = tank.getClass().getMethod("asStack").invoke(tank);
            Object resource = asStack.getClass().getMethod("resource").invoke(asStack);
            long amt = ((Number) asStack.getClass().getMethod("amount").invoke(asStack)).longValue();
            if (resource == null || amt <= 0) {
                Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
                return stackClass.getField("EMPTY").get(null);
            }
            int capped = (int) Math.min(amt, Integer.MAX_VALUE);
            return resource.getClass().getMethod("toStack", int.class).invoke(resource, capped);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isRadioactiveInTank(@Nullable Object handler) {
        return isRadioactiveStack(getChemicalInTank(handler, 0));
    }

    /** Client/server helper when only the synced registry name is available (e.g. port GUI). */
    public static boolean isRadioactiveGasId(@Nullable String registryName) {
        if (!isLoaded() || registryName == null || registryName.isBlank()) {
            return false;
        }
        Identifier id = Identifier.tryParse(registryName);
        if (id == null) {
            return false;
        }
        Object stack = createStack(id, 1);
        return isRadioactiveStack(stack);
    }

    public static boolean isEmpty(@Nullable Object chemicalStack) {
        if (chemicalStack == null) return true;
        try {
            return Boolean.TRUE.equals(chemicalStack.getClass().getMethod("isEmpty").invoke(chemicalStack));
        } catch (Throwable e) {
            try {
                long amount = ((Number) chemicalStack.getClass().getMethod("getAmount").invoke(chemicalStack)).longValue();
                return amount <= 0;
            } catch (Throwable e2) {
                return true;
            }
        }
    }

    public static long getAmount(@Nullable Object chemicalStack) {
        if (chemicalStack == null) return 0;
        try {
            return ((Number) chemicalStack.getClass().getMethod("amount").invoke(chemicalStack)).longValue();
        } catch (Throwable ignored) {
        }
        try {
            return ((Number) chemicalStack.getClass().getMethod("getAmount").invoke(chemicalStack)).longValue();
        } catch (Throwable e) {
            return 0;
        }
    }

    @Nullable
    public static String getTypeRegistryName(@Nullable Object chemicalStack) {
        if (chemicalStack == null || isEmpty(chemicalStack)) return null;
        try {
            Object rl = chemicalStack.getClass().getMethod("getTypeRegistryName").invoke(chemicalStack);
            return rl != null ? rl.toString() : null;
        } catch (Throwable ignored) {
        }
        try {
            Object holder = chemicalStack.getClass().getMethod("typeHolder").invoke(chemicalStack);
            if (holder != null) {
                Object name = holder.getClass().getMethod("getRegisteredName").invoke(holder);
                return name != null ? name.toString() : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static int getTankAmount(Object handler) {
        try {
            Object stack = handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, 0);
            return (int) Math.min(getAmount(stack), Integer.MAX_VALUE);
        } catch (Throwable e) {
            return 0;
        }
    }

    public static int getTankCapacity(Object handler) {
        return (int) Math.min(getTankCapacityLong(handler), Integer.MAX_VALUE);
    }

    public static long getTankAmountLong(Object handler) {
        return Math.max(0L, getAmount(getChemicalInTank(handler, 0)));
    }

    public static long getTankCapacityLong(Object handler) {
        if (handler == null) return 0L;
        try {
            return Math.max(0L, ((Number) handler.getClass().getMethod("getChemicalTankCapacity", int.class)
                    .invoke(handler, 0)).longValue());
        } catch (Throwable ignored) {
        }
        try {
            Object stack = stackFromChemicalTank(handler);
            Class<?> chemRes = Class.forName("mekanism.api.chemical.ChemicalResource");
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object resource = chemRes.getMethod("of", stackClass).invoke(null, stack);
            return Math.max(0L, ((Number) handler.getClass().getMethod("capacityAsLong", chemRes)
                    .invoke(handler, resource)).longValue());
        } catch (Throwable e) {
            return 0L;
        }
    }

    /**
     * Drains up to {@code amount} mB from a chemical handler tank, optionally filtered by chemical type.
     * Tries Mek 10.7+ {@code extractChemical(ChemicalStack, Action)} first, then legacy signatures.
     */
    public static int extractFromTank(Object handler, int tank, long amount, @Nullable Object typeFilter) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        try {
            Object inTank = handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, tank);
            if (isEmpty(inTank)) {
                return 0;
            }
            if (typeFilter != null && !isEmpty(typeFilter)) {
                String filterName = getTypeRegistryName(typeFilter);
                if (filterName != null && !matchesSelector(inTank, "%" + filterName)) {
                    String inName = getTypeRegistryName(inTank);
                    if (inName == null || !inName.equals(filterName)) {
                        return 0;
                    }
                }
            }
            long drain = Math.min(amount, getAmount(inTank));
            if (drain <= 0) {
                return 0;
            }
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object exec = actionClass.getField("EXECUTE").get(null);
            Object toExtract = copyStack(inTank, drain);
            Object drained = null;
            if (toExtract != null) {
                Class<?> stackClass = toExtract.getClass();
                try {
                    drained = handler.getClass().getMethod("extractChemical", stackClass, actionClass)
                            .invoke(handler, toExtract, exec);
                } catch (NoSuchMethodException e) {
                    try {
                        drained = handler.getClass().getMethod("extractChemical", int.class, stackClass, actionClass)
                                .invoke(handler, tank, toExtract, exec);
                    } catch (NoSuchMethodException e2) {
                        drained = handler.getClass().getMethod("extractChemical", int.class, long.class, actionClass)
                                .invoke(handler, tank, drain, exec);
                    }
                }
            } else {
                drained = handler.getClass().getMethod("extractChemical", int.class, long.class, actionClass)
                        .invoke(handler, tank, drain, exec);
            }
            return (int) Math.min(getAmount(drained), Integer.MAX_VALUE);
        } catch (Throwable t) {
            LOGGER.debug("extractFromTank failed: {}", t.getMessage());
            return 0;
        }
    }

    public static int fill(Object handler, Object stack, boolean simulate) {
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object action = actionClass.getField(simulate ? "SIMULATE" : "EXECUTE").get(null);
            Class<?> stackClass = stack.getClass();
            Object result;
            try {
                result = handler.getClass().getMethod("insertChemical", stackClass, actionClass)
                        .invoke(handler, stack, action);
            } catch (NoSuchMethodException e) {
                result = handler.getClass().getMethod("insertChemical", int.class, stackClass, actionClass)
                        .invoke(handler, 0, stack, action);
            }
            if (result == null) return 0;
            long remaining = getAmount(result);
            long wanted = getAmount(stack);
            return (int) Math.min(wanted - remaining, Integer.MAX_VALUE);
        } catch (Throwable e) {
            LOGGER.warn("MekChemicalHelper.fill failed (handler={}, stack={}, simulate={}): {}",
                    handler == null ? "null" : handler.getClass().getSimpleName(),
                    getTypeRegistryName(stack),
                    simulate, e.toString());
            return 0;
        }
    }

    /** Mek 10.7+: insertChemical(ChemicalStack, Action); older builds may use (int, ChemicalStack, Action). */
    @Nullable
    public static Object findChemicalStackInArgs(@Nullable Object[] args) {
        if (args == null) return null;
        try {
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            for (Object arg : args) {
                if (arg != null && stackClass.isInstance(arg)) return arg;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Return value for a rejected insertChemical call (unchanged stack). */
    public static Object rejectedInsertReturn(@Nullable Object[] args) {
        Object stack = findChemicalStackInArgs(args);
        if (stack != null) return stack;
        try {
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            return stackClass.getField("EMPTY").get(null);
        } catch (Throwable e) {
            return null;
        }
    }

    @Nullable
    public static Object copyStack(@Nullable Object stack, long amount) {
        if (stack == null) return null;
        int capped = (int) Math.max(0L, Math.min(amount, Integer.MAX_VALUE));
        try {
            return stack.getClass().getMethod("copyWithAmount", int.class).invoke(stack, capped);
        } catch (Throwable ignored) {
        }
        try {
            Object copy = stack.getClass().getMethod("copy").invoke(stack);
            try {
                copy.getClass().getMethod("setAmount", int.class).invoke(copy, capped);
            } catch (NoSuchMethodException e) {
                copy.getClass().getMethod("setAmount", long.class).invoke(copy, amount);
            }
            return copy;
        } catch (Throwable e) {
            return null;
        }
    }

    /** Display amount for JEI / GUI previews (1 bucket in mB). */
    public static final long JEI_DISPLAY_AMOUNT_MB = 1000L;

    @Nullable
    public static Object createStack(Identifier chemicalId, long amount) {
        if (!isLoaded() || amount <= 0) return null;
        Object registry = chemicalRegistry();
        if (registry == null) return null;
        try {
            Object holder = resolveHolder(registry, chemicalId);
            if (holder == null) return null;
            return newStackFromHolder(holder, amount);
        } catch (Throwable t) {
            LOGGER.debug("createStack failed for {}: {}", chemicalId, t.getMessage());
            return null;
        }
    }

    /** Builds a stack from a datapack selector ({@code %namespace:id} or tag). */
    @Nullable
    public static Object createStackFromSelector(@Nullable String selector, long amount) {
        if (!isLoaded() || selector == null || selector.isBlank() || amount <= 0) {
            return null;
        }
        if (MaterialSelector.isChemicalPrefix(selector)) {
            Identifier id = Identifier.tryParse(selector.substring(1));
            if (id != null) {
                Object stack = createStack(id, amount);
                if (stack != null) {
                    return stack;
                }
            }
            List<Object> fromTag = stacksForSelector(selector);
            if (!fromTag.isEmpty()) {
                return copyStack(fromTag.get(0), amount);
            }
            return null;
        }
        Identifier id = Identifier.tryParse(selector);
        return id != null ? createStack(id, amount) : null;
    }

    public static boolean chemicalsMatch(@Nullable Object a, @Nullable Object b) {
        if (isEmpty(a) || isEmpty(b)) {
            return false;
        }
        String na = getTypeRegistryName(a);
        String nb = getTypeRegistryName(b);
        return na != null && na.equals(nb);
    }

    /**
     * Resolves a {@code %namespace:id} selector to one or more chemical stacks (id or Mek tag).
     */
    public static List<Object> stacksForSelector(String selector) {
        if (!isLoaded() || selector == null || !selector.startsWith("%")) {
            return List.of();
        }
        Identifier id = Identifier.tryParse(selector.substring(1));
        if (id == null) return List.of();
        Object single = createStack(id, JEI_DISPLAY_AMOUNT_MB);
        if (single != null && !isEmpty(single)) {
            return List.of(single);
        }
        return stacksInChemicalTag(id, JEI_DISPLAY_AMOUNT_MB);
    }

    private static List<Object> stacksInChemicalTag(Identifier tagId, long amountMb) {
        List<Object> out = new ArrayList<>();
        Object registry = chemicalRegistry();
        if (registry == null) return out;
        try {
            ResourceKey<?> registryName = chemicalRegistryNameKey();
            if (registryName == null) return out;
            Class<?> tagKeyClass = Class.forName("net.minecraft.tags.TagKey");
            Object tagKey = tagKeyClass.getMethod("create", ResourceKey.class, Identifier.class)
                    .invoke(null, registryName, tagId);
            Object opt = registry.getClass().getMethod("getTag", tagKeyClass).invoke(registry, tagKey);
            if (!(opt instanceof Optional<?> optional) || optional.isEmpty()) {
                return out;
            }
            Object holders = optional.get();
            for (Object holder : (Iterable<?>) holders) {
                Object stack = newStackFromHolder(holder, amountMb);
                if (stack != null && !isEmpty(stack)) {
                    out.add(stack);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    @Nullable
    private static Object newStackFromHolder(Object holder, long amount) {
        if (holder == null || amount <= 0) return null;
        int capped = (int) Math.min(amount, Integer.MAX_VALUE);
        try {
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Class<?> holderClass = Class.forName("net.minecraft.core.Holder");
            // Mek VL / 10.8+: ChemicalStack(Holder, int). Older builds used long.
            try {
                return stackClass.getConstructor(holderClass, int.class).newInstance(holder, capped);
            } catch (NoSuchMethodException e) {
                return stackClass.getConstructor(holderClass, long.class).newInstance(holder, amount);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object resolveHolder(Object registry, Identifier id) {
        try {
            ResourceKey<?> registryName = chemicalRegistryNameKey();
            if (registryName != null) {
                Class<?> resourceKeyClass = Class.forName("net.minecraft.resources.ResourceKey");
                Object key = resourceKeyClass.getMethod("create", ResourceKey.class, Identifier.class)
                        .invoke(null, registryName, id);
                Object opt = registry.getClass().getMethod("getHolder", resourceKeyClass).invoke(registry, key);
                if (opt instanceof Optional<?> optional && optional.isPresent()) {
                    return optional.get();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object chemical = null;
            try {
                chemical = registry.getClass().getMethod("getValue", Identifier.class).invoke(registry, id);
            } catch (NoSuchMethodException e) {
                chemical = registry.getClass().getMethod("get", Identifier.class).invoke(registry, id);
            }
            if (chemical == null) return null;
            return registry.getClass().getMethod("wrapAsHolder", chemical.getClass()).invoke(registry, chemical);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean matchesSelector(@Nullable Object chemicalStack, String selector) {
        if (chemicalStack == null || isEmpty(chemicalStack) || selector == null) return false;
        String name = getTypeRegistryName(chemicalStack);
        if (name == null) return false;
        if (selector.startsWith("%")) {
            String rest = selector.substring(1);
            Identifier id = Identifier.tryParse(rest);
            if (id == null) return false;
            if (name.equals(id.toString())) return true;
            return matchesChemicalTag(chemicalStack, id);
        }
        Identifier id = Identifier.tryParse(selector);
        return id != null && name.equals(id.toString());
    }

    private static boolean matchesChemicalTag(Object chemicalStack, Identifier tagId) {
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical == null) return false;
            Object tags = chemical.getClass().getMethod("tags").invoke(chemical);
            if (tags instanceof Iterable<?> iterable) {
                for (Object tagKey : iterable) {
                    Object loc = tagKey.getClass().getMethod("location").invoke(tagKey);
                    if (tagId.equals(loc)) return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** True when Mekanism global radiation is enabled (same gate as {@code IRadiationManager#dumpRadiation}). */
    public static boolean isMekRadiationEnabled() {
        if (!isLoaded()) {
            return false;
        }
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            return Boolean.TRUE.equals(managerClass.getMethod("isRadiationEnabled").invoke(manager));
        } catch (Throwable t) {
            LOGGER.debug("Could not query Mek radiation enabled: {}", t.getMessage());
            return false;
        }
    }

    /**
     * Releases radioactive gas into the world at {@code pos}, like Mek {@code TileEntityMekanism#blockRemoved}
     * and the Radioactive Waste Barrel. When {@code clearRadioactive} is true, emptied tanks are cleared.
     * <p>
     * Mek VL 10.8 uses {@code dumpRadiation(Level, BlockPos, ResourceHandler, TransactionContext, clearer)}.
     */
    public static void dumpRadiationFromHandler(Level level, BlockPos pos, @Nullable Object chemicalHandler,
                                                boolean clearRadioactive) {
        if (level == null || level.isClientSide() || chemicalHandler == null || !isLoaded()) {
            return;
        }
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            Class<?> resourceHandlerClass = Class.forName("net.neoforged.neoforge.transfer.ResourceHandler");
            Object handlerForDump = chemicalHandler;
            if (!resourceHandlerClass.isInstance(handlerForDump)) {
                handlerForDump = wrapAsHandler(chemicalHandler);
            }
            if (handlerForDump != null && resourceHandlerClass.isInstance(handlerForDump)) {
                Class<?> clearerClass = Class.forName(
                        "mekanism.api.radiation.IRadiationManager$HandlerRadiationClearer");
                Object clearer = Proxy.newProxyInstance(
                        clearerClass.getClassLoader(),
                        new Class<?>[]{clearerClass},
                        (proxy, method, args) -> {
                            if ("clear".equals(method.getName()) && clearRadioactive && args != null && args.length >= 2) {
                                Object h = args[0];
                                int index = args[1] instanceof Number n ? n.intValue() : 0;
                                clearHandlerSlot(h, index);
                            }
                            return null;
                        });
                Class<?> txClass = Class.forName("net.neoforged.neoforge.transfer.transaction.TransactionContext");
                managerClass.getMethod("dumpRadiation", Level.class, BlockPos.class, resourceHandlerClass,
                                txClass, clearerClass)
                        .invoke(manager, level, pos, handlerForDump, null, clearer);
                return;
            }
        } catch (Throwable t) {
            LOGGER.debug("Mek VL ResourceHandler dumpRadiation failed: {}", t.toString());
        }
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            Class<?> tankClass = Class.forName("mekanism.api.chemical.IChemicalTank");
            Object tank = chemicalHandler;
            if (!tankClass.isInstance(tank)) {
                // unwrap proxy / use raw createAllValid tank when possible
                tank = null;
            }
            if (tank != null) {
                Class<?> txClass = Class.forName("net.neoforged.neoforge.transfer.transaction.TransactionContext");
                managerClass.getMethod("dumpRadiation", Level.class, BlockPos.class, List.class, boolean.class, txClass)
                        .invoke(manager, level, pos, List.of(tank), clearRadioactive, null);
                return;
            }
        } catch (Throwable t) {
            LOGGER.debug("Mek VL IChemicalTank list dumpRadiation failed: {}", t.toString());
        }
        try {
            // Legacy signature (pre-VL)
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Class<?> handlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            Object wrapped = wrapAsHandler(chemicalHandler);
            if (wrapped == null || !handlerClass.isInstance(wrapped)) {
                return;
            }
            Object manager = managerClass.getField("INSTANCE").get(null);
            managerClass.getMethod("dumpRadiation", Level.class, BlockPos.class, handlerClass, boolean.class)
                    .invoke(manager, level, pos, wrapped, clearRadioactive);
        } catch (Throwable t) {
            LOGGER.debug("Could not dump Mek radiation from chemical handler: {}", t.getMessage());
        }
    }

    private static void clearHandlerSlot(@Nullable Object handler, int index) {
        if (handler == null) return;
        try {
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object empty = stackClass.getField("EMPTY").get(null);
            handler.getClass().getMethod("setChemicalInTank", int.class, stackClass).invoke(handler, index, empty);
            return;
        } catch (Throwable ignored) {
        }
        try {
            dumpTank(handler);
        } catch (Throwable ignored) {
        }
    }

    public static boolean dumpTank(Object handler) {
        try {
            Class<?> emptyClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Object empty = emptyClass.getField("EMPTY").get(null);
            handler.getClass().getMethod("setChemicalInTank", int.class, emptyClass).invoke(handler, 0, empty);
            return true;
        } catch (Throwable e) {
            try {
                setChemicalTankEmpty(handler);
                return true;
            } catch (Throwable e2) {
                return false;
            }
        }
    }

    private static void setChemicalTankEmpty(Object tank) throws Exception {
        Class<?> chemResClass = Class.forName("mekanism.api.chemical.ChemicalResource");
        Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
        Object emptyStack = stackClass.getField("EMPTY").get(null);
        Object emptyRes = chemResClass.getMethod("of", stackClass).invoke(null, emptyStack);
        Class<?> txClass = Class.forName("net.neoforged.neoforge.transfer.transaction.TransactionContext");
        tank.getClass().getMethod("setContents", chemResClass, long.class, txClass)
                .invoke(tank, emptyRes, 0L, null);
    }

    /**
     * Ensures an {@code IChemicalHandler} for capability/GUI use. Mek VL {@code createAllValid} returns
     * {@code IChemicalTank} (resource container); wrap it so legacy CR call sites keep working.
     */
    @Nullable
    public static Object wrapAsHandler(Object innerTank) {
        if (innerTank == null) return null;
        try {
            Class<?> handlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            if (handlerClass.isInstance(innerTank)) {
                return innerTank;
            }
            return Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    chemicalTankAsHandler(innerTank));
        } catch (Throwable e) {
            LOGGER.debug("wrapAsHandler failed: {}", e.toString());
            return innerTank;
        }
    }

    private static InvocationHandler chemicalTankAsHandler(Object tank) {
        return (proxy, method, args) -> {
            String name = method.getName();
            if ("getChemicalTanks".equals(name) || "size".equals(name)) {
                return 1;
            }
            if ("getChemicalInTank".equals(name)) {
                return stackFromChemicalTank(tank);
            }
            if ("setChemicalInTank".equals(name)) {
                Object stack = args != null && args.length > 1 ? args[1] : null;
                setTankFromChemicalStack(tank, stack);
                return null;
            }
            if ("getChemicalTankCapacity".equals(name)) {
                return getTankCapacityLong(tank);
            }
            if ("isValid".equals(name) && args != null && args.length >= 2) {
                Object second = args[1];
                if (second != null && second.getClass().getName().contains("ChemicalStack")) {
                    return Boolean.TRUE; // createAllValid already allows all attributes
                }
            }
            if ("insertChemical".equals(name)) {
                return insertIntoTank(tank, args);
            }
            if ("extractChemical".equals(name)) {
                return extractFromTankProxy(tank, args);
            }
            if ("getResource".equals(name)) {
                Object stack = stackFromChemicalTank(tank);
                Class<?> chemRes = Class.forName("mekanism.api.chemical.ChemicalResource");
                Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
                return chemRes.getMethod("of", stackClass).invoke(null, stack);
            }
            if ("getAmountAsLong".equals(name)) {
                return getAmount(stackFromChemicalTank(tank));
            }
            if ("getCapacityAsLong".equals(name)) {
                return getTankCapacityLong(tank);
            }
            if ("insert".equals(name) || "extract".equals(name)) {
                // ResourceHandler transaction API — delegate via IChemicalHandler defaults when possible
                if (method.isDefault()) {
                    return InvocationHandler.invokeDefault(proxy, method, args);
                }
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "CR-MekChemicalHandlerProxy(" + tank.getClass().getSimpleName() + ")";
            }
            throw new UnsupportedOperationException("Unsupported Mek chemical handler method: " + name);
        };
    }

    private static void setTankFromChemicalStack(Object tank, @Nullable Object stack) throws Exception {
        Class<?> chemResClass = Class.forName("mekanism.api.chemical.ChemicalResource");
        Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
        if (stack == null || isEmpty(stack)) {
            setChemicalTankEmpty(tank);
            return;
        }
        Object resource = chemResClass.getMethod("of", stackClass).invoke(null, stack);
        long amt = getAmount(stack);
        Class<?> txClass = Class.forName("net.neoforged.neoforge.transfer.transaction.TransactionContext");
        tank.getClass().getMethod("setContents", chemResClass, long.class, txClass)
                .invoke(tank, resource, amt, null);
    }

    @Nullable
    private static Object insertIntoTank(Object tank, @Nullable Object[] args) throws Exception {
        Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
        Object stack = findChemicalStackInArgs(args);
        if (stack == null || isEmpty(stack)) {
            return stackClass.getField("EMPTY").get(null);
        }
        boolean simulate = isSimulateAction(args);
        Class<?> chemResClass = Class.forName("mekanism.api.chemical.ChemicalResource");
        Object resource = chemResClass.getMethod("of", stackClass).invoke(null, stack);
        int want = (int) Math.min(getAmount(stack), Integer.MAX_VALUE);
        Class<?> txClass = Class.forName("net.neoforged.neoforge.transfer.transaction.TransactionContext");
        // Prefer ResourceHandler.insert on tank if present
        try {
            Method insert = tank.getClass().getMethod("insert", int.class, chemResClass, int.class, txClass);
            if (simulate) {
                // no open tx — use IResourceContainer insert helpers when available
            }
        } catch (NoSuchMethodException ignored) {
        }
        Object current = stackFromChemicalTank(tank);
        long capacity = getTankCapacityLong(tank);
        long have = getAmount(current);
        long space = Math.max(0L, capacity - have);
        if (!isEmpty(current) && !chemicalsMatch(current, stack)) {
            return stack; // reject — remaining = full input
        }
        int moved = (int) Math.min(want, space);
        if (!simulate && moved > 0) {
            Object merged = isEmpty(current) ? copyStack(stack, moved) : copyStack(current, have + moved);
            setTankFromChemicalStack(tank, merged);
        }
        return copyStack(stack, want - moved);
    }

    @Nullable
    private static Object extractFromTankProxy(Object tank, @Nullable Object[] args) throws Exception {
        Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
        Object empty = stackClass.getField("EMPTY").get(null);
        Object inTank = stackFromChemicalTank(tank);
        if (isEmpty(inTank)) {
            return empty;
        }
        long want = Long.MAX_VALUE;
        Object filter = findChemicalStackInArgs(args);
        if (filter != null && !isEmpty(filter) && !chemicalsMatch(filter, inTank)) {
            return empty;
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Number n && !(arg instanceof Boolean)) {
                    long v = n.longValue();
                    if (v > 0 && v < want) {
                        want = v;
                    }
                }
            }
        }
        boolean simulate = isSimulateAction(args);
        long have = getAmount(inTank);
        int moved = (int) Math.min(want, have);
        if (moved <= 0) {
            return empty;
        }
        Object out = copyStack(inTank, moved);
        if (!simulate) {
            setTankFromChemicalStack(tank, copyStack(inTank, have - moved));
        }
        return out;
    }

    private static boolean isSimulateAction(@Nullable Object[] args) {
        if (args == null) return false;
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object simulate = actionClass.getField("SIMULATE").get(null);
            for (Object arg : args) {
                if (arg != null && actionClass.isInstance(arg) && arg.equals(simulate)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
