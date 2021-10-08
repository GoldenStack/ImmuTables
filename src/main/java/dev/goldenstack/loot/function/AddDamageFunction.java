package dev.goldenstack.loot.function;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.goldenstack.loot.LootTableLoader;
import dev.goldenstack.loot.condition.LootCondition;
import dev.goldenstack.loot.context.LootContext;
import dev.goldenstack.loot.json.JsonHelper;
import dev.goldenstack.loot.json.LootDeserializer;
import dev.goldenstack.loot.json.LootSerializer;
import dev.goldenstack.loot.provider.number.NumberProvider;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a {@code LootFunction} that sets the amount of damage that the provided ItemStack has.
 */
public class AddDamageFunction extends ConditionalLootFunction {
    /**
     * The immutable key for all {@code AddDamageFunction}s
     */
    public static final @NotNull NamespaceID KEY = NamespaceID.from(NamespaceID.MINECRAFT_NAMESPACE, "set_damage");

    private final NumberProvider damage;
    private final boolean add;

    /**
     * Initialize an AddDamageFunction with the provided damage and whether or not the damage should be added
     */
    public AddDamageFunction(@NotNull ImmutableList<LootCondition> conditions, @NotNull NumberProvider damage, boolean add){
        super(conditions);
        this.damage = damage;
        this.add = add;
    }

    /**
     * Returns the NumberProvider that is used to calculate how much damage should be added
     */
    public @NotNull NumberProvider damage() {
        return damage;
    }

    /**
     * Returns whether the damage from {@link #damage()} should be added to the ItemStack's current damage or its damage
     * should be set to the value.
     */
    public boolean add() {
        return add;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(@NotNull JsonObject object, @NotNull LootTableLoader loader) throws JsonParseException {
        super.serialize(object, loader);
        object.add("damage", loader.getNumberProviderManager().serialize(this.damage));
        object.addProperty("add", this.add);
    }

    /**
     * {@inheritDoc}
     * @return {@link #KEY}
     */
    @Override
    public @NotNull NamespaceID getKey() {
        return KEY;
    }

    /**
     * Modifies the ItemStack's damage according to {@link #damage()} and {@link #add()}.<br>
     * Looking at the source for this method could be very helpful.
     */
    @Override
    public @NotNull ItemStack modify(@NotNull ItemStack itemStack, @NotNull LootContext context) {
        // Don't change the durability if the item is unbreakable.
        if (itemStack.getMeta().isUnbreakable()) {
            return itemStack;
        }
        // Store the maximum damage value for later use
        final int max_damage = itemStack.getMaterial().registry().maxDamage();

        // Don't change the durability if the item has no durability
        if (max_damage == 0){
            return itemStack;
        }

        // The double is a number from 0 to 1 that represents the percentage of the durability that it should have.
        // To convert it to a damage value, we first need to subtract it from 1.
        // This is because the damage value isn't stored as the durability that is left; it's stored as the amount of damage that has happened.
        // 0 damage means it's at 100% durability, while 400 damage (assuming 400 max durability) means it's at 0% durability.
        // After we've done that math, we can multiply it by the max_damage value.
        // We also have to cast it to an integer because damage amounts are integers.
        final int value = (int) ((1 - this.damage.getDouble(context)) * max_damage);

        // Different calculations are required if we need to factor in the current durability.
        if (add){
            final int damage = itemStack.getMeta().getDamage();
            // The reason we use "damage - (max_damage - value)" is because the current damage still needs to be taken into account.
            // If the value variable represented durability instead of damage, we could simply write "damage - value".
            // However, value must be converted to a durability value instead with a simple "max_damage - value"
            // At the end, we just need to clamp the resulting value.
            return itemStack.withMeta(builder -> builder.damage(Math.min(Math.max(damage - (max_damage - value), 0), max_damage)));
        } else {
            // Make sure to clamp the value so that nothing has more than 100% or less than 0% durability.
            // The raw value variable can be used because it simply represents the amount of damage and no other numbers are taken into account.
            return itemStack.withMeta(builder -> builder.damage(Math.min(Math.max(value, 0), max_damage)));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull LootDeserializer<? extends LootSerializer<LootFunction>> getDeserializer() {
        return AddDamageFunction::deserialize;
    }

    @Override
    public String toString() {
        return "AddDamageFunction[conditions=" + conditions() + ", damage=" + damage + ", add=" + add + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddDamageFunction that = (AddDamageFunction) o;
        return conditions().equals(that.conditions()) && add == that.add && Objects.equals(damage, that.damage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(damage) * 31 + Boolean.hashCode(add);
    }

    /**
     * Static method to deserialize a {@code JsonObject} to an {@code AddDamageFunction}
     */
    public static @NotNull LootFunction deserialize(@NotNull JsonObject json, @NotNull LootTableLoader loader) throws JsonParseException {
        ImmutableList<LootCondition> list = ConditionalLootFunction.deserializeConditions(json, loader);
        NumberProvider provider = loader.getNumberProviderManager().deserialize(json.get("damage"), "damage");
        boolean add = JsonHelper.assureBoolean(json.get("add"), "add");
        return new AddDamageFunction(list, provider, add);
    }
}