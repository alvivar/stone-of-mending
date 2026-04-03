package io.github.alvivar.stoneofmending;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: player left-clicked air to clear their selection.
 */
public record ClearSelectionC2SPayload() implements CustomPacketPayload {

	public static final Type<ClearSelectionC2SPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "clear_selection"));

	public static final StreamCodec<FriendlyByteBuf, ClearSelectionC2SPayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> {}, buf -> new ClearSelectionC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
