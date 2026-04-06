package io.github.alvivar.stoneofmending;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetNormalC2SPayload() implements CustomPacketPayload {

	public static final Type<SetNormalC2SPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "set_normal")
	);

	public static final StreamCodec<FriendlyByteBuf, SetNormalC2SPayload> STREAM_CODEC =
			StreamCodec.unit(new SetNormalC2SPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
