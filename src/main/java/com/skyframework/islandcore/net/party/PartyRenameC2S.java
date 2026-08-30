package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Wire field order: newName.
public record PartyRenameC2S(String newName) implements CustomPayload {
	public static final CustomPayload.Id<PartyRenameC2S> ID = new CustomPayload.Id<>(NetworkChannels.PARTY_RENAME_C2S);

	public static final PacketCodec<RegistryByteBuf, PartyRenameC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, PartyRenameC2S::newName,
			PartyRenameC2S::new
	);

	@Override
	public CustomPayload.Id<PartyRenameC2S> getId() {
		return ID;
	}
}
