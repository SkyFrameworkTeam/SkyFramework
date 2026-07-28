package com.skyframework.islandcore.island.model;

import com.skyframework.islandcore.api.island.IslandPermission;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

public record IslandMember(UUID playerUuid, IslandRole role, Instant addedAt, EnumSet<IslandPermission> overrides) {
}
