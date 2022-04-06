package dev.goldenstack.loot.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.goldenstack.loot.ImmuTables;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents something that can be serialized to a JsonObject. The method adds data to the provided JsonObject instead
 * of creating its own, to allow for extra data to be added before the serialization part.
 * <p>Must be paired with a {@link LootDeserializer}</p>
 */
public interface LootSerializer<T> {
    /**
     * Adds data to the provided JsonObject. When using this method, do not create your own JsonObject. The reason this
     * method adds data to a JsonObject instead of creating its own is to allow whatever is serializing this object to
     * add data to it before the serialization method does.
     * @param object The object to add data to
     * @param loader The ImmuTables instance, to make it easier to serialize things accurately.
     * @throws JsonParseException if any errors occur
     */
    void serialize(@NotNull JsonObject object, @NotNull ImmuTables loader) throws JsonParseException;

    /**
     * Returns this LootSerializer's NamespacedID. If you're adding this yourself, try to make it unique, so it
     * doesn't mess with anything else.
     */
    @NotNull NamespaceID getKey();

    /**
     * Returns this LootSerializer's deserializer. The purpose of this is to reduce map lookups for
     * {@link JsonSerializationManager} and to allow deserialization for things that aren't registered in a
     * JsonSerializationManager.
     */
    @NotNull LootDeserializer<? extends LootSerializer<T>> getDeserializer();
}
