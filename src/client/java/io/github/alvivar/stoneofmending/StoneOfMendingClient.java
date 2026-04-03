package io.github.alvivar.stoneofmending;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class StoneOfMendingClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SelectionSyncPayload.TYPE, (payload, context) -> {
			ClientSelectionState.update(payload);
		});

		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
			ClientSelectionState.clear();
		});

		SelectionRenderer.register();
	}
}
