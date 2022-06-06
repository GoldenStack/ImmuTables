package dev.goldenstack.loot.function;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.goldenstack.loot.ImmuTables;
import dev.goldenstack.loot.condition.LootCondition;
import dev.goldenstack.loot.context.LootContext;
import dev.goldenstack.loot.json.JsonHelper;
import dev.goldenstack.loot.json.JsonLootConverter;
import dev.goldenstack.loot.provider.number.NumberProvider;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@code LootFunction} that adds enchantments to the ItemStack that is provided.
 */
public class SetEnchantmentsFunction extends ConditionalLootFunction {

    private final @NotNull Map<Enchantment, NumberProvider> enchantments;
    private final boolean add;

    /**
     * Initialize a SetEnchantmentsFunction with the provided enchantments and whether or not the enchantments already
     * on the item will get removed.
     */
    public SetEnchantmentsFunction(@NotNull List<LootCondition> conditions,
                                   @NotNull Map<Enchantment, NumberProvider> enchantments, boolean add) {
        super(conditions);
        this.enchantments = Map.copyOf(enchantments);
        this.add = add;
    }

    /**
     * Returns the immutable map of enchantments that will be given to the item
     */
    public @NotNull Map<Enchantment, NumberProvider> enchantments() {
        return enchantments;
    }

    /**
     * Returns whether or not the enchantments already on the item will be removed when this function is applied to it.
     * If this is true, the enchantments will not be removed because they are just getting added, but if this is false,
     * the enchantments that are already on the item will not be kept.
     */
    public boolean add() {
        return add;
    }

    /**
     * If {@link #add()} is false, sets the ItemStack's enchantments to {@link #enchantments()}. Otherwise, just adds
     * them, preserving any current enchantments. <b>If you have a custom ItemMeta implementation that has a different
     * way to add enchantments to an item, you will have to extend this class.</b>
     */
    @Override
    public @NotNull ItemStack modify(@NotNull ItemStack itemStack, @NotNull LootContext context) {
        return itemStack.withMeta(im -> {
            if (!add) {
                im.enchantments(new HashMap<>());
            }
            for (var entry : this.enchantments.entrySet()) {
                im.enchantment(entry.getKey(), (short) entry.getValue().getInt(context));
            }
        });
    }

    @Override
    public String toString() {
        return "SetEnchantmentsFunction[enchantments=" + enchantments + ", add=" + add + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetEnchantmentsFunction that = (SetEnchantmentsFunction) o;
        return add == that.add && enchantments.equals(that.enchantments);
    }

    @Override
    public int hashCode() {
        return enchantments.hashCode() * 31 + Boolean.hashCode(add);
    }

    public static final @NotNull JsonLootConverter<SetEnchantmentsFunction> CONVERTER = new JsonLootConverter<>(
            NamespaceID.from("minecraft:set_enchantments"), SetEnchantmentsFunction.class) {
        @Override
        public @NotNull SetEnchantmentsFunction deserialize(@NotNull JsonObject json, @NotNull ImmuTables loader) throws JsonParseException {
            List<LootCondition> list = ConditionalLootFunction.deserializeConditions(json, loader);

            JsonObject object = JsonHelper.assureJsonObject(json.get("enchantments"), "enchantments");
            Map<Enchantment, NumberProvider> map = new HashMap<>();
            for (var entry : object.entrySet()){
                NamespaceID namespaceID = NamespaceID.from(entry.getKey());

                Enchantment enchantment = Enchantment.fromNamespaceId(namespaceID);
                if (enchantment == null) {
                    throw new JsonParseException("Invalid enchantment \"" + namespaceID + "\"! Did you initialize your enchantment manager correctly?");
                }

                map.put(enchantment, loader.getNumberProviderManager().deserialize(entry.getValue(), entry.getKey()));
            }

            boolean add = JsonHelper.assureBoolean(json.get("add"), "add");

            return new SetEnchantmentsFunction(list, map, add);
        }

        @Override
        public void serialize(@NotNull SetEnchantmentsFunction input, @NotNull JsonObject result, @NotNull ImmuTables loader) throws JsonParseException {
            ConditionalLootFunction.serializeConditionalLootFunction(input, result, loader);
            JsonObject enchantments = new JsonObject();
            for (var entry : input.enchantments.entrySet()){
                enchantments.add(entry.getKey().name(), loader.getNumberProviderManager().serialize(entry.getValue()));
            }
            result.add("enchantments", enchantments);
            result.addProperty("add", input.add);
        }
    };
}