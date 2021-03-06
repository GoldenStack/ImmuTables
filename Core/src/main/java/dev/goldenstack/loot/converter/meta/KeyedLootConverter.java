package dev.goldenstack.loot.converter.meta;

import dev.goldenstack.loot.context.LootConversionContext;
import dev.goldenstack.loot.converter.LootDeserializer;
import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

import java.lang.reflect.AnnotatedType;

/**
 * Handles loot conversion where the exact deserializer will be determined from the key of the configuration node, where
 * values are determined from {@link #key()}, and where the exact serializer will be determined from a map lookup with
 * {@link #convertedType()}.
 * @param <L> the loot item type
 * @param <V> the type of object that will be serialized and deserialized
 */
public abstract class KeyedLootConverter<L, V> implements LootDeserializer<L, V> {

    private final @NotNull String key;
    private final @NotNull TypeToken<V> convertedType;

    public KeyedLootConverter(@NotNull String key, @NotNull TypeToken<V> convertedType) {
        this.key = key;
        this.convertedType = convertedType;
    }

    /**
     * Although this key represents the key of this converter, it will actually be a value in the key-value map of a
     * configuration nodes that will be provided to {@link #serialize(Object, ConfigurationNode, LootConversionContext)}.
     * This converter does not know the actual key that this converter's key will be stored at because it allows for
     * outside customizability.
     * @return the key of this converter
     */
    public @NotNull String key() {
        return key;
    }

    /**
     * This is the exact type of object that is required as input. Not even subclasses of this are valid; they are
     * compared with {@link TypeToken#equals(Object)} which will likely delegate to something like
     * {@link io.leangen.geantyref.GenericTypeReflector#equals(AnnotatedType, AnnotatedType)}
     * @return a type token representing the type of object that this converter will convert
     */
    public @NotNull TypeToken<V> convertedType() {
        return convertedType;
    }

    /**
     * Serializes an input object into a resulting node.<br>
     * Note: it is valid, but not recommended, for the resulting node to be set to a non-map (including null) value.
     * @param input the input object to serialize onto the node
     * @param result the configuration node to serialize values onto. Importantly, this node might already have values.
     * @param context the context object, to use if required
     * @throws ConfigurateException if the input could not be serialized on to the provided object for some reason
     */
    public abstract void serialize(@NotNull V input, @NotNull ConfigurationNode result, @NotNull LootConversionContext<L> context) throws ConfigurateException;
}
