package io.github.alvivar.stoneofmending;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

	private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

	public static Selection getOrCreate(ServerPlayer player) {
		return SELECTIONS.computeIfAbsent(player.getUUID(), k -> new Selection());
	}

	public static @Nullable Selection get(ServerPlayer player) {
		return SELECTIONS.get(player.getUUID());
	}

	public static void remove(ServerPlayer player) {
		SELECTIONS.remove(player.getUUID());
	}

	public static void sync(ServerPlayer player) {
		Selection sel = getOrCreate(player);
		ServerPlayNetworking.send(player, SelectionSyncPayload.from(sel));
	}

	/** Clear selections for players no longer holding the Stone. */
	public static void tick(MinecraftServer server) {
		if (SELECTIONS.isEmpty()) return;

		List<ServerPlayer> toClear = null;
		for (var entry : SELECTIONS.entrySet()) {
			if (!entry.getValue().hasA()) continue;
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) continue;
			if (!player.getMainHandItem().is(ModItems.STONE_OF_MENDING)) {
				if (toClear == null) toClear = new ArrayList<>();
				toClear.add(player);
			}
		}

		if (toClear != null) {
			for (ServerPlayer player : toClear) {
				SELECTIONS.remove(player.getUUID());
				ServerPlayNetworking.send(player, SelectionSyncPayload.from(new Selection()));
			}
		}
	}
}
