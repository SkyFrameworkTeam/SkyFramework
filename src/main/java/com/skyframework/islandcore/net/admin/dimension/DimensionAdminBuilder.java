package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.dimension.model.DimensionDefinition;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Pure assembly for the Dimension Manager, mirroring AdminIslandBuilder/IslandSnapshotBuilder: no
// business logic, just mapping DimensionRegistry's own domain objects onto the wire format.
public final class DimensionAdminBuilder {

	// Same pattern/zone as IslandMessages/AdminIslandBuilder's own DATE_FORMATTER, so createdAt/
	// updatedAt read identically across the whole admin block regardless of which screen shows them.
	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

	private DimensionAdminBuilder() {
	}

	public static DimensionListS2C.DimensionEntry buildEntry(DimensionDefinition dimension) {
		return new DimensionListS2C.DimensionEntry(
				dimension.getId().toString(),
				dimension.getDisplayName(),
				dimension.getGeneratorStyle().name(),
				dimension.getSeed(),
				dimension.getState().name()
		);
	}

	public static DimensionDetailS2C buildDetail(DimensionDefinition dimension) {
		return new DimensionDetailS2C(
				dimension.getId().toString(),
				dimension.getDisplayName(),
				dimension.getGeneratorStyle().name(),
				dimension.getSeed(),
				dimension.getState().name(),
				DATE_FORMATTER.format(dimension.getCreatedAt()),
				DATE_FORMATTER.format(dimension.getUpdatedAt())
		);
	}
}
