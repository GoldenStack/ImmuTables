package dev.goldenstack.loot.converter;

import dev.goldenstack.loot.context.LootConversionContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

/**
 * A standard representation of something that can deserialize a configuration node into a specific type of object.
 * @param <L> the loot item type
 * @param <O> the type of output that will be created
 */
public interface LootDeserializer<L, O> {

    /**
     * Deserializes an input node into a resulting object.<br>
     * Note: it is possible for the provided node to not have a value.
     * @param input the configuration node to deserialize into an instance of {@link O}
     * @param context the context object, to use if required
     * @return the provided configuration node deserialized into an object
     * @throws ConfigurateException if the input could not be deserialized for some reason
     */
    @NotNull O deserialize(@NotNull ConfigurationNode input, @NotNull LootConversionContext<L> context) throws ConfigurateException;

}
