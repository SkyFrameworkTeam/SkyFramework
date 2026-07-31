package com.skyframework.islandcore.island.lifecycle;

import java.time.Instant;
import java.util.UUID;

// Promoted out of InviteManagerImpl (was a private nested record keyed only by inviteeUuid in
// its backing map) so the network layer can query invites without reaching into the
// implementation's internals — see InviteManager#getPendingInvite/#getPendingInvitesForIsland.
public record PendingInvite(UUID islandId, UUID invitedByUuid, UUID inviteeUuid, Instant expiresAt) {
}
