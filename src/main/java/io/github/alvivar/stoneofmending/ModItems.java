package io.github.alvivar.stoneofmending;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class ModItems {

	private static final ResourceKey<Item> STONE_OF_MENDING_KEY = ResourceKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "stone_of_mending"));

	public static final Item STONE_OF_MENDING = Registry.register(
			BuiltInRegistries.ITEM,
			STONE_OF_MENDING_KEY,
			new Item(new Item.Properties().setId(STONE_OF_MENDING_KEY).stacksTo(1)));

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(STONE_OF_MENDING));
	}
}
