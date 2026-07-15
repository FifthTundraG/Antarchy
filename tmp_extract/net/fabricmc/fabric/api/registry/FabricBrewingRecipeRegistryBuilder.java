/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.api.registry;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.class_1792;
import net.minecraft.class_1842;
import net.minecraft.class_1845;
import net.minecraft.class_1856;
import net.minecraft.class_6880;
import net.minecraft.class_7699;

/**
 * An extension of {@link class_1845.class_9665} to support ingredients.
 */
public interface FabricBrewingRecipeRegistryBuilder {
	/**
	 * An event that is called when the brewing recipe registry is being built.
	 */
	Event<FabricBrewingRecipeRegistryBuilder.BuildCallback> BUILD = EventFactory.createArrayBacked(FabricBrewingRecipeRegistryBuilder.BuildCallback.class, listeners -> builder -> {
		for (FabricBrewingRecipeRegistryBuilder.BuildCallback listener : listeners) {
			listener.build(builder);
		}
	});

	default void registerItemRecipe(class_1792 input, class_1856 ingredient, class_1792 output) {
		throw new AssertionError("Must be implemented via interface injection");
	}

	default void registerPotionRecipe(class_6880<class_1842> input, class_1856 ingredient, class_6880<class_1842> output) {
		throw new AssertionError("Must be implemented via interface injection");
	}

	default void registerRecipes(class_1856 ingredient, class_6880<class_1842> potion) {
		throw new AssertionError("Must be implemented via interface injection");
	}

	default class_7699 getEnabledFeatures() {
		throw new AssertionError("Must be implemented via interface injection");
	}

	/**
	 * Use this event to register custom brewing recipes.
	 */
	@FunctionalInterface
	interface BuildCallback {
		/**
		 * Called when the brewing recipe registry is being built.
		 *
		 * @param builder the {@link class_1845} instance
		 */
		void build(class_1845.class_9665 builder);
	}
}
