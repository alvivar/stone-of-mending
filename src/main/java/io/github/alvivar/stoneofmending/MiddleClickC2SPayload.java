package io.github.alvivar.stoneofmending;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: player middle-clicked to replace blocks in selection.
 */
public record MiddleClickC2SPayload() implements CustomPacketPayload {

	public static final Type<MiddleClickC2SPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "middle_click"));

	public static final StreamCodec<FriendlyByteBuf, MiddleClickC2SPayload> STREAM_CODEC =
			StreamCodec.of((buf, payload) -> {}, buf -> new MiddleClickC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
