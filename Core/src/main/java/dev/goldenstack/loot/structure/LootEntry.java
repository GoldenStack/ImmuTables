package dev.goldenstack.loot.structure;

import dev.goldenstack.loot.context.LootContext;
import dev.goldenstack.loot.conversion.LootAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.List;

/**
 * An entry in a loot table that is able to provide a list of options for the loot table to choose from. Each option may
 * have its own loot and weight.
 * @param <L> the loot item
 */
public interface LootEntry<L> extends LootAware<L> {

    /**
     * Requests a list of options from this entry.
     * @return the generated options
     */
    @NotNull List<Option<L>> requestOptions(@NotNull LootContext context);

    interface Option<L> extends LootAware<L> {

        /**
         * <b>IMPORTANT: This number should not be below 1.</b>
         * @return the weight of this option, which will be used while choosing between options
         */
        @Range(from = 1L, to = Long.MAX_VALUE) long getWeight(@NotNull LootContext context);

        /**
         * Generates loot based on the provided loot context.
         * @return the list of loot items that were generated
         */
        @NotNull List<L> generate(@NotNull LootContext context);

    }
}