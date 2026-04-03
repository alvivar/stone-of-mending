package io.github.alvivar.stoneofmending;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

	private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

	public static Selection getOrCreate(ServerPlayer player) {
		return SELECTIONS.computeIfAbsent(player.getUUID(), k -> new Selection());
	}

	public static void remove(ServerPlayer player) {
		SELECTIONS.remove(player.getUUID());
	}

	public static void sync(ServerPlayer player) {
		Selection sel = getOrCreate(player);
		ServerPlayNetworking.send(player, SelectionSyncPayload.from(sel));
	}
}
