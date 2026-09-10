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

	// "<jugador> no es copropietario de tu isla." (executeUntrust, MembershipService#untrust —
	// untrust only demotes an existing CO_OWNER, it never applies to a plain MEMBER)
	public static final String NOT_CO_OWNER = "not_co_owner";

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

	// Added alongside the Sprint "acciones de isla" service extraction (Paso 0):

	// "Tu isla no tiene un home asignado." (TeleportManagerImpl#requestHome)
	public static final String HOME_NOT_SET = "home_not_set";

	// "Esta isla ya está en proceso de eliminación." (IslandDeletionServiceImpl#requestDeletion)
	public static final String ISLAND_ALREADY_DELETING = "island_already_deleting";

	// GameProfile/UUID resolution failed for a target name (MembershipService#invite via the
	// network path, which has no Brigadier GameProfileArgumentType to resolve it for free).
	public static final String TARGET_NOT_FOUND = "target_not_found";

	// "El comando /spawn está desactivado en este servidor." (SpawnCommand — network path only,
	// see TeleportRequestC2S; the text command keeps its own source.sendError check unchanged)
	public static final String SPAWN_DISABLED = "spawn_disabled";

	// "El comando /farming está desactivado en este servidor." (FarmingCommand — network path only)
	public static final String FARMING_DISABLED = "farming_disabled";

	// "El comando /rtp está desactivado en este servidor." (TeleportManagerImpl#requestRtp)
	public static final String RTP_DISABLED = "rtp_disabled";

	// "No puedes usar /rtp en esta dimensión." (TeleportManagerImpl#requestRtp)
	public static final String RTP_DIMENSION_NOT_ALLOWED = "rtp_dimension_not_allowed";

	// "No se ha podido encontrar un lugar seguro. Inténtalo de nuevo." (TeleportManagerImpl#requestRtp)
	public static final String RTP_NO_SAFE_LOCATION = "rtp_no_safe_location";

	// Admin network block (island list/detail/delete, Spawn management, Dimension Manager, vanilla
	// reset queue). Unlike every constant above, these carry an "error." prefix on the wire — this
	// block is a distinct, newer packet family with its own client-side lookup convention; the
	// prefix is deliberate, not a typo, and does not retroactively apply to the bare keys above.

	// Sender isn't a server operator (hasPermissionLevel(2)) for one of this admin block's actions.
	// Deliberately separate from NO_PERMISSION above: that one means specifically "no LuckPerms
	// permission to create an island" (executeCreate/ISLAND_CREATE_DENY) and must keep meaning only
	// that — this is a different failure (missing operator status), reused by every operator check
	// in ServerPacketHandlers' admin block instead of overloading NO_PERMISSION with a second
	// meaning.
	public static final String NOT_OPERATOR = "error.not_operator";

	// Target player has no island (AdminIslandDetailC2S/AdminIslandDeleteC2S/ConfirmC2S): distinct
	// from NO_ISLAND, which is first-person ("you don't have an island") — this one is about a
	// third party the admin is looking up.
	public static final String ISLAND_NOT_FOUND = "error.island_not_found";

	// No managed dimension with the given id (DimensionDetail/Delete/Regenerate request handlers;
	// DimensionRegistryImpl#requireActive's IllegalArgumentException branch).
	public static final String DIMENSION_NOT_FOUND = "error.dimension_not_found";

	// DimensionRegistryImpl#createDimension's IllegalStateException (id already registered).
	public static final String DIMENSION_ALREADY_EXISTS = "error.dimension_already_exists";

	// Identifier.of("islandcore", id) rejected the id string (DimensionCreateC2S) — mirrors
	// DimensionCommand#executeCreate's own separate catch for a malformed id argument.
	public static final String INVALID_DIMENSION_ID = "error.invalid_dimension_id";

	// DimensionGeneratorStyle.valueOf(...) rejected the style string (DimensionCreateC2S).
	public static final String INVALID_STYLE = "error.invalid_style";

	// SpawnIslandCreateC2S when the Spawn island already exists (IllegalStateException from
	// IslandRegistry#createSpawnIsland). Distinct wire value from the older SPAWN_ALREADY_EXISTS,
	// which this admin block does not reuse (see class javadoc above).
	public static final String SPAWN_ISLAND_ALREADY_EXISTS = "error.spawn_island_already_exists";

	// SpawnIslandResizeC2S/SpawnIslandSetHomeC2S when the Spawn island doesn't exist yet. Distinct
	// wire value from the older SPAWN_NOT_EXISTS, same reasoning as above.
	public static final String SPAWN_ISLAND_NOT_FOUND = "error.spawn_island_not_found";

	// SpawnIslandSetHomeC2S when the sender isn't standing inside the Spawn island's built bounds —
	// the same bounds+dimension check /island admin spawn sethome already performs inline.
	public static final String UNSAFE_LOCATION = "error.unsafe_location";

	// A confirm/cancel packet (island delete, dimension delete/regenerate, vanilla reset
	// confirm/cancel) found no matching pending request. Every underlying service collapses
	// "never requested" and "already expired" into the same boolean false with no way to tell them
	// apart from the public interface (confirmed by reading IslandDeletionServiceImpl,
	// DimensionRegistryImpl, and VanillaResetService's confirm methods) — so this is the only one
	// of the two actually reachable today; CONFIRMATION_EXPIRED below is added per spec but not
	// currently distinguishable/used.
	public static final String NO_PENDING_CONFIRMATION = "error.no_pending_confirmation";

	// Reserved: would need a service-level change (expose whether a just-missed request existed vs.
	// never existed) to ever be distinguishable from NO_PENDING_CONFIRMATION. Not reused anywhere
	// yet — see NO_PENDING_CONFIRMATION's comment.
	public static final String CONFIRMATION_EXPIRED = "error.confirmation_expired";

	// VanillaResetService#requireValidKey's IllegalArgumentException (dimensionKey isn't one of
	// VanillaResetService.VALID_DIMENSION_KEYS).
	public static final String VANILLA_DIMENSION_INVALID = "error.vanilla_dimension_invalid";

	// VanillaResetQueueC2S when VanillaResetService#listPendingResets() already has an IN_PROGRESS
	// entry for the requested dimension. VanillaResetService#requestReset itself has no such guard
	// (re-requesting silently replaces the queued entry on confirm), so this check is done in the
	// packet handler itself, as a read-only pre-check via the existing listPendingResets(), without
	// changing that service's own (intentionally idempotent) behavior for the text command.
	public static final String VANILLA_RESET_ALREADY_QUEUED = "error.vanilla_reset_already_queued";

	// DimensionRegistryImpl#requireActive's IllegalStateException branch (dimension exists but isn't
	// ACTIVE — already DELETING or REGENERATING). Not in the originally specified key list; added
	// because requireActive's two failure branches need two different reasons and only one
	// (DIMENSION_NOT_FOUND) was specified.
	public static final String DIMENSION_STATE_CONFLICT = "error.dimension_state_conflict";

	// Sent instead of actually processing any C2S packet from a player whose ClientHandshakeC2S
	// reported a protocolVersion that doesn't match NetworkChannels.PROTOCOL_VERSION (see
	// ClientSyncNotifier#isProtocolIncompatible and ServerPacketHandlers#registerGuarded). The
	// handshake reply itself already carries protocolCompatible=false as the primary warning; this
	// is the fallback for a client that ignores it and keeps sending packets anyway.
	public static final String PROTOCOL_MISMATCH = "error.protocol_mismatch";

	// Flags/exception-groups network block. Player-facing (not admin-only), so — like every
	// constant above the admin block — no "error." prefix.

	// FlagSetC2S when the flagId doesn't match any registered Flag (FlagRegistry#get).
	public static final String FLAG_NOT_FOUND = "flag_not_found";

	// FlagSetC2S when the value string isn't one of allow/deny/default (TriState#valueOf).
	public static final String INVALID_FLAG_VALUE = "invalid_flag_value";

	// FlagSetPresetC2S / IslandRegistryApi#applyFlagPreset's IllegalArgumentException: flagId isn't
	// a ROLE_BASED flag, or preset isn't one of nadie/miembros/aliados/todos (FlagPreset#fromId).
	public static final String INVALID_FLAG_PRESET = "invalid_flag_preset";

	// ExceptionGroupsStatusRequestC2S/ExceptionGroupSetC2S when the groupId doesn't match any
	// registered ExceptionGroup (ExceptionGroupRegistry#getGroup).
	public static final String EXCEPTION_GROUP_NOT_FOUND = "exception_group_not_found";

	// ExceptionGroupSetPresetC2S when the group exists but ExceptionGroup#isOwnerConfigurable() is
	// false (only an admin may change it — mirrors executeExceptionsPreset's own check).
	public static final String EXCEPTION_GROUP_NOT_OWNER_CONFIGURABLE = "exception_group_not_owner_configurable";

	// ExceptionGroupSetPresetC2S / IslandRegistryApi#applyExceptionGroupPreset's
	// IllegalArgumentException: groupId isn't a registered ExceptionGroup, or preset isn't one of
	// nadie/miembros/aliados/todos (FlagPreset#fromId) — exact mirror of INVALID_FLAG_PRESET above.
	public static final String INVALID_EXCEPTION_PRESET = "invalid_exception_preset";

	// IslandActionService#updateFlag/applyFlagPreset when FlagPermissionRequirements has a node set
	// for this flag and the acting player doesn't hold it (PERMISSION_PROVIDER#hasPermission false).
	public static final String MISSING_FLAG_PERMISSION = "missing_flag_permission";

	// Party network block.

	// PartyCreateC2S/PartyInviteC2S/PartyAcceptC2S when the acting/target player is already in a
	// party (PartyRegistry#createParty/#addMember's one-party-at-a-time guard).
	public static final String ALREADY_IN_PARTY = "already_in_party";

	// PartyCreateC2S/PartyRenameC2S when the requested name is already taken by another party
	// (PartyRegistry#createParty/#renameParty, case-insensitive).
	public static final String PARTY_NAME_TAKEN = "party_name_taken";

	// PartyLeaveC2S/PartyKickC2S/PartyRenameC2S/PartyDisbandRequestC2S/PartyAllyAddC2S/
	// PartyAllyRemoveC2S when the sender isn't in any party (PartyRegistry#getPartyOf empty).
	public static final String NO_PARTY = "no_party";

	// PartyKickC2S/PartyRenameC2S/PartyDisbandRequestC2S/PartyAllyAddC2S/PartyAllyRemoveC2S when
	// the sender is in a party but isn't its leader — mirrors PartyCommand#requireLeaderOf.
	public static final String NOT_PARTY_LEADER = "not_party_leader";

	// PartyAcceptC2S when there's no pending party invite for the sender, or it already expired
	// (PartyInviteManager#acceptInvite empty).
	public static final String NO_PENDING_PARTY_INVITE = "no_pending_party_invite";

	// PartyAllyAddC2S/PartyAllyRemoveC2S when targetPartyName doesn't match any party
	// (PartyRegistry#getPartyByName empty).
	public static final String PARTY_NOT_FOUND = "party_not_found";

	// PartyKickC2S when targetUuid is the sender's own uuid (use PartyLeaveC2S/disband instead).
	public static final String CANNOT_KICK_SELF = "cannot_kick_self";

	// PartyKickC2S when targetUuid isn't a member of the sender's party.
	public static final String NOT_A_PARTY_MEMBER = "not_a_party_member";

	// PartyAllyAddC2S when targetPartyName resolves to the sender's own party.
	public static final String PARTY_ALLY_SELF = "party_ally_self";

	// PartyDisbandConfirmC2S when there's no pending disband request for the sender's party, or it
	// already expired (PartyDisbandRequests#confirm false) — mirrors PartyCommand's own "/party
	// disband" (request) step must run first.
	public static final String NO_PENDING_PARTY_DISBAND = "no_pending_party_disband";

	private ActionReason() {
	}
}
