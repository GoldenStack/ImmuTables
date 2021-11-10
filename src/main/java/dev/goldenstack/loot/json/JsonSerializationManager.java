package dev.goldenstack.loot.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.goldenstack.loot.ImmuTables;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Manages serialization and deserialization for groups of serializable classes.
 * @param <T> Something that is a {@link LootSerializer}
 */
public class JsonSerializationManager <T extends LootSerializer<?>> {

    private final @NotNull String elementName;
    private final @NotNull Map<String, LootDeserializer<T>> registry;
    private final @NotNull ImmuTables owner;
    private @Nullable BiFunction<JsonElement, JsonSerializationManager<T>, T> defaultDeserializer;
    private JsonSerializationManager(boolean useConcurrentHashMap, @NotNull String elementName, @NotNull ImmuTables owner){
        this.registry = useConcurrentHashMap ? new ConcurrentHashMap<>() : new HashMap<>();
        this.elementName = elementName;
        this.owner = owner;
    }

    /**
     * Returns this JsonSerializationManager's default deserializer
     */
    public @Nullable BiFunction<JsonElement, JsonSerializationManager<T>, T> defaultDeserializer(){
        return defaultDeserializer;
    }

    /**
     * Sets this JsonSerializationManager's default deserializer to the provided value
     */
    public void defaultDeserializer(@Nullable BiFunction<JsonElement, JsonSerializationManager<T>, T> defaultDeserializer){
        this.defaultDeserializer = defaultDeserializer;
    }

    /**
     * Associates the provided NamespaceID with the provided deserializer (only for this specific manager)
     * @param key The key
     * @param value The value
     */
    public void register(@NotNull NamespaceID key, @NotNull LootDeserializer<T> value){
        this.registry.put(key.asString(), value);
    }

    /**
     * Unregisters the provided NamespaceID from this manager
     * @param key The key to remove
     * @return True if a non-null key was removed, false if there was no key or if the key was null
     */
    public boolean unregister(@NotNull NamespaceID key){
        return this.registry.remove(key.asString()) != null;
    }

    /**
     * Attempts to find a {@link LootDeserializer} based on the provided NamespacedID
     * @param key The key to search for
     * @return The deserializer that was found, or null if none was found.
     */
    public @Nullable LootDeserializer<T> request(@NotNull NamespaceID key){
        return this.registry.get(key.asString());
    }

    /**
     * Clears all of the registered values from this JsonSerializationManager
     */
    public void clear(){
        this.registry.clear();
    }

    /**
     * @return This manager's {@code elementName} that it uses for serialization and deserialization.
     */
    public @NotNull String getElementName(){
        return elementName;
    }

    /**
     * Serializes the provided object according to its deserializer. Technically, a JsonSerializationManager isn't
     * needed for this, but it makes it simpler and automatically manages things such as the element name.
     * @param t The object to serialize
     * @return The generated JsonObject
     * @throws JsonParseException if the argument's deserializer throws an error
     */
    public @NotNull JsonObject serialize(@NotNull T t) throws JsonParseException {
        JsonObject object = new JsonObject();
        object.addProperty(this.elementName, t.getKey().asString());
        t.serialize(object, this.owner);
        return object;
    }

    /**
     * Deserializes the provided JsonElement. If this has a {@link #defaultDeserializer} and the element could not
     * be deserialized (not from the deserializer, but, for example, if the element wasn't a JsonElement), the default
     * deserializer will be given the element to parse. If it returns null or there is no default deserializer, an error
     * will be thrown.
     * @param element The JsonElement that should be deserialized
     * @param key The key (to improve error messages). Setting it to null will just change the message slightly.
     * @return The deserialized object
     * @throws JsonParseException if there is an error in the deserialization process
     */
    public @NotNull T deserialize(@Nullable JsonElement element, @Nullable String key) throws JsonParseException {
        if (element != null && element.isJsonObject()){
            JsonObject object = element.getAsJsonObject();
            JsonElement rawElement = object.get(elementName);
            if (rawElement != null && rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()){
                String type = rawElement.getAsString();
                LootDeserializer<T> t = this.registry.get(type);
                if (t != null){
                    return t.deserialize(object, this.owner);
                }
                throw new JsonParseException("Could not find deserializer for type \"" + type + "\"!");
            }
        }
        if (this.defaultDeserializer != null) {
            T t = this.defaultDeserializer.apply(element, this);
            if (t != null){
                return t;
            }
        }
        throw new JsonParseException(JsonHelper.expectedNotNullMessage(key, element));
    }

