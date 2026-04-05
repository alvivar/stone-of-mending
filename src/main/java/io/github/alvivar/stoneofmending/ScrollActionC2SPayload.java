package io.github.alvivar.stoneofmending;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server packet: player scrolled while holding Stone with active selection.
 * direction: +1 = scroll up (place), -1 = scroll down (collect)
 */
public record ScrollActionC2SPayload(int direction, boolean shifted) implements CustomPacketPayload {

	public static final Type<ScrollActionC2SPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "scroll_action"));

	public static final StreamCodec<FriendlyByteBuf, ScrollActionC2SPayload> STREAM_CODEC =
			StreamCodec.of(ScrollActionC2SPayload::write, ScrollActionC2SPayload::read);

	private static void write(FriendlyByteBuf buf, ScrollActionC2SPayload payload) {
		buf.writeByte(payload.direction);
		buf.writeBoolean(payload.shifted);
	}

	private static ScrollActionC2SPayload read(FriendlyByteBuf buf) {
		return new ScrollActionC2SPayload(buf.readByte(), buf.readBoolean());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
