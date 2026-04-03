package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoneOfMendingMod implements ModInitializer {

	public static final String MOD_ID = "stone_of_mending";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Stone of Mending loaded");
	}
}