    /**
     * Creates a new {@link Builder}
     */
    public static @NotNull <T extends LootSerializer<?>> Builder<T> builder(){
        return new Builder<>();
    }


    /**
     * Utility class for building JsonSerializationManager instances
     */
    public static class Builder <T extends LootSerializer<?>> {
        private boolean useConcurrentHashMap;

        private String elementName = null;
        private BiFunction<JsonElement, JsonSerializationManager<T>, T> defaultDeserializer = null;
        private ImmuTables owner;

        private Map<String, LootDeserializer<T>> deserializers = null;

        private Builder(){}

        private void assureMapExists(){
            if (this.deserializers == null){
                this.deserializers = new HashMap<>();
            }
        }

        /**
         * Associates the provided value with the provided key. The internal map is not concurrent, but that shouldn't
         * cause many problems.
         */
        @Contract("_, _ -> this")
        public @NotNull Builder<T> putDeserializer(@NotNull NamespaceID key, @NotNull LootDeserializer<T> deserializer){
            assureMapExists();
            this.deserializers.put(key.asString(), deserializer);
            return this;
        }

        /**
         * Removes the key from this builder. The internal map is not concurrent, but that shouldn't cause many problems.
         */
        @Contract("_ -> this")
        public @NotNull Builder<T> removeDeserializer(@NotNull NamespaceID key){
            assureMapExists();
            this.deserializers.remove(key.asString());
            return this;
        }

        /**
         * Sets the {@code elementName} for instances created from this builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder<T> elementName(@NotNull String elementName){
            this.elementName = elementName;
            return this;
        }

        /**
         * Makes instances created from this builder use a ConcurrentHashMap for storage instead of a normal HashMap
         * when true.
         */
        @Contract("_ -> this")
        public @NotNull Builder<T> useConcurrentHashMap(boolean useConcurrentHashMap){
            this.useConcurrentHashMap = useConcurrentHashMap;
            return this;
        }

        /**
         * Sets the {@code defaultDeserializer} for instances created with this builder.<br>
         * The default deserializer handles deserialization when it would normally fail so that other cases can be
         * handled.
         */
        @Contract("_ -> this")
        public @NotNull Builder<T> defaultDeserializer(@NotNull BiFunction<JsonElement, JsonSerializationManager<T>, T> defaultDeserializer){
            this.defaultDeserializer = defaultDeserializer;
            return this;
        }

        /**
         * Sets the {@code owner} for instances created with this builder.<br>
         * The owner allows objects that are being deserialized to deserialize other objects that they need to.
         */
        @Contract("_ -> this")
        public @NotNull Builder<T> owner(@NotNull ImmuTables owner){
            this.owner = owner;
            return this;
        }

        /**
         * Builds a {@code JsonSerializationManager} instance from this builder.
         */
        public JsonSerializationManager<T> build(){
            Objects.requireNonNull(elementName, "This builder must have an elementName!");
            Objects.requireNonNull(owner, "This builder must have an owner!");
            JsonSerializationManager<T> manager = new JsonSerializationManager<>(this.useConcurrentHashMap, this.elementName, this.owner);
            manager.defaultDeserializer = this.defaultDeserializer;
            if (this.deserializers != null){
                manager.registry.putAll(this.deserializers);
            }
            return manager;
        }
    }
}