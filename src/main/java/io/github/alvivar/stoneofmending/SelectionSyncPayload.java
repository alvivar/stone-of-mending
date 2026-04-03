package io.github.alvivar.stoneofmending;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record SelectionSyncPayload(
		@Nullable BlockPos pointA,
		@Nullable BlockPos pointB,
		@Nullable Direction normal,
		@Nullable ResourceKey<Level> dimension,
		int frontierOffset) implements CustomPacketPayload {

	public static final Type<SelectionSyncPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(StoneOfMendingMod.MOD_ID, "selection_sync"));

	public static final StreamCodec<FriendlyByteBuf, SelectionSyncPayload> STREAM_CODEC = StreamCodec
			.of(SelectionSyncPayload::write, SelectionSyncPayload::read);

	private static void write(FriendlyByteBuf buf, SelectionSyncPayload payload) {
		boolean hasSelection = payload.pointA != null && payload.normal != null && payload.dimension != null;
		buf.writeBoolean(hasSelection);
		if (!hasSelection)
			return;

		buf.writeBlockPos(payload.pointA);
		buf.writeBoolean(payload.pointB != null);
		if (payload.pointB != null) {
			buf.writeBlockPos(payload.pointB);
		}
		buf.writeEnum(payload.normal);
		buf.writeResourceKey(payload.dimension);
		buf.writeVarInt(payload.frontierOffset);
	}

	private static SelectionSyncPayload read(FriendlyByteBuf buf) {
		if (!buf.readBoolean()) {
			return new SelectionSyncPayload(null, null, null, null, 0);
		}

		BlockPos a = buf.readBlockPos();
		BlockPos b = buf.readBoolean() ? buf.readBlockPos() : null;
		Direction normal = buf.readEnum(Direction.class);
		ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
		int offset = buf.readVarInt();
		return new SelectionSyncPayload(a, b, normal, dim, offset);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static SelectionSyncPayload from(Selection sel) {
		return new SelectionSyncPayload(
				sel.pointA(), sel.pointB(), sel.normal(), sel.dimension(), sel.frontierOffset());
	}
}
