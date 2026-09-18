package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

// Sets all four toggles at once, the full desired state rather than a single flip — see
// PlayerLocationSharingConfig#setSendPositionToParty/#setReceivePositionsFromParty/
// #setSendPositionToAllies/#setReceivePositionsFromAllies.
//
// Wire field order: sendPositionToParty, receivePositionsFromParty, sendPositionToAllies,
// receivePositionsFromAllies.
public record LocationSharingSetC2S(
		boolean sendPositionToParty,
		boolean receivePositionsFromParty,
		boolean sendPositionToAllies,
		boolean receivePositionsFromAllies
) implements CustomPayload {
	public static final CustomPayload.Id<LocationSharingSetC2S> ID =
			new CustomPayload.Id<>(NetworkChannels.LOCATION_SHARING_SET_C2S);

	public static final PacketCodec<RegistryByteBuf, LocationSharingSetC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, LocationSharingSetC2S::sendPositionToParty,
			PacketCodecs.BOOL, LocationSharingSetC2S::receivePositionsFromParty,
			PacketCodecs.BOOL, LocationSharingSetC2S::sendPositionToAllies,
			PacketCodecs.BOOL, LocationSharingSetC2S::receivePositionsFromAllies,
			LocationSharingSetC2S::new
	);

	@Override
	public CustomPayload.Id<LocationSharingSetC2S> getId() {
		return ID;
	}
}
