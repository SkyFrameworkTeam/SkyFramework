package com.skyframework.islandcore.api.network;

// String keys a future ActionResultS2C can carry so the client can display/localize a reason
// without depending on the exact Spanish wording of IslandCommand's chat messages. Each constant
// below maps to an existing error case in IslandCommand/InviteManagerImpl; extend this list as
// more command error paths grow a network equivalent in later sprints.
public final class ActionReason {

	// "No tienes ninguna isla todavía." (executeInfo, executeList, executeUpgrade, executeSetHome,
	// executeSettings, executeDelete, executeDeleteConfirm, executeInvite, executeTrust,
	// executeUntrust, executeKick, executeBiome)
	public static final String NO_ISLAND = "no_island";

	// "Ya tienes una isla. Usa /island info para ver sus datos." (executeCreate)
	public static final String ALREADY_HAS_ISLAND = "already_has_island";

	// "No tienes permitido crear una isla en este servidor." (executeCreate, ISLAND_CREATE_DENY)
	public static final String NO_PERMISSION = "no_permission";

	// "Solo el propietario de la isla puede cambiar sus ajustes/su bioma." (executeSettings, executeBiome)
	public static final String NOT_OWNER = "not_owner";

	// "El tamaño permitido por tus permisos excede la parcela reservada de tu isla." (executeUpgrade)
	public static final String PLOT_SIZE_EXCEEDED = "plot_size_exceeded";

	// "El home debe fijarse dentro de la parte ya construida de tu isla." (executeSetHome,
	// executeAdminSpawnSetHome)
	public static final String HOME_OUTSIDE_ISLAND = "home_outside_island";

	// "Ajuste desconocido." (executeSettings)
	public static final String UNKNOWN_SETTING = "unknown_setting";

	// "No hay ninguna solicitud de borrado pendiente (o ha expirado)." (executeDeleteConfirm,
	// executeAdminDeleteConfirm)
	public static final String NO_PENDING_DELETION = "no_pending_deletion";

	// "No tienes ninguna invitación pendiente (o ha caducado)." (executeAccept)
	public static final String NO_PENDING_INVITE = "no_pending_invite";

	// "Ya eres el propietario de esta isla." (InviteManagerImpl#invite)
	public static final String ALREADY_OWNER = "already_owner";

	// "Ese jugador ya es miembro de tu isla." (InviteManagerImpl#invite)
	public static final String ALREADY_MEMBER = "already_member";

	// "<jugador> no es miembro de tu isla." (executeKick)
	public static final String NOT_A_MEMBER = "not_a_member";

	// "El bioma X no existe." (executeBiome)
	public static final String BIOME_NOT_FOUND = "biome_not_found";

	// "El bioma X no está disponible." / tier-lock message (executeBiome, BiomeTierRegistry#canUse)
	public static final String BIOME_LOCKED = "biome_locked";

	// "Todavía no puedes volver a cambiar el bioma de tu isla." (executeBiome cooldown check)
	public static final String COOLDOWN_ACTIVE = "cooldown_active";

	// "La dimensión de tu isla no está disponible ahora mismo." (executeBiome)
	public static final String DIMENSION_UNAVAILABLE = "dimension_unavailable";

	// "The spawn island already exists." (executeAdminSpawnCreate)
	public static final String SPAWN_ALREADY_EXISTS = "spawn_already_exists";

	// "La isla de Spawn todavía no existe." (executeAdminSpawnResize, executeAdminSpawnSetHome)
	public static final String SPAWN_NOT_EXISTS = "spawn_not_exists";

	private ActionReason() {
	}
}
