package com.skyframework.islandcore.protection.flag;

import com.skyframework.islandcore.island.model.IslandRole;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

// A named shortcut for setting VISITOR/ALLY/MEMBER's overrides on a ROLE_BASED flag all at once
// ("/island flags preset", FlagSetPresetC2S). OWNER, CO_OWNER and DENIED are never touched by any
// preset: OWNER always resolves ALLOW regardless of any override (see FlagResolver's OWNER
// short-circuit), CO_OWNER is likewise a hard-coded always-ALLOW role the owner can't weaken this
// way (see IslandRole's javadoc — only promote/demote via MembershipService#trust/untrust moves a
// player into or out of it), and DENIED is an explicit per-player block the owner set
// individually — a role-wide preset overwriting that silently would defeat the point of it being
// per-player.
public enum FlagPreset {
	NOBODY("nadie", false, false, false),
	MEMBERS("miembros", false, false, true),
	ALLIES("aliados", false, true, true),
	EVERYONE("todos", true, true, true);

	private final String id;
	private final boolean visitorAllow;
	private final boolean allyAllow;
	private final boolean memberAllow;

	FlagPreset(String id, boolean visitorAllow, boolean allyAllow, boolean memberAllow) {
		this.id = id;
		this.visitorAllow = visitorAllow;
		this.allyAllow = allyAllow;
		this.memberAllow = memberAllow;
	}

	public String getId() {
		return id;
	}

	public Map<IslandRole, TriState> toRoleValues() {
		Map<IslandRole, TriState> values = new EnumMap<>(IslandRole.class);
		values.put(IslandRole.VISITOR, TriState.fromBoolean(visitorAllow));
		values.put(IslandRole.ALLY, TriState.fromBoolean(allyAllow));
		values.put(IslandRole.MEMBER, TriState.fromBoolean(memberAllow));
		return values;
	}

	public static Optional<FlagPreset> fromId(String id) {
		for (FlagPreset preset : values()) {
			if (preset.id.equals(id)) {
				return Optional.of(preset);
			}
		}
		return Optional.empty();
	}

	// Reverse lookup: given the RESOLVED (not raw override) VISITOR/ALLY/MEMBER values for a flag —
	// i.e. exactly what FlagResolver#resolveForRole already returns for each — which preset (if
	// any) they exactly match. Used by FlagsStatusBuilder to compute FlagsStatusS2C#currentPreset
	// ("custom" if none match, e.g. after a per-role IslandData#getRoleFlagOverride was ever set
	// individually outside a preset).
	public static Optional<FlagPreset> matching(boolean visitorAllow, boolean allyAllow, boolean memberAllow) {
		for (FlagPreset preset : values()) {
			if (preset.visitorAllow == visitorAllow && preset.allyAllow == allyAllow && preset.memberAllow == memberAllow) {
				return Optional.of(preset);
			}
		}
		return Optional.empty();
	}
}
