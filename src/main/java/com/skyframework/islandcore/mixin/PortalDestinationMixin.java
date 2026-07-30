package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.IslandCoreMod;

import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

// NetherPortalBlock.createTeleportTarget hardcodes its destination as a ternary (confirmed by
// decompiling the 1.21.1 game jar): currentDimension == NETHER ? OVERWORLD : NETHER, with zero
// concept of "the dimension this portal was originally linked from". This redirects only the
// MinecraftServer.getWorld(...) call made right after that ternary, substituting our own
// configured destination when one exists for the CURRENT (origin) dimension — everything else in
// the method (world-null check, exit-portal search/creation, coordinate-scale handling) runs
// completely untouched, using whatever world we hand back here.
//
// @Redirect (not @ModifyArg) because only @Redirect lets the handler also receive the enclosing
// method's own parameters (world/entity/pos) as trailing arguments in plain Fabric Mixin, without
// needing the MixinExtras @Local sugar.
@Mixin(NetherPortalBlock.class)
public class PortalDestinationMixin {

	@Redirect(
			method = "createTeleportTarget",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;"))
	private ServerWorld islandcore$redirectPortalDestination(
			MinecraftServer server, RegistryKey<World> originalKey, ServerWorld world, Entity entity, BlockPos pos) {
		Optional<Identifier> configured = IslandCoreMod.PORTAL_LINK_CONFIG.getDestination(world.getRegistryKey().getValue());
		if (configured.isPresent()) {
			return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, configured.get()));
		}
		return server.getWorld(originalKey);
	}
}
