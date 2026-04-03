package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoneOfMendingMod implements ModInitializer {

	public static final String MOD_ID = "stone_of_mending";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();

		PayloadTypeRegistry.clientboundPlay().register(
				SelectionSyncPayload.TYPE, SelectionSyncPayload.STREAM_CODEC);

		// Clean up selection state when player disconnects
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			SelectionManager.remove(handler.getPlayer());
		});

		LOGGER.info("Stone of Mending loaded");
	}
}
